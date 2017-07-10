package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DvObject;
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

@RequiredPermissions(Permission.PublishDataset)
public class ReturnDatasetToAuthorCommand extends AbstractCommand<Dataset> {

    private final Dataset theDataset;

    public ReturnDatasetToAuthorCommand(DataverseRequest aRequest, DvObject anAffectedDvObject) {
        super(aRequest, anAffectedDvObject);
        this.theDataset = (Dataset) anAffectedDvObject;
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
        return save(ctxt);
    }

    public Dataset save(CommandContext ctxt) throws CommandException {

        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        // We set "in review" to false because now the ball is back in the author's court.
        theDataset.getEditVersion().setInReview(false);
        theDataset.setModificationTime(updateTime);

        Dataset savedDataset = ctxt.em().merge(theDataset);
        ctxt.em().flush();

        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(theDataset.getLatestVersion(), this.getUser());

        if (ddu != null) {
            ddu.setLastUpdateDate(updateTime);
            ctxt.em().merge(ddu);
        } else {
            // TODO: This logic to update the DatasetVersionUser was copied from UpdateDatasetCommand and also appears in CreateDatasetCommand, PublishDatasetCommand UpdateDatasetCommand, and SubmitDatasetForReviewCommand. Consider consolidating.
            DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
            datasetDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
            datasetDataverseUser.setLastUpdateDate((Timestamp) updateTime);
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            datasetDataverseUser.setAuthenticatedUser(au);
            ctxt.em().merge(datasetDataverseUser);
        }
        List<AuthenticatedUser> authUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        List<AuthenticatedUser> editUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.EditDataset, savedDataset);
        for (AuthenticatedUser au : authUsers) {
            editUsers.remove(au);
        }
        for (AuthenticatedUser au : editUsers) {
            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.RETURNEDDS, savedDataset.getLatestVersion().getId());
        }
        // TODO: What should we do with the indexing result? Print it to the log?
        boolean doNormalSolrDocCleanUp = true;
        Future<String> indexingResult = ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        return savedDataset;
    }

}
