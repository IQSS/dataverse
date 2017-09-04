package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

/**
 * Kick-off a dataset publication process. The process may complete immediatly, 
 * but may also result in a workflow being started and pending on some external 
 * response. Either way, the process will be completed by an instance of 
 * {@link FinalizeDatasetPublicationCommand}.
 * 
 * @see FinalizeDatasetPublicationCommand
 * 
 * @author skraffmiller
 * @author michbarsinai
 */
@RequiredPermissions(Permission.PublishDataset)
public class PublishDatasetCommand extends AbstractPublishDatasetCommand<PublishDatasetResult> {

    private static final int FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT = 2 ^ 8;

    boolean minorRelease;
    
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        super(datasetIn, aRequest);
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public PublishDatasetResult execute(CommandContext ctxt) throws CommandException {

        verifyCommandArguments();
        registerExternalIdentifier(theDataset, ctxt);

        /* make an attempt to register if not registered */
        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);

        if (theDataset.getPublicationDate() == null) {
            // First Release
            // Send notifications to users with download file permission
            notifyUsers(ctxt, theDataset, UserNotification.Type.ASSIGNROLE);
            
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
            theDataset.getEditVersion().setVersionNumber(new Long(1)); // minor release is blocked by #verifyCommandArguments
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            
        } else if ( minorRelease ) {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber()));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber() + 1));
            
        } else /* major, non-first release */ {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
        }

        // update metadata
        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setReleaseTime(updateTime);
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.setModificationTime(updateTime);
        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());
        
        updateFiles(updateTime, ctxt);
        
        theDataset = ctxt.em().merge(theDataset);
        
        //Move remove lock to after merge... SEK 9/1/17
        ctxt.engine().submit( new RemoveLockCommand(getRequest(), theDataset));

        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(theDataset.getLatestVersion(), getUser());

        if (ddu == null) {
            ddu = new DatasetVersionUser();
            ddu.setDatasetVersion(theDataset.getLatestVersion());
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            ddu.setAuthenticatedUser(au);
        }
        ddu.setLastUpdateDate((Timestamp) updateTime);
        ctxt.em().merge(ddu);
        
        Optional<Workflow> prePubWf = ctxt.workflows().getDefaultWorkflow(TriggerType.PrePublishDataset);
        if ( prePubWf.isPresent() ) {
            // We start a workflow
            ctxt.engine().submit( new AddLockCommand(getRequest(), theDataset, new DatasetLock(DatasetLock.Reason.Workflow, getRequest().getAuthenticatedUser())));
            ctxt.workflows().start(prePubWf.get(), buildContext(doiProvider, TriggerType.PrePublishDataset) );
            return new PublishDatasetResult(theDataset, false);
            
        } else {
            // Synchronous publishing (no workflow involved)
            theDataset = ctxt.engine().submit( new FinalizeDatasetPublicationCommand(theDataset, doiProvider, getRequest()) );
            return new PublishDatasetResult(ctxt.em().merge(theDataset), true);
        }
    }
    
    private void updateFiles(Timestamp updateTime, CommandContext ctxt) throws CommandException {
        for (DataFile dataFile : theDataset.getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                // this is a new, previously unpublished file, so publish by setting date
                dataFile.setPublicationDate(updateTime);
                
                // check if any prexisting roleassignments have file download and send notifications
                notifyUsers(ctxt, dataFile, UserNotification.Type.GRANTFILEACCESS);
            }
            
            // set the files restriction flag to the same as the latest version's
            if (dataFile.getFileMetadata() != null && dataFile.getFileMetadata().getDatasetVersion().equals(theDataset.getLatestVersion())) {
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
                        ctxt.notifications().sendNotification(authenticatedUser, new Timestamp(new Date().getTime()), UserNotification.Type.MAPLAYERDELETEFAILED, dataFile.getFileMetadata().getId());
                    }

                }
                
                // Dataset thumbnail assignment: 
                
                if (dataFile.equals(theDataset.getThumbnailFile())) {
                    theDataset.setThumbnailFile(null);
                }
            }
        }
    }

    /**
     * See that publishing the dataset in the requested manner makes sense, at
     * the given state of the dataset.
     * 
     * @throws IllegalCommandException if the publication request is invalid.
     */
    private void verifyCommandArguments() throws IllegalCommandException {
        if (!theDataset.getOwner().isReleased()) {
            throw new IllegalCommandException("This dataset may not be published because its host dataverse (" + theDataset.getOwner().getAlias() + ") has not been published.", this);
        }
        
        if (theDataset.isLocked()  && !theDataset.getDatasetLock().getReason().equals(DatasetLock.Reason.InReview)) {
            
            throw new IllegalCommandException("This dataset is locked. Reason: " + theDataset.getDatasetLock().getReason().toString() + ". Please try publishing later.", this);
        }
        
        if (theDataset.getLatestVersion().isReleased()) {
            throw new IllegalCommandException("Latest version of dataset " + theDataset.getIdentifier() + " is already released. Only draft versions can be released.", this);
        }
        
        // prevent publishing of 0.1 version
        if (minorRelease && theDataset.getVersions().size() == 1 && theDataset.getLatestVersion().isDraft()) {
            throw new IllegalCommandException("Cannot publish as minor version. Re-try as major release.", this);
        }
        
        if (minorRelease && !theDataset.getLatestVersion().isMinorUpdate()) {
            throw new IllegalCommandException("Cannot release as minor version. Re-try as major release.", this);
        }
        
        if ( ! getUser().isAuthenticated() ) {
            throw new IllegalCommandException("Only authenticated users can release a Dataset. Please authenticate and try again.", this);
        }
    }

    
    private void notifyUsers(CommandContext ctxt, DvObject subject, UserNotification.Type messageType ) {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        ctxt.roles().directRoleAssignments(subject).stream()
            .filter(  ra -> ra.getRole().permissions().contains(Permission.DownloadFile) )
            .flatMap( ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream() )
            .distinct() // prevent double-send
            .forEach( au -> ctxt.notifications().sendNotification(au, timestamp, messageType, theDataset.getId()) );
    }
    
    /**
     * Whether it's EZID or DataCiteif, if the registration is 
     * refused because the identifier already exists, we'll generate another one
     * and try to register again... but only up to some
     * reasonably high number of times - so that we don't 
     * go into an infinite loop here, if EZID is giving us 
     * these duplicate messages in error. 
     * 
     * (and we do want the limit to be a "reasonably high" number! 
     * true, if our identifiers are randomly generated strings, 
     * then it is highly unlikely that we'll ever run into a 
     * duplicate race condition repeatedly; but if they are sequential
     * numeric values, than it is entirely possible that a large
     * enough number of values will be legitimately registered 
     * by another entity sharing the same authority...)
     * @param theDataset
     * @param ctxt
     * @param doiProvider
     * @throws CommandException 
     */
    private void registerExternalIdentifier(Dataset theDataset, CommandContext ctxt) throws CommandException {
        IdServiceBean idServiceBean = IdServiceBean.getBean(theDataset.getProtocol(), ctxt);
        if (theDataset.getGlobalIdCreateTime() == null) {
          if (idServiceBean!=null) {
            try {
              if (!idServiceBean.alreadyExists(theDataset)) {
                idServiceBean.createIdentifier(theDataset);
                theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
              } else {
                int attempts = 0;

                while (idServiceBean.alreadyExists(theDataset) && attempts < FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                  theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
                  attempts++;
                }

                if (idServiceBean.alreadyExists(theDataset)) {
                  throw new IllegalCommandException("This dataset may not be published because its identifier is already in use by another dataset;gave up after " + attempts + " attempts. Current (last requested) identifier: " + theDataset.getIdentifier(), this);
                }
                idServiceBean.createIdentifier(theDataset);
                theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));

              }

            } catch (Throwable e) {
              throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()),this); 
            }
          } else {
            throw new IllegalCommandException("This dataset may not be published because its id registry service is not supported.", this);
          }
          
        }
    }
}
