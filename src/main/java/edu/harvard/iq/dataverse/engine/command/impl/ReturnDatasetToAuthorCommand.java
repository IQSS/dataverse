package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrServerException;

@RequiredPermissions(Permission.PublishDataset)
public class ReturnDatasetToAuthorCommand extends AbstractDatasetCommand<Dataset> {

    private final String comment;

    public ReturnDatasetToAuthorCommand(DataverseRequest aRequest, Dataset anAffectedDvObject, String comment) {
        super(aRequest, anAffectedDvObject);

        if (comment == null || comment.isEmpty()) {
            throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataset.reject.commentNull"));
        }

        this.comment = comment;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        final Dataset dataset = getDataset();
        
        if (!dataset.getLatestVersion().isInReview()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.reject.datasetNotInReview"), this);
        }
        
        dataset.getOrCreateEditVersion().setLastUpdateTime(getTimestamp());
        dataset.setModificationTime(getTimestamp());
        
        ctxt.engine().submit( new RemoveLockCommand(getRequest(), getDataset(), DatasetLock.Reason.InReview) );
        WorkflowComment workflowComment = new WorkflowComment(dataset.getOrCreateEditVersion(), WorkflowComment.Type.RETURN_TO_AUTHOR, comment, (AuthenticatedUser) this.getUser());
        ctxt.datasets().addWorkflowComment(workflowComment);

        updateDatasetUser(ctxt);
        ctxt.em().flush();
        Dataset savedDataset = ctxt.em().merge(dataset);
        
        /*
            So what we're doing here is sending notifications to the authors who do not have publish permissions
            First get users who can publish - or in this case review
            Then get authors.
            Then remove reviewers from the autors list
            Finally send a notification to the remaining (non-reviewing) authors - Hey! your dataset was rejected.
        */
        List<AuthenticatedUser> reviewers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        List<AuthenticatedUser> authors   = ctxt.permissions().getUsersWithPermissionOn(Permission.EditDataset, savedDataset);
        authors.removeAll(reviewers);
        for (AuthenticatedUser au : authors) {
            ctxt.notifications().sendNotification(au, getTimestamp(), UserNotification.Type.RETURNEDDS, savedDataset.getLatestVersion().getId(), comment);
        }

        
        return savedDataset;
        
    }
    
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        boolean retVal = true;
        Dataset dataset = (Dataset) r;

        ctxt.index().asyncIndexDataset(dataset, true);

        return retVal;
    }

}
