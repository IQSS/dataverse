package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetLock;
import static edu.harvard.iq.dataverse.DatasetVersion.VersionState.*;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.util.ArrayList;
import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrServerException;

import javax.ejb.EJB;
import javax.inject.Inject;


/**
 *
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
    
    List<Dataverse> dataversesToIndex = new ArrayList<>();
    
    public static final String FILE_VALIDATION_ERROR = "FILE VALIDATION ERROR";
    
    public FinalizeDatasetPublicationCommand(Dataset aDataset, DataverseRequest aRequest) {
        this( aDataset, aRequest, false );
    }
    public FinalizeDatasetPublicationCommand(Dataset aDataset, DataverseRequest aRequest, boolean isPidPrePublished) {
        super(aDataset, aRequest);
	datasetExternallyReleased = isPidPrePublished;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        Dataset theDataset = getDataset();
        
        logger.info("Finalizing publication of the dataset "+theDataset.getGlobalId().asString());
        
        // validate the physical files before we do anything else: 
        // (unless specifically disabled; or a minor version)
        if (theDataset.getLatestVersion().getVersionState() != RELEASED
                && theDataset.getLatestVersion().getMinorVersionNumber() != null
                && theDataset.getLatestVersion().getMinorVersionNumber().equals((long) 0)
                && ctxt.systemConfig().isDatafileValidationOnPublishEnabled()) {
            // some imported datasets may already be released.

            // validate the physical files (verify checksums):
            validateDataFiles(theDataset, ctxt);
            // (this will throw a CommandException if it fails)
        }
        
        validateOrDie(theDataset.getLatestVersion(), false);
        
		/*
		 * Try to register the dataset identifier. For PID providers that have registerWhenPublished == false (all except the FAKE provider at present)
		 * the registerExternalIdentifier command will make one try to create the identifier if needed (e.g. if reserving at dataset creation wasn't done/failed).
		 * For registerWhenPublished == true providers, if a PID conflict is found, the call will retry with new PIDs. 
		 */
        if ( theDataset.getGlobalIdCreateTime() == null ) {
            try {
                // This can potentially throw a CommandException, so let's make 
                // sure we exit cleanly:

            	registerExternalIdentifier(theDataset, ctxt, false);
            } catch (CommandException comEx) {
                logger.warning("Failed to reserve the identifier "+theDataset.getGlobalId().asString()+"; notifying the user(s), unlocking the dataset");
                // Send failure notification to the user: 
                notifyUsersDatasetPublishStatus(ctxt, theDataset, UserNotification.Type.PUBLISHFAILED_PIDREG);
                // Remove the dataset lock: 
                ctxt.datasets().removeDatasetLocks(theDataset, DatasetLock.Reason.finalizePublication);
                // re-throw the exception:
                throw comEx;
            }
        }
                
        // is this the first publication of the dataset?
        if (theDataset.getPublicationDate() == null) {
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
        }
        if ( theDataset.getPublicationDate() == null ) {
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
        } 

        //Clear any external status
        theDataset.getLatestVersion().setExternalStatusLabel(null);
        
        // update metadata
        if (theDataset.getLatestVersion().getReleaseTime() == null) {
            // Allow migrated versions to keep original release dates
            theDataset.getLatestVersion().setReleaseTime(getTimestamp());
        }
        theDataset.getLatestVersion().setLastUpdateTime(getTimestamp());
        theDataset.setModificationTime(getTimestamp());
        theDataset.setFileAccessRequest(theDataset.getLatestVersion().getTermsOfUseAndAccess().isFileAccessRequest());
        
        //Use dataset pub date (which may not be the current date for migrated datasets)
        updateFiles(new Timestamp(theDataset.getLatestVersion().getReleaseTime().getTime()), ctxt);
        
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
        
        try {
            updateParentDataversesSubjectsField(theDataset, ctxt);
        } catch (IOException | SolrServerException e) {
            String failureLogText = "Post-publication indexing failed for Dataverse subject update. ";
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(this, failureLogText, theDataset);

        }

        List<Command> previouslyCalled = ctxt.getCommandsCalled();
        
        PrivateUrl privateUrl = ctxt.engine().submit(new GetPrivateUrlCommand(getRequest(), theDataset));
        List<Command> afterSub = ctxt.getCommandsCalled();
        previouslyCalled.forEach((c) -> {
            ctxt.getCommandsCalled().add(c);
        });
        if (privateUrl != null) {
            ctxt.engine().submit(new DeletePrivateUrlCommand(getRequest(), theDataset));
        }
        
	if (theDataset.getLatestVersion().getVersionState() != RELEASED) {
            // some imported datasets may already be released.

            if (!datasetExternallyReleased) {
                publicizeExternalIdentifier(theDataset, ctxt);
                // Will throw a CommandException, unless successful.
                // This will end the execution of the command, but the method 
                // above takes proper care to "clean up after itself" in case of
                // a failure - it will remove any locks, and it will send a
                // proper notification to the user(s). 
            }
            theDataset.getLatestVersion().setVersionState(RELEASED);
        }
        
        final Dataset ds = ctxt.em().merge(theDataset);
        //Remove any pre-pub workflow lock (not needed as WorkflowServiceBean.workflowComplete() should already have removed it after setting the finalizePublication lock?)
        ctxt.datasets().removeDatasetLocks(ds, DatasetLock.Reason.Workflow);
        
        //Should this be in onSuccess()?
        ctxt.workflows().getDefaultWorkflow(TriggerType.PostPublishDataset).ifPresent(wf -> {
            try {
                ctxt.workflows().start(wf, buildContext(ds, TriggerType.PostPublishDataset, datasetExternallyReleased), false);
            } catch (CommandException ex) {
                ctxt.datasets().removeDatasetLocks(ds, DatasetLock.Reason.Workflow);
                logger.log(Level.SEVERE, "Error invoking post-publish workflow: " + ex.getMessage(), ex);
            }
        });

        Dataset readyDataset = ctxt.em().merge(ds);
        
        // Finally, unlock the dataset (leaving any post-publish workflow lock in place)
        ctxt.datasets().removeDatasetLocks(readyDataset, DatasetLock.Reason.finalizePublication);
        if (readyDataset.isLockedFor(DatasetLock.Reason.InReview) ) {
            ctxt.datasets().removeDatasetLocks(readyDataset, DatasetLock.Reason.InReview);
        }
        
        logger.info("Successfully published the dataset "+readyDataset.getGlobalId().asString());
        readyDataset = ctxt.em().merge(readyDataset);
        
        return readyDataset;
    }
    
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        boolean retVal = true;
        Dataset dataset = null;
        try{
            dataset = (Dataset) r;
        } catch (ClassCastException e){
            dataset  = ((PublishDatasetResult) r).getDataset();
        }
        
        try {
            // Success! - send notification:
            notifyUsersDatasetPublishStatus(ctxt, dataset, UserNotification.Type.PUBLISHEDDS);
        } catch (Exception e) {
            logger.warning("Failure to send dataset published messages for : " + dataset.getId() + " : " + e.getMessage());
        }
        try {
            Future<String> indexString = ctxt.index().indexDataset(dataset, true);                   
        } catch (IOException | SolrServerException e) {    
            String failureLogText = "Post-publication indexing failed. You can kick off a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(this, failureLogText,  dataset);
            retVal = false;
        }
        
        //re-indexing dataverses that have additional subjects
        if (!dataversesToIndex.isEmpty()){
            for (Dataverse dv : dataversesToIndex) {
                try {
                    Future<String> indexString = ctxt.index().indexDataverse(dv);
                } catch (IOException | SolrServerException e) {
                    String failureLogText = "Post-publication indexing failed. You can kick off a re-index of this dataverse with: \r\n curl http://localhost:8080/api/admin/index/dataverses/" + dv.getId().toString();
                    failureLogText += "\r\n" + e.getLocalizedMessage();
                    LoggingUtil.writeOnSuccessFailureLog(this, failureLogText, dataset);
                    retVal = false;
                } 
            }
        }

        // Metadata export:
        
        try {
            ExportService instance = ExportService.getInstance();
            instance.exportAllFormats(dataset);
            dataset = ctxt.datasets().merge(dataset); 
        } catch (Exception ex) {
            // Something went wrong!
            // Just like with indexing, a failure to export is not a fatal
            // condition. We'll just log the error as a warning and keep
            // going:
            logger.warning("Finalization: exception caught while exporting: "+ex.getMessage());
            // ... but it is important to only update the export time stamp if the 
            // export was indeed successful.
        }        
        
        return retVal;
    }

    /**
     * add the dataset subjects to all parent dataverses.
     */
    private void updateParentDataversesSubjectsField(Dataset savedDataset, CommandContext ctxt) throws  SolrServerException, IOException {
        
        for (DatasetField dsf : savedDataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                Dataverse dv = savedDataset.getOwner();
                while (dv != null) {
                    boolean newSubjectsAdded = false;
                    for (ControlledVocabularyValue cvv : dsf.getControlledVocabularyValues()) {                   
                        if (!dv.getDataverseSubjects().contains(cvv)) {
                            logger.fine("dv "+dv.getAlias()+" does not have subject "+cvv.getStrValue());
                            newSubjectsAdded = true;
                            dv.getDataverseSubjects().add(cvv);
                        } else {
                            logger.fine("dv "+dv.getAlias()+" already has subject "+cvv.getStrValue());
                        }
                    }
                    if (newSubjectsAdded) {
                        logger.fine("new dataverse subjects added - saving and reindexing in OnSuccess");
                        Dataverse dvWithSubjectJustAdded = ctxt.em().merge(dv);
                        ctxt.em().flush();
                        //adding dv to list of those we need to re-index for new subjects
                        dataversesToIndex.add(dvWithSubjectJustAdded);                       
                    } else {
                        logger.fine("no new subjects added to the dataverse; skipping reindexing");
                    }
                    dv = dv.getOwner();
                }
                break; // we just update the field whose name is DatasetFieldConstant.subject
            }
        }
    }

    private void validateDataFiles(Dataset dataset, CommandContext ctxt) throws CommandException {
        try {
            long maxDatasetSize = ctxt.systemConfig().getDatasetValidationSizeLimit();
            long maxFileSize = ctxt.systemConfig().getFileValidationSizeLimit();

            long datasetSize = DatasetUtil.getDownloadSizeNumeric(dataset.getLatestVersion(), false);
            if (maxDatasetSize == -1 || datasetSize < maxDatasetSize) {
                for (DataFile dataFile : dataset.getFiles()) {
                    // TODO: Should we validate all the files in the dataset, or only
                    // the files that haven't been published previously?
                    // (the decision was made to validate all the files on every
                    // major release; we can revisit the decision if there's any
                    // indication that this makes publishing take significantly longer.
                    if (maxFileSize == -1 || dataFile.getFilesize() < maxFileSize) {
                        FileUtil.validateDataFileChecksum(dataFile);
                    }
                    else {
                        String message = "Checksum Validation skipped for this datafile: " + dataFile.getId() + ", because of the size of the datafile limit (set to " + maxFileSize + " ); ";
                        logger.info(message);
                    }
                }
            }
            else {
                String message = "Checksum Validation skipped for this dataset: " + dataset.getId() + ", because of the size of the dataset limit (set to " + maxDatasetSize + " ); ";
                logger.info(message);
            }
        } catch (Throwable e) {
            if (dataset.isLockedFor(DatasetLock.Reason.finalizePublication)) {
                DatasetLock lock = dataset.getLockFor(DatasetLock.Reason.finalizePublication);
                lock.setReason(DatasetLock.Reason.FileValidationFailed);
                lock.setInfo(FILE_VALIDATION_ERROR);
                ctxt.datasets().updateDatasetLock(lock);
            } else {            
                // Lock the dataset with a new FileValidationFailed lock: 
                DatasetLock lock = new DatasetLock(DatasetLock.Reason.FileValidationFailed, getRequest().getAuthenticatedUser()); //(AuthenticatedUser)getUser());
                lock.setDataset(dataset);
                lock.setInfo(FILE_VALIDATION_ERROR);
                ctxt.datasets().addDatasetLock(dataset, lock);
            }
            
            // Throw a new CommandException; if the command is being called 
            // synchronously, it will be intercepted and the page will display 
            // the error message for the user.
            throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.details"), this);
        }
    }
    
    private void publicizeExternalIdentifier(Dataset dataset, CommandContext ctxt) throws CommandException {
        String protocol = getDataset().getProtocol();
        String authority = getDataset().getAuthority();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, ctxt);
 
        if (idServiceBean != null) {
            List<String> args = idServiceBean.getProviderInformation();
            try {
                String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, "");
                String currentGlobalAuthority = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, "");
                String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
                boolean isFilePIDsEnabled = ctxt.systemConfig().isFilePIDsEnabled();
                // We will skip trying to register the global identifiers for datafiles 
                // if "dependent" file-level identifiers are requested, AND the naming 
                // protocol, or the authority of the dataset global id is different from 
                // what's currently configured for the Dataverse. In other words
                // we can't get "dependent" DOIs assigned to files in a dataset  
                // with the registered id that is a handle; or even a DOI, but in 
                // an authority that's different from what's currently configured.
                // Additionaly in 4.9.3 we have added a system variable to disable 
                // registering file PIDs on the installation level.
                if (((currentGlobalIdProtocol.equals(protocol) && currentGlobalAuthority.equals(authority))
                        || dataFilePIDFormat.equals("INDEPENDENT"))
                        && isFilePIDsEnabled
                        && dataset.getLatestVersion().getMinorVersionNumber() != null
                        && dataset.getLatestVersion().getMinorVersionNumber().equals((long) 0)) {
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
                logger.warning("Failed to register the identifier "+dataset.getGlobalId().asString()+", or to register a file in the dataset; notifying the user(s), unlocking the dataset");

                // Send failure notification to the user: 
                notifyUsersDatasetPublishStatus(ctxt, dataset, UserNotification.Type.PUBLISHFAILED_PIDREG);
                
                ctxt.datasets().removeDatasetLocks(dataset, DatasetLock.Reason.finalizePublication);
                throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", args), this);
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
                // If the file has been restricted: 
                //    If this (image) file has been assigned as the dedicated 
                //    thumbnail for the dataset, we need to remove that assignment, 
                //    now that the file is restricted. 
               
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
    
    private void notifyUsersDatasetPublishStatus(CommandContext ctxt, DvObject subject, UserNotification.Type type) {
        
        ctxt.roles().rolesAssignments(subject).stream()
            .filter(  ra -> ra.getRole().permissions().contains(Permission.ViewUnpublishedDataset) || ra.getRole().permissions().contains(Permission.DownloadFile))
            .flatMap( ra -> ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier())).stream() )
            .distinct() // prevent double-send
            //.forEach( au -> ctxt.notifications().sendNotification(au, timestamp, messageType, theDataset.getId()) ); //not sure why this line doesn't work instead
            .forEach( au -> ctxt.notifications().sendNotificationInNewTransaction(au, getTimestamp(), type, getDataset().getLatestVersion().getId()) ); 
    }

}
