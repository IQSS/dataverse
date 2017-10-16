package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
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

    boolean minorRelease;
    
    public PublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest, boolean minor) {
        super(datasetIn, aRequest);
        minorRelease = minor;
        theDataset = datasetIn;
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
            
        } else /* major, non-first release */ {
            theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
            theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
        }

        theDataset = ctxt.em().merge(theDataset);
        
        //Move remove lock to after merge... SEK 9/1/17 (why? -- L.A.)
        ctxt.engine().submit( new RemoveLockCommand(getRequest(), theDataset));

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
    
}
