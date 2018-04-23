package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
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
import java.util.Date;
import java.util.Optional;
import static java.util.stream.Collectors.joining;
import javax.ejb.Asynchronous;

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

    boolean minorRelease;
    DataverseRequest request;
    
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        super(datasetIn, aRequest);
        minorRelease = minor;
        theDataset = datasetIn;
        request = aRequest;
    }

    @Override
    public PublishDatasetResult execute(CommandContext ctxt) throws CommandException {

        verifyCommandArguments();

        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        
        // Set the version numbers:

        if (theDataset.getPublicationDate() == null) {
            // First Release
            theDataset.getEditVersion().setVersionNumber(new Long(1)); // minor release is blocked by #verifyCommandArguments
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            
        } else if ( minorRelease ) {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber()));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber() + 1));
            
        } else {
            // major, non-first release
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
        }

        theDataset = ctxt.em().merge(theDataset);
        
        Optional<Workflow> prePubWf = ctxt.workflows().getDefaultWorkflow(TriggerType.PrePublishDataset);
        if ( prePubWf.isPresent() ) {
            // We start a workflow
            ctxt.workflows().start(prePubWf.get(), buildContext(doiProvider, TriggerType.PrePublishDataset) );
            return new PublishDatasetResult(theDataset, false);
            
        } else {
            //if there are more than required size files  then call Finalize asychronously (default is 10)
            if (theDataset.getFiles().size() > ctxt.systemConfig().getPIDAsynchRegFileCount()) {     
                String info = "Adding File PIDs asynchronously";
                AuthenticatedUser user = request.getAuthenticatedUser() ;
                DatasetLock lock = new DatasetLock(DatasetLock.Reason.pidRegister, user);
                lock.setDataset(theDataset);
                lock.setInfo(info);
                lock.setStartTime(new Date());
                theDataset.getLocks().add(lock);
                return callFinalizeAsync(ctxt);
            }
            // Synchronous publishing (no workflow involved)
            theDataset = ctxt.engine().submit(new FinalizeDatasetPublicationCommand(theDataset, doiProvider, getRequest()));
            return new PublishDatasetResult(ctxt.em().merge(theDataset), true);
        }
    }
    
    
    private PublishDatasetResult callFinalizeAsync(CommandContext ctxt) throws CommandException {
        try {
            ctxt.datasets().callFinalizePublishCommandAsynchronously(theDataset, ctxt, request);
            return new PublishDatasetResult(theDataset, false);
        } catch (CommandException ce){
            throw new CommandException("Publish Dataset failed", this);
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
        
        if ( theDataset.isLockedFor(DatasetLock.Reason.Workflow)
                || theDataset.isLockedFor(DatasetLock.Reason.Ingest) ) {
            throw new IllegalCommandException("This dataset is locked. Reason: " 
                    + theDataset.getLocks().stream().map(l -> l.getReason().name()).collect( joining(",") )
                    + ". Please try publishing later.", this);
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
    
}
