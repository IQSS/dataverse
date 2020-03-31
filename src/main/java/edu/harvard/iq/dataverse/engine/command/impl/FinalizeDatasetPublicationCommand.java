package edu.harvard.iq.dataverse.engine.command.impl;

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
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.ExportException;
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
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;


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
        
        if ( theDataset.getGlobalIdCreateTime() == null ) {
            registerExternalIdentifier(theDataset, ctxt);
        }
                
        // is this the first publication of the dataset?
        if (theDataset.getPublicationDate() == null) {
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
        
	if ( theDataset.getLatestVersion().getVersionState() != RELEASED ) {
		// some imported datasets may already be released.
                
                // validate the physical files (verify checksums):
                validateDataFiles(theDataset, ctxt);
                
		if (!datasetExternallyReleased){
			publicizeExternalIdentifier(theDataset, ctxt);
		}
		theDataset.getLatestVersion().setVersionState(RELEASED);
	}
        

        // Remove locks
        ctxt.engine().submit(new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.Workflow));
        ctxt.engine().submit(new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.pidRegister));
        if ( theDataset.isLockedFor(DatasetLock.Reason.InReview) ) {
            ctxt.engine().submit( 
                    new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.InReview) );
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
        
        if ( readyDataset != null ) {
            notifyUsersDatasetPublish(ctxt, theDataset);
        }
        
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
            Future<String> indexString = ctxt.index().indexDataset(dataset, true);                   
        } catch (IOException | SolrServerException e) {    
            String failureLogText = "Post-publication indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(this, failureLogText,  dataset);
            retVal = false;
        }

        ctxt.solrIndex().indexPermissionsForOneDvObject(dataset);
        exportMetadata(dataset, ctxt.settings());
        ctxt.datasets().updateLastExportTimeStamp(dataset.getId());
        return retVal;
    }

    /**
     * Attempting to run metadata export, for all the formats for which we have
     * metadata Exporters.
     */
    private void exportMetadata(Dataset dataset, SettingsServiceBean settingsServiceBean) {

        try {
            ExportService instance = ExportService.getInstance(settingsServiceBean);
            instance.exportAllFormats(dataset);

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
    private void updateParentDataversesSubjectsField(Dataset savedDataset, CommandContext ctxt) throws  SolrServerException, IOException {
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

    private void validateDataFiles(Dataset dataset, CommandContext ctxt) throws CommandException {
        try {
            for (DataFile dataFile : dataset.getFiles()) {
                // TODO: Should we validate all the files in the dataset, or only 
                // the files that haven't been published previously?
                logger.log(Level.FINE, "validating DataFile {0}", dataFile.getId());
                
                DataFile.ChecksumType checksumType = dataFile.getChecksumType();
                if (checksumType == null) {
                    String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.noChecksumType", Arrays.asList(dataFile.getId().toString()));
                    logger.log(Level.INFO, info);
                    throw new Exception(info);
                }
                                
                StorageIO<DataFile> storage = dataFile.getStorageIO();
                storage.open(DataAccessOption.READ_ACCESS);
                InputStream in = null;
                
                if (!dataFile.isTabularData()) {
                    in = storage.getInputStream();
                } else {
                    // if this is a tabular file, read the preserved original "auxiliary file"
                    // instead:
                    in = storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                }
                                
                if (in == null) {
                    String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.failRead", Arrays.asList(dataFile.getId().toString()));
                    logger.log(Level.INFO, info);
                    throw new Exception(info);
                }
                
                String recalculatedChecksum = null; 
                try {
                    recalculatedChecksum = FileUtil.calculateChecksum(in, checksumType);
                } catch (RuntimeException rte) {
                    recalculatedChecksum = null; 
                } finally {
                    IOUtils.closeQuietly(in);
                }
                
                if (recalculatedChecksum == null) {
                    String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.failCalculateChecksum", Arrays.asList(dataFile.getId().toString()));
                    logger.log(Level.INFO, info); 
                    throw new Exception(info);
                }
                
                // TODO: What should we do if the datafile does not have a non-null checksum?
                // Should we fail, or should we assume that the recalculated checksum
                // is correct, and populate the checksumValue field with it?
                
                if (!recalculatedChecksum.equals(dataFile.getChecksumValue())) {
                    String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.wrongChecksumValue", Arrays.asList(dataFile.getId().toString()));
                    logger.log(Level.INFO, info); 
                    throw new Exception(info);
                }
                
                logger.log(Level.INFO, "successfully validated DataFile {0}; checksum {1}", new Object[]{dataFile.getId(), recalculatedChecksum});
            }
        } catch (Throwable e) {
            // Check if there is a workflow lock on the dataset - i.e., if this 
            // is being done asynchronously. If so, change the lock message 
            // to notify the user what went wrong, and leave the lock in place:
            
            if (dataset.isLockedFor(DatasetLock.Reason.pidRegister)) {
                DatasetLock workflowLock = dataset.getLockFor(DatasetLock.Reason.pidRegister);
                workflowLock.setInfo(FILE_VALIDATION_ERROR);
                ctxt.datasets().updateDatasetLock(workflowLock);
            }

            // Throw a new CommandException; if the command is being called 
            // synchronously, it will be intercepted and the page will display 
            // the error message for the user.
            throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.details"), this);
        }
        
        /* 
         * for debugging only: (TODO: remove before making the final PR)
        logger.log(Level.INFO,"Validation successful; but throwing an exception anyway, for testing purposes");
        if (dataset.isLockedFor(DatasetLock.Reason.pidRegister)) {
            DatasetLock workflowLock = dataset.getLockFor(DatasetLock.Reason.pidRegister);
            workflowLock.setInfo(FILE_VALIDATION_ERROR);
            ctxt.datasets().updateDatasetLock(workflowLock);
        }
        throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.details"), this);
        */
    }
    
    private void publicizeExternalIdentifier(Dataset dataset, CommandContext ctxt) throws CommandException {
        String protocol = getDataset().getProtocol();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(protocol, ctxt);
        if (idServiceBean != null) {
            List<String> args = idServiceBean.getProviderInformation();
            try {
                String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, "");
                String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
                boolean isFilePIDsEnabled = ctxt.systemConfig().isFilePIDsEnabled();
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
        /*
         * for debugging only: (TODO: remove before making the final PR)
        throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()), this);
         */
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
