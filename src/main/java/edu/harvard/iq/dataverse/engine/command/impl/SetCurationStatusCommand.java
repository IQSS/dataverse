package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
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

import org.apache.solr.client.solrj.SolrServerException;

import com.google.api.LabelDescriptor;

@RequiredPermissions(Permission.PublishDataset)
public class SetCurationStatusCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(SetCurationStatusCommand.class.getName());
    
    String label;
    
    public SetCurationStatusCommand(DataverseRequest aRequest, Dataset dataset, String label) {
        super(aRequest, dataset);
        this.label=label;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        if (getDataset().getLatestVersion().isReleased()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.status.failure.isReleased"), this);
        }
        if (label==null || label.isEmpty()) {
            getDataset().getLatestVersion().setExternalStatusLabel(null);
        } else {
            String setName = getDataset().getEffectiveCurationLabelSetName();
            if(setName.equals(SystemConfig.CURATIONLABELSDISABLED)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.status.failure.disabled"), this);
            }
            String[] labelArray = ctxt.systemConfig().getCurationLabels().get(setName);
            boolean found = false;
            for(String name: labelArray) {
                if(name.equals(label)) {
                    found=true;
                    getDataset().getLatestVersion().setExternalStatusLabel(label);
                    break;
                }
            }
            if(!found) {
                logger.fine("Label not found: " + label + " in set " + setName);
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.status.failure.notallowed"), this);
            }
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
        
        List<AuthenticatedUser> authUsers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        for (AuthenticatedUser au : authUsers) {
            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.STATUSUPDATED, savedDataset.getLatestVersion().getId(), "", requestor, false);
        }
        
        //  TODO: What should we do with the indexing result? Print it to the log?
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
