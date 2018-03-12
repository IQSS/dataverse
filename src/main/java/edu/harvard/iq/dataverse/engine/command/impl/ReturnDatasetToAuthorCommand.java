package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

@RequiredPermissions(Permission.PublishDataset)
public class ReturnDatasetToAuthorCommand extends AbstractCommand<Dataset> {

    private final Dataset theDataset;
    private final String comment;

    public ReturnDatasetToAuthorCommand(DataverseRequest aRequest, Dataset anAffectedDvObject, String comment) {
        super(aRequest, anAffectedDvObject);
        this.theDataset =  anAffectedDvObject;
        this.comment = comment;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        if (theDataset == null) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.reject.datasetNull"), this);
        }

        if (!theDataset.getLatestVersion().isInReview()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.reject.datasetNotInReview"), this);
        }
        /*
        if(theDataset.getLatestVersion().getReturnReason() == null || theDataset.getLatestVersion().getReturnReason().isEmpty()){
             throw new IllegalCommandException("You must enter a reason for returning a dataset to the author(s).", this);
        }
         */
        ctxt.engine().submit( new RemoveLockCommand(getRequest(), theDataset, DatasetLock.Reason.InReview));
        Dataset updatedDataset = save(ctxt);
        return updatedDataset;
        
    }

    public Dataset save(CommandContext ctxt) throws CommandException {

        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        // We set "in review" to false because now the ball is back in the author's court.
        theDataset.setModificationTime(updateTime);
        // TODO: ctxt.datasets().removeDatasetLocks() doesn't work. Try RemoveLockCommand?
        AuthenticatedUser authenticatedUser = null;
        for (DatasetLock lock : theDataset.getLocks()) {
            if (DatasetLock.Reason.InReview.equals(lock.getReason())) {
                theDataset.removeLock(lock);
                // TODO: Are we supposed to remove the dataset lock from the user? What's going on here?
                authenticatedUser = lock.getUser();
            }
        }
        Dataset savedDataset = ctxt.em().merge(theDataset);
        ctxt.em().flush();

        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(theDataset.getLatestVersion(), this.getUser());
        
        WorkflowComment workflowComment = new WorkflowComment(theDataset.getEditVersion(), WorkflowComment.Type.RETURN_TO_AUTHOR, comment, (AuthenticatedUser) this.getUser());
        ctxt.datasets().addWorkflowComment(workflowComment);

        if (ddu != null) {
            ddu.setLastUpdateDate(updateTime);
            ctxt.em().merge(ddu);
        } else {
            // TODO: This logic to update the DatasetVersionUser was copied from UpdateDatasetCommand and also appears in CreateDatasetCommand, PublishDatasetCommand UpdateDatasetCommand, and SubmitDatasetForReviewCommand. Consider consolidating.
            DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
            datasetDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
            datasetDataverseUser.setLastUpdateDate(updateTime);
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            datasetDataverseUser.setAuthenticatedUser(au);
            ctxt.em().merge(datasetDataverseUser);
        }
        /*
            So what we're doing here is sending notifications to the authors who do not have publish permissions
            First get users who can publish - or in this case review
            Then get authors.
            Then remove reviewers from the autors list
            Finally send a notification to the remaining (non-reviewing) authors - Hey! your dataset was rejected.
        */
        List<AuthenticatedUser> reviewers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        List<AuthenticatedUser> authors = ctxt.permissions().getUsersWithPermissionOn(Permission.EditDataset, savedDataset);
        for (AuthenticatedUser au : reviewers) {
            authors.remove(au);
        }
        for (AuthenticatedUser au : authors) {
            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.RETURNEDDS, savedDataset.getLatestVersion().getId(), comment);
        }
        // TODO: What should we do with the indexing result? Print it to the log?
        boolean doNormalSolrDocCleanUp = true;
        Future<String> indexingResult = ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        return savedDataset;
    }

}
