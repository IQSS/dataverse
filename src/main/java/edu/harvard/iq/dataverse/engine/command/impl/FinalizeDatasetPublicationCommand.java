package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetLock;
import static edu.harvard.iq.dataverse.DatasetVersion.VersionState.*;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.harvard.iq.dataverse.PersistentIdentifierServiceBean;

/**
 *
 * Takes the last internal steps in publishing a dataset.
 *
 * @author michael
 */
@RequiredPermissions(Permission.PublishDataset)
public class FinalizeDatasetPublicationCommand extends AbstractPublishDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(FinalizeDatasetPublicationCommand.class.getName());
    
    String doiProvider;
    
    public FinalizeDatasetPublicationCommand(Dataset aDataset, String aDoiProvider, DataverseRequest aRequest) {
        super(aDataset, aRequest);
        doiProvider = aDoiProvider;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        Dataset theDataset = getDataset();
        
        registerExternalIdentifier(theDataset, ctxt);
        
        // is this the first publication of the dataset?
        if ( theDataset.getReleaseUser() == null ) {
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
        }
        if ( theDataset.getPublicationDate() == null ) {
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
        } 

        // update metadata
        theDataset.getLatestVersion().setReleaseTime(getTimestamp());
        theDataset.getLatestVersion().setLastUpdateTime(getTimestamp());
        theDataset.setModificationTime(getTimestamp());
        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());
        
        updateFiles(getTimestamp(), ctxt);
        
        // 
        // TODO: Not sure if this .merge() is necessary here - ? 
        // I'm moving a bunch of code from PublishDatasetCommand here; and this .merge()
        // comes from there. There's a chance that the final merge, at the end of this
        // command, would be sufficient. -- L.A. Sep. 6 2017
        theDataset = ctxt.em().merge(theDataset);
        
        updateDatasetUser(ctxt);
        
        updateParentDataversesSubjectsField(theDataset, ctxt);

        PrivateUrl privateUrl = ctxt.engine().submit(new GetPrivateUrlCommand(getRequest(), theDataset));
        if (privateUrl != null) {
            ctxt.engine().submit(new DeletePrivateUrlCommand(getRequest(), theDataset));
        }
        
        if ( theDataset.getLatestVersion().getVersionState() != RELEASED ) {
            // some imported datasets may already be released.
            publicizeExternalIdentifier(theDataset, ctxt);
            theDataset.getLatestVersion().setVersionState(RELEASED);
        }
        
        exportMetadata(ctxt.settings());
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(theDataset, doNormalSolrDocCleanUp);
        ctxt.solrIndex().indexPermissionsForOneDvObject(theDataset);

        // Remove locks
        ctxt.engine().submit(new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.Workflow));
        if ( theDataset.isLockedFor(DatasetLock.Reason.InReview) ) {
            ctxt.engine().submit( 
                    new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.InReview) );
        }
        
        ctxt.workflows().getDefaultWorkflow(TriggerType.PostPublishDataset).ifPresent(wf -> {
            try {
                ctxt.workflows().start(wf, buildContext(doiProvider, TriggerType.PostPublishDataset));
            } catch (CommandException ex) {
                logger.log(Level.SEVERE, "Error invoking post-publish workflow: " + ex.getMessage(), ex);
            }
        });
        
        Dataset readyDataset = ctxt.em().merge(theDataset);
        
        if ( readyDataset != null ) {
            notifyUsersDatasetPublish(ctxt, theDataset);
        }
        
        return readyDataset;
    }

    /**
     * Attempting to run metadata export, for all the formats for which we have
     * metadata Exporters.
     */
    private void exportMetadata(SettingsServiceBean settingsServiceBean) {

        try {
            ExportService instance = ExportService.getInstance(settingsServiceBean);
            instance.exportAllFormats(getDataset());

        } catch (ExportException ex) {
            // Something went wrong!
            // Just like with indexing, a failure to export is not a fatal
            // condition. We'll just log the error as a warning and keep
            // going:
            logger.log(Level.WARNING, "Dataset publication finalization: exception while exporting:{0}", ex.getMessage());
        }
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

    private void publicizeExternalIdentifier(Dataset dataset, CommandContext ctxt) throws CommandException {
        String protocol = getDataset().getProtocol();
        PersistentIdentifierServiceBean idServiceBean = PersistentIdentifierServiceBean.getBean(protocol, ctxt);
        if ( idServiceBean != null ){
            try {
                idServiceBean.publicizeIdentifier(dataset);
            } catch (Throwable e) {
                throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()),this); 
            }
        }
    }
    
    private void updateFiles(Timestamp updateTime, CommandContext ctxt) throws CommandException {
        for (DataFile dataFile : getDataset().getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                // this is a new, previously unpublished file, so publish by setting date
                dataFile.setPublicationDate(updateTime);
                
                // check if any prexisting roleassignments have file download and send notifications
                notifyUsersFileDownload(ctxt, dataFile);
            }
            
            // set the files restriction flag to the same as the latest version's
            if (dataFile.getFileMetadata() != null && dataFile.getFileMetadata().getDatasetVersion().equals(getDataset().getLatestVersion())) {
                dataFile.setRestricted(dataFile.getFileMetadata().isRestricted());
            }
            
            
            if (dataFile.isRestricted()) {
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
                        ctxt.notifications().sendNotification(authenticatedUser, getTimestamp(), UserNotification.Type.MAPLAYERDELETEFAILED, dataFile.getFileMetadata().getId());
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
            .filter(  ra -> ra.getRole().permissions().contains(Permission.DownloadFile) )
            .flatMap( ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream() )
            .distinct() // prevent double-send
            .forEach( au -> ctxt.notifications().sendNotification(au, getTimestamp(), UserNotification.Type.GRANTFILEACCESS, getDataset().getId()) );
    }
    
    private void notifyUsersDatasetPublish(CommandContext ctxt, DvObject subject) {
        ctxt.roles().rolesAssignments(subject).stream()
            .filter(  ra -> ra.getRole().permissions().contains(Permission.ViewUnpublishedDataset) || ra.getRole().permissions().contains(Permission.DownloadFile))
            .flatMap( ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream() )
            .distinct() // prevent double-send
            //.forEach( au -> ctxt.notifications().sendNotification(au, timestamp, messageType, theDataset.getId()) ); //not sure why this line doesn't work instead
            .forEach( au -> ctxt.notifications().sendNotification(au, getTimestamp(), UserNotification.Type.PUBLISHEDDS, getDataset().getLatestVersion().getId()) ); 
    }

}
