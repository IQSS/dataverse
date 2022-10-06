package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.NoDatasetFilesException;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.User;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredPermissions(Permission.EditDataset)
public class SubmitDatasetForReviewCommand extends AbstractDatasetCommand<Dataset> {

    private final String comment;

    public SubmitDatasetForReviewCommand(DataverseRequest aRequest, Dataset dataset, String comment) {
        super(aRequest, dataset);
        this.comment = comment;
    }

    @Override
    public Dataset execute(CommandContext ctxt)  {

        if (getDataset().getLatestVersion().isReleased()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.submit.failure.isReleased"), this);
        }

        if (getDataset().getLatestVersion().isInReview()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.submit.failure.inReview"), this);
        }

        if (getDataset().getLatestVersion().getFileMetadatas().isEmpty()) {
            throw new NoDatasetFilesException("There was no files for dataset version with id: " + getDataset().getLatestVersion().getId());
        }

        //SEK 9-1 Add Lock before saving dataset
        DatasetLock inReviewLock = new DatasetLock(DatasetLock.Reason.InReview, getRequest().getAuthenticatedUser());
        ctxt.engine().submit(new AddLockCommand(getRequest(), getDataset(), inReviewLock));

        return save(ctxt);
    }

    public Dataset save(CommandContext ctxt)  {

        getDataset().getEditVersion().setLastUpdateTime(getTimestamp());
        getDataset().setModificationTime(getTimestamp());

        Dataset savedDataset = ctxt.em().merge(getDataset());
        ctxt.em().flush();

        updateDatasetUser(ctxt);

        sendNotification(ctxt, savedDataset);
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        return savedDataset;
    }

    private void sendNotification(CommandContext ctxt, Dataset savedDataset) {
        User user = getUser();
        if (user == null || !user.isAuthenticated()) {
            return;
        }
        List<AuthenticatedUser> curators = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        AuthenticatedUser requestor = (AuthenticatedUser) user;
        Timestamp timestamp = new Timestamp(new Date().getTime());
        Map<String, String> parameters = new HashMap<>();
        parameters.put(NotificationParameter.REQUESTOR_ID.key(), String.valueOf(requestor.getId()));
        parameters.put(NotificationParameter.MESSAGE.key(), comment);

        curators.forEach(c -> ctxt.notifications()
                .sendNotificationWithEmail(c, timestamp, NotificationType.SUBMITTEDDS,
                        savedDataset.getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION, parameters));
    }
}
