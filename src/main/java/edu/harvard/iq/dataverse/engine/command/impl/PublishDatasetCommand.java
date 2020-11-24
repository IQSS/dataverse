package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import static edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetResult.Status;

/**
 * Kick-off a dataset publication process. The process may complete immediately, 
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
    private static final Logger logger = Logger.getLogger(PublishDatasetCommand.class.getName());
    boolean minorRelease;
    DataverseRequest request;
    
    /** 
     * The dataset was already released by an external system, and now Dataverse
     * is just internally marking this release version as released. This is happening
     * in scenarios like import or migration.
     */
    final boolean datasetExternallyReleased;
    
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        this( datasetIn, aRequest, minor, false );
    }
    
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor, boolean isPidPrePublished) {
        super(datasetIn, aRequest);
        minorRelease = minor;
        datasetExternallyReleased = isPidPrePublished;
        request = aRequest;
    }

    @Override
    public PublishDatasetResult execute(CommandContext ctxt) throws CommandException {
        
        verifyCommandArguments();
        
        // Invariant 1: If we're here, publishing the dataset makes sense, from a "business logic" point of view.
        // Invariant 2: The latest version of the dataset is the one being published, EVEN IF IT IS NOT DRAFT.
        //              When importing a released dataset, the latest version is marked as RELEASED.

        Dataset theDataset = getDataset();

        // Set the version numbers:

        if (theDataset.getPublicationDate() == null) {
            // First Release
            theDataset.getLatestVersion().setVersionNumber(new Long(1)); // minor release is blocked by #verifyCommandArguments
            theDataset.getLatestVersion().setMinorVersionNumber(new Long(0));
            
        } else if ( minorRelease ) {
            theDataset.getLatestVersion().setVersionNumber(new Long(theDataset.getVersionNumber()));
            theDataset.getLatestVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber() + 1));
            
        } else {
            // major, non-first release
            theDataset.getLatestVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getLatestVersion().setMinorVersionNumber(new Long(0));
        }
        
        Optional<Workflow> prePubWf = ctxt.workflows().getDefaultWorkflow(TriggerType.PrePublishDataset);
        if ( prePubWf.isPresent() ) {
            // We start a workflow
            theDataset = ctxt.em().merge(theDataset);
            ctxt.em().flush();
            ctxt.workflows().start(prePubWf.get(), buildContext(theDataset, TriggerType.PrePublishDataset, datasetExternallyReleased));
            return new PublishDatasetResult(theDataset, Status.Workflow);
            
        } else{
            // We will skip trying to register the global identifiers for datafiles 
            // if "dependent" file-level identifiers are requested, AND the naming 
            // protocol of the dataset global id is different from the 
            // one currently configured for the Dataverse. This is to specifically 
            // address the issue with the datasets with handle ids registered, 
            // that are currently configured to use DOI.
            // If we are registering file-level identifiers, and there are more 
            // than the configured limit number of files, then call Finalize 
            // asychronously (default is 10)
            // ...
            // Additionaly in 4.9.3 we have added a system variable to disable 
            // registering file PIDs on the installation level.
            String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, "");
            String currentGlobalAuthority= ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, "");
            String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
            boolean registerGlobalIdsForFiles = 
                    (currentGlobalIdProtocol.equals(theDataset.getProtocol()) || dataFilePIDFormat.equals("INDEPENDENT")) 
                    && ctxt.systemConfig().isFilePIDsEnabled();
            
            if ( registerGlobalIdsForFiles ){
                registerGlobalIdsForFiles = currentGlobalAuthority.equals( theDataset.getAuthority() );
	    }
            
            boolean validatePhysicalFiles = ctxt.systemConfig().isDatafileValidationOnPublishEnabled();

            // As of v5.0, publishing a dataset is always done asynchronously, 
            // with the dataset locked for the duration of the operation. 
            
            //if ((registerGlobalIdsForFiles || validatePhysicalFiles) 
            //        && theDataset.getFiles().size() > ctxt.systemConfig().getPIDAsynchRegFileCount()) { 
                
            String info = "Publishing the dataset; "; 
            info += registerGlobalIdsForFiles ? "Registering PIDs for Datafiles; " : "";
            info += validatePhysicalFiles ? "Validating Datafiles Asynchronously" : "";
            
            AuthenticatedUser user = request.getAuthenticatedUser();
            DatasetLock lock = new DatasetLock(DatasetLock.Reason.finalizePublication, user);
            lock.setDataset(theDataset);
            lock.setInfo(info);
            ctxt.datasets().addDatasetLock(theDataset, lock);
            theDataset = ctxt.em().merge(theDataset);
            // The call to FinalizePublicationCommand has been moved to the new @onSuccess()
            // method:
            //ctxt.datasets().callFinalizePublishCommandAsynchronously(theDataset.getId(), ctxt, request, datasetExternallyReleased);
            return new PublishDatasetResult(theDataset, Status.Inprogress);

            /**
              * Code for for "synchronous" (while-you-wait) publishing 
              * is preserved below, commented out:
            } else {
                // Synchronous publishing (no workflow involved)
                theDataset = ctxt.engine().submit(new FinalizeDatasetPublicationCommand(theDataset, getRequest(),datasetExternallyReleased));
                return new PublishDatasetResult(theDataset, Status.Completed);
            } */
        }
    }
    
    /**
     * See that publishing the dataset in the requested manner makes sense, at
     * the given state of the dataset.
     * 
     * @throws IllegalCommandException if the publication request is invalid.
     */
    private void verifyCommandArguments() throws IllegalCommandException {
        if (!getDataset().getOwner().isReleased()) {
            throw new IllegalCommandException("This dataset may not be published because its host dataverse (" + getDataset().getOwner().getAlias() + ") has not been published.", this);
        }
        
        if ( ! getUser().isAuthenticated() ) {
            throw new IllegalCommandException("Only authenticated users can release a Dataset. Please authenticate and try again.", this);
        }
        
        if ( getDataset().isLockedFor(DatasetLock.Reason.Workflow)
                || getDataset().isLockedFor(DatasetLock.Reason.Ingest) 
                || getDataset().isLockedFor(DatasetLock.Reason.finalizePublication)) {
            throw new IllegalCommandException("This dataset is locked. Reason: " 
                    + getDataset().getLocks().stream().map(l -> l.getReason().name()).collect( joining(",") )
                    + ". Please try publishing later.", this);
        }
        
        if ( getDataset().isLockedFor(DatasetLock.Reason.FileValidationFailed)) {
            throw new IllegalCommandException("This dataset cannot be published because some files have been found missing or corrupted. " 
                    + ". Please contact support to address this.", this);
        }
        
        if ( datasetExternallyReleased ) {
            if ( ! getDataset().getLatestVersion().isReleased() ) {
                throw new IllegalCommandException("Latest version of dataset " + getDataset().getIdentifier() + " is not marked as releasd.", this);
            }
                
        } else {
            if (getDataset().getLatestVersion().isReleased()) {
                throw new IllegalCommandException("Latest version of dataset " + getDataset().getIdentifier() + " is already released. Only draft versions can be released.", this);
            }

            // prevent publishing of 0.1 version
            if (minorRelease && getDataset().getVersions().size() == 1 && getDataset().getLatestVersion().isDraft()) {
                throw new IllegalCommandException("Cannot publish as minor version. Re-try as major release.", this);
            }

            if (minorRelease && !getDataset().getLatestVersion().isMinorUpdate()) {
                throw new IllegalCommandException("Cannot release as minor version. Re-try as major release.", this);
            }
        }
    }
    
    
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        Dataset dataset = null;
        try{
            dataset = (Dataset) r;
        } catch (ClassCastException e){
            dataset  = ((PublishDatasetResult) r).getDataset();
        }

        if (dataset != null) {
            logger.fine("From onSuccess, calling FinalizeDatasetPublicationCommand for dataset " + dataset.getGlobalId().asString());
            ctxt.datasets().callFinalizePublishCommandAsynchronously(dataset.getId(), ctxt, request, datasetExternallyReleased);
            return true;
        }
        
        return false;
    }
    
}
