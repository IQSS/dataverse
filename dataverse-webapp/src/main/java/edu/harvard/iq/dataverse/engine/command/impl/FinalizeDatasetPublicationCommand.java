package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;

/**
 * Takes the last internal steps in publishing a dataset.
 *
 * @author michael
 */
@RequiredPermissions(Permission.PublishDataset)
public class FinalizeDatasetPublicationCommand extends AbstractPublishDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(FinalizeDatasetPublicationCommand.class.getName());

    /**
     * mirror field from {@link PublishDatasetCommand} of same name
     */
    final boolean datasetExternallyReleased;

    public FinalizeDatasetPublicationCommand(Dataset aDataset, DataverseRequest aRequest) {
        this(aDataset, aRequest, false);
    }

    public FinalizeDatasetPublicationCommand(Dataset aDataset, DataverseRequest aRequest, boolean isPidPrePublished) {
        super(aDataset, aRequest);
        datasetExternallyReleased = isPidPrePublished;
    }

    @Override
    public Dataset execute(CommandContext ctxt)  {
        Dataset theDataset = getDataset();

        if (theDataset.getGlobalIdCreateTime() == null) {
            registerExternalIdentifier(theDataset, ctxt);
        }

        // is this the first publication of the dataset?
        if (theDataset.getPublicationDate() == null) {
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
        }
        if (theDataset.getPublicationDate() == null) {
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
        }

        // update metadata
        theDataset.getLatestVersion().setReleaseTime(getTimestamp());
        theDataset.getLatestVersion().setLastUpdateTime(getTimestamp());
        theDataset.setModificationTime(getTimestamp());

        updateFiles(getTimestamp(), ctxt);

        // 
        // TODO: Not sure if this .merge() is necessary here - ? 
        // I'm moving a bunch of code from PublishDatasetCommand here; and this .merge()
        // comes from there. There's a chance that the final merge, at the end of this
        // command, would be sufficient. -- L.A. Sep. 6 2017
        theDataset = ctxt.em().merge(theDataset);
        setDataset(theDataset);
        updateDatasetUser(ctxt);

        //if the publisher hasn't contributed to this version
        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(theDataset.getLatestVersion(), getUser());

        if (ddu == null) {
            ddu = new DatasetVersionUser();
            ddu.setDatasetVersion(theDataset.getLatestVersion());
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            ddu.setAuthenticatedUser(au);
        }
        ddu.setLastUpdateDate(getTimestamp());
        ctxt.em().merge(ddu);

        updateParentDataversesSubjectsField(theDataset, ctxt);

        if (theDataset.getLatestVersion().getVersionState() != RELEASED) {
            // some imported datasets may already be released.
            if (!datasetExternallyReleased) {
                publicizeExternalIdentifier(theDataset, ctxt);
            }
            theDataset.getLatestVersion().setVersionState(RELEASED);
        }

        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(theDataset, doNormalSolrDocCleanUp);
        ctxt.solrIndex().indexPermissionsForOneDvObject(theDataset);

        // Remove locks
        ctxt.engine().submit(new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.Workflow));
        ctxt.engine().submit(new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.pidRegister));
        if (theDataset.isLockedFor(DatasetLock.Reason.InReview)) {
            ctxt.engine().submit(
                    new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.InReview));
        }

        final Dataset ds = ctxt.em().merge(theDataset);

        ctxt.workflows().getDefaultWorkflow(TriggerType.PostPublishDataset).ifPresent(wf -> {
            try {
                ctxt.workflows().start(wf, buildContext(ds, TriggerType.PostPublishDataset, datasetExternallyReleased));
            } catch (CommandException ex) {
                logger.log(Level.SEVERE, "Error invoking post-publish workflow: " + ex.getMessage(), ex);
            }
        });

        Dataset readyDataset = ctxt.em().merge(theDataset);

        if (readyDataset != null) {
            notifyUsersDatasetPublish(ctxt, theDataset);
        }

        return readyDataset;
    }

    /**
     * add the dataset subjects to all parent dataverses.
     */
    private void updateParentDataversesSubjectsField(Dataset savedDataset, CommandContext ctxt) {
        for (DatasetField dsf : savedDataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                Dataverse dv = savedDataset.getOwner();
                while (dv != null) {
                    if (dv.getDataverseSubjects().addAll(dsf.getControlledVocabularyValues())) {
                        Dataverse dvWithSubjectJustAdded = ctxt.em().merge(dv);
                        ctxt.em().flush();
                        ctxt.index().indexDataverse(dvWithSubjectJustAdded); // need to reindex to capture the new subjects
                    }
                    dv = dv.getOwner();
                }
                break; // we just update the field whose name is DatasetFieldConstant.subject
            }
        }
    }

    private void publicizeExternalIdentifier(Dataset dataset, CommandContext ctxt)  {
        String protocol = getDataset().getProtocol();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, ctxt);
        if (idServiceBean != null) {
            List<String> args = idServiceBean.getProviderInformation();
            try {
                String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol);
                String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat);
                boolean isFilePIDsEnabled = ctxt.settings().isTrueForKey(SettingsServiceBean.Key.FilePIDsEnabled);
                // We will skip trying to register the global identifiers for datafiles 
                // if "dependent" file-level identifiers are requested, AND the naming 
                // protocol of the dataset global id is different from the
                // one currently configured for the Dataverse. This is to specifically 
                // address the issue with the datasets with handle ids registered, 
                // that are currently configured to use DOI.
                // ...
                // Additionaly in 4.9.3 we have added a system variable to disable 
                // registering file PIDs on the installation level.
                if ((currentGlobalIdProtocol.equals(protocol) || dataFilePIDFormat.equals("INDEPENDENT"))//TODO(pm) - check authority too
                        && isFilePIDsEnabled) {
                    //A false return value indicates a failure in calling the service
                    for (DataFile df : dataset.getFiles()) {
                        logger.log(Level.FINE, "registering global id for file {0}", df.getId());
                        //A false return value indicates a failure in calling the service
                        if (!idServiceBean.publicizeIdentifier(df)) {
                            throw new Exception();
                        }
                        df.setGlobalIdCreateTime(getTimestamp());
                        df.setIdentifierRegistered(true);
                    }
                }
                if (!idServiceBean.publicizeIdentifier(dataset)) {
                    throw new Exception();
                }
                dataset.setGlobalIdCreateTime(new Date()); // TODO these two methods should be in the responsibility of the idServiceBean.
                dataset.setIdentifierRegistered(true);
            } catch (Throwable e) {
                ctxt.datasets().removeDatasetLocks(dataset, DatasetLock.Reason.pidRegister);
                throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", args), this);
            }
        }
    }

    private void updateFiles(Timestamp updateTime, CommandContext ctxt)  {
        for (DataFile dataFile : getDataset().getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                // this is a new, previously unpublished file, so publish by setting date
                dataFile.setPublicationDate(updateTime);

                // check if any prexisting roleassignments have file download and send notifications
                notifyUsersFileDownload(ctxt, dataFile);
            }


            if (dataFile.getFileMetadata().getTermsOfUse().getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
                // A couple things need to happen if the file has been restricted: 
                // 1. If there's a map layer associated with this shape file, or 
                //    tabular-with-geo-tag file, all that map layer data (that 
                //    includes most of the actual data in the file!) need to be
                //    removed from WorldMap and GeoConnect, since anyone can get 
                //    download the data from there;
                // 2. If this (image) file has been assigned as the dedicated 
                //    thumbnail for the dataset, we need to remove that assignment, 
                //    now that the file is restricted. 

                // Map layer: 

                if (ctxt.mapLayerMetadata().findMetadataByDatafile(dataFile) != null) {
                    // (We need an AuthenticatedUser in order to produce a WorldMap token!)
                    String id = getUser().getIdentifier();
                    id = id.startsWith("@") ? id.substring(1) : id;
                    AuthenticatedUser authenticatedUser = ctxt.authentication().getAuthenticatedUser(id);
                    try {
                        ctxt.mapLayerMetadata().deleteMapLayerFromWorldMap(dataFile, authenticatedUser);

                        // If that was successful, delete the layer on the Dataverse side as well:
                        //SEK 4/20/2017                
                        //Command to delete from Dataverse side
                        ctxt.engine().submit(new DeleteMapLayerMetadataCommand(this.getRequest(), dataFile));

                        // RP - Bit of hack, update the datafile here b/c the reference to the datafile 
                        // is not being passed all the way up/down the chain.   
                        //
                        dataFile.setPreviewImageAvailable(false);

                    } catch (IOException ioex) {
                        // We are not going to treat it as a fatal condition and bail out, 
                        // but we will send a notification to the user, warning them about
                        // the layer still being out there, un-deleted:
                        ctxt.notifications().sendNotificationWithEmail(authenticatedUser, getTimestamp(), NotificationType.MAPLAYERDELETEFAILED, dataFile.getFileMetadata().getId(), NotificationObjectType.FILEMETADATA);
                    }

                }

                // Dataset thumbnail assignment: 

                if (dataFile.equals(getDataset().getThumbnailFile())) {
                    getDataset().setThumbnailFile(null);
                }
            }
        }
    }


    //These notification methods are fairly similar, but it was cleaner to create a few copies.
    //If more notifications are needed in this command, they should probably be collapsed.
    private void notifyUsersFileDownload(CommandContext ctxt, DvObject subject) {
        ctxt.roles().directRoleAssignments(subject).stream()
                .filter(ra -> ra.getRole().permissions().contains(Permission.DownloadFile))
                .flatMap(ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream())
                .distinct() // prevent double-send
                .forEach(au -> ctxt.notifications().sendNotificationWithEmail(au, getTimestamp(), NotificationType.GRANTFILEACCESS, getDataset().getId(), NotificationObjectType.DATASET));
    }

    private void notifyUsersDatasetPublish(CommandContext ctxt, DvObject subject) {
        ctxt.roles().rolesAssignments(subject).stream()
                .filter(ra -> ra.getRole().permissions().contains(Permission.ViewUnpublishedDataset) || ra.getRole().permissions().contains(Permission.DownloadFile))
                .flatMap(ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream())
                .distinct() // prevent double-send
                //.forEach( au -> ctxt.notifications().sendNotification(au, timestamp, messageType, theDataset.getId()) ); //not sure why this line doesn't work instead
                .forEach(au -> ctxt.notifications().sendNotificationWithEmail(au, getTimestamp(), NotificationType.PUBLISHEDDS, getDataset().getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION));
    }

}
