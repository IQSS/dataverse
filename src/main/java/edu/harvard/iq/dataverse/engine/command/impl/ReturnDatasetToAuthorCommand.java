package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.util.List;

@RequiredPermissions(Permission.PublishDataset)
public class ReturnDatasetToAuthorCommand extends AbstractDatasetCommand<Dataset> {

    private final String comment;

    public ReturnDatasetToAuthorCommand(DataverseRequest aRequest, Dataset anAffectedDvObject, String comment) {
        super(aRequest, anAffectedDvObject);
        this.comment = comment;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        if (!getDataset().getLatestVersion().isInReview()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.reject.datasetNotInReview"), this);
        }
        
        ctxt.engine().submit( new RemoveLockCommand(getRequest(), getDataset(), DatasetLock.Reason.InReview));
        Dataset updatedDataset = save(ctxt);
        return updatedDataset;
        
    }

    public Dataset save(CommandContext ctxt) throws CommandException {

        final Dataset dataset = getDataset();
        dataset.getEditVersion().setLastUpdateTime(getTimestamp());
        dataset.setModificationTime(getTimestamp());
        
        Dataset savedDataset = ctxt.em().merge(dataset);
        ctxt.em().flush();

        ctxt.engine().submit( new RemoveLockCommand(getRequest(), getDataset(), DatasetLock.Reason.InReview) );
        WorkflowComment workflowComment = new WorkflowComment(dataset.getEditVersion(), WorkflowComment.Type.RETURN_TO_AUTHOR, comment, (AuthenticatedUser) this.getUser());
        ctxt.datasets().addWorkflowComment(workflowComment);

        updateDatasetUser(ctxt);
        
        /*
            So what we're doing here is sending notifications to the authors who do not have publish permissions
            First get users who can publish - or in this case review
            Then get authors.
            Then remove reviewers from the autors list
            Finally send a notification to the remaining (non-reviewing) authors - Hey! your dataset was rejected.
        */
        List<AuthenticatedUser> reviewers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        List<AuthenticatedUser> authors = ctxt.permissions().getUsersWithPermissionOn(Permission.EditDataset, savedDataset);
        authors.removeAll(reviewers);
        for (AuthenticatedUser au : authors) {
            ctxt.notifications().sendNotification(au, getTimestamp(), UserNotification.Type.RETURNEDDS, savedDataset.getLatestVersion().getId(), comment);
        }
        reindexDataset(ctxt);
        return savedDataset;
    }

}
