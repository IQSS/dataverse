package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.CurationStatus;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.util.Strings;
import org.apache.solr.client.solrj.SolrServerException;

import com.google.api.LabelDescriptor;

@RequiredPermissions(Permission.PublishDataset)
public class SetCurationStatusCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(SetCurationStatusCommand.class.getName());

    String label;

    public SetCurationStatusCommand(DataverseRequest aRequest, Dataset dataset, String label) {
        super(aRequest, dataset);
        this.label = label;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        DatasetVersion version = getDataset().getLatestVersion();
        if (version.isReleased()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.curationstatus.failure.isReleased"), this);
        }
        CurationStatus currentStatus = version.getCurrentCurationStatus();

        CurationStatus status = null;
        if (((currentStatus == null || Strings.isBlank(currentStatus.getLabel())) && Strings.isNotBlank(label)) ||
                (currentStatus != null && !currentStatus.getLabel().equals(label))) {
            status = new CurationStatus(label, version, getRequest().getAuthenticatedUser());
        }

        String setName = getDataset().getEffectiveCurationLabelSetName();
        if (setName.equals(SystemConfig.CURATIONLABELSDISABLED)) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.curationstatus.failure.disabled"), this);
        }
        if (status != null) {
            boolean found = false;
            if (status.getLabel() != null) {
                String[] labelArray = ctxt.systemConfig().getCurationLabels().get(setName);
                for (String name : labelArray) {
                    if (name.equals(label)) {
                        found = true;
                        version.addCurationStatus(status);
                        break;
                    }
                }
            } else {
                //
                found = true;
                version.addCurationStatus(status);
            }
            if (!found) {
                logger.fine("Label not found: " + label + " in set " + setName);
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.curationstatus.failure.notallowed"), this);
            }
        } else {
            logger.fine("Attempt to reset with the same label : " + label);
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.curationstatus.failure.noChange"), this);
        }
        Dataset updatedDataset = save(ctxt);
        return updatedDataset;
    }

    public Dataset save(CommandContext ctxt) throws CommandException {

        getDataset().getOrCreateEditVersion().setLastUpdateTime(getTimestamp());
        getDataset().setModificationTime(getTimestamp());

        Dataset savedDataset = ctxt.em().merge(getDataset());
        ctxt.em().flush();

        updateDatasetUser(ctxt);

        AuthenticatedUser requestor = getUser().isAuthenticated() ? (AuthenticatedUser) getUser() : null;

        boolean showToAll = JvmSettings.UI_SHOW_CURATION_STATUS_TO_ALL.lookupOptional(Boolean.class).orElse(false);

        List<AuthenticatedUser> authUsers;
        if (showToAll) {
            authUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.ViewUnpublishedDataset, savedDataset);
        } else {
            authUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        }
        for (AuthenticatedUser au : authUsers) {
            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.STATUSUPDATED, savedDataset.getLatestVersion().getId(), "", requestor, false);
        }

        // TODO: What should we do with the indexing result? Print it to the log?
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
