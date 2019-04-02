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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

@RequiredPermissions(Permission.EditDataset)
public class SubmitDatasetForReviewCommand extends AbstractDatasetCommand<Dataset> {

    public SubmitDatasetForReviewCommand(DataverseRequest aRequest, Dataset dataset) {
        super(aRequest, dataset);
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        if (getDataset().getLatestVersion().isReleased()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.submit.failure.isReleased"), this);
        }

        if (getDataset().getLatestVersion().isInReview()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.submit.failure.inReview"), this);
        }

        //SEK 9-1 Add Lock before saving dataset
        DatasetLock inReviewLock = new DatasetLock(DatasetLock.Reason.InReview, getRequest().getAuthenticatedUser());
        ctxt.engine().submit(new AddLockCommand(getRequest(), getDataset(), inReviewLock));       
        Dataset updatedDataset = save(ctxt);
        
        return updatedDataset;
    }

    public Dataset save(CommandContext ctxt) throws CommandException {

        getDataset().getEditVersion().setLastUpdateTime(getTimestamp());
        getDataset().setModificationTime(getTimestamp());

        Dataset savedDataset = ctxt.em().merge(getDataset());
        ctxt.em().flush();

        updateDatasetUser(ctxt);

        AuthenticatedUser requestor = getUser().isAuthenticated() ? (AuthenticatedUser) getUser() : null;
        
        List<AuthenticatedUser> authUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        for (AuthenticatedUser au : authUsers) {
            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.SUBMITTEDDS, savedDataset.getLatestVersion().getId(), "", requestor);
        }
        
        //  TODO: What should we do with the indexing result? Print it to the log?
        boolean doNormalSolrDocCleanUp = true;
        Future<String> indexingResult = ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        return savedDataset;
    }

}
