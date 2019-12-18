package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.NoDatasetFilesException;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import io.vavr.control.Option;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@RequiredPermissions(Permission.EditDataset)
public class SubmitDatasetForReviewCommand extends AbstractDatasetCommand<Dataset> {

    public SubmitDatasetForReviewCommand(DataverseRequest aRequest, Dataset dataset) {
        super(aRequest, dataset);
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

        AuthenticatedUser requestor = getUser().isAuthenticated() ? (AuthenticatedUser) getUser() : null;

        List<AuthenticatedUser> authUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        for (AuthenticatedUser au : authUsers) {

            Option.of(requestor)
                    .peek(user -> ctxt.notifications().sendNotificationWithEmail(au, new Timestamp(new Date().getTime()), NotificationType.SUBMITTEDDS,
                                                                                 savedDataset.getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION, requestor))
                    .onEmpty(() -> ctxt.notifications().sendNotificationWithEmail(au, new Timestamp(new Date().getTime()), NotificationType.SUBMITTEDDS,
                                                                                  savedDataset.getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION));
        }

        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        return savedDataset;
    }

}
