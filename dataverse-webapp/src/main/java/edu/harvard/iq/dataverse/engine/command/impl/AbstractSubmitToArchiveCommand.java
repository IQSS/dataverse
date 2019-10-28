package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RequiredPermissions(Permission.PublishDataset)
public abstract class AbstractSubmitToArchiveCommand extends AbstractCommand<DatasetVersion> {

    private final DatasetVersion version;
    private final Map<String, String> requestedSettings = new HashMap<String, String>();
    private static final Logger logger = Logger.getLogger(AbstractSubmitToArchiveCommand.class.getName());

    public AbstractSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version.getDataset());
        this.version = version;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt)  {

        List<String> settingNames = ctxt.settings().getValueForKeyAsList(SettingsServiceBean.Key.ArchiverSettings);
        for (String setting : settingNames) {
            setting = setting.trim();
            if (!setting.startsWith(":")) {
                logger.warning("Invalid Archiver Setting: " + setting);
            } else {
                requestedSettings.put(setting, ctxt.settings().get(setting));
            }
        }

        requestedSettings.put(SettingsServiceBean.Key.SiteUrl.toString(), ctxt.systemConfig().getDataverseSiteUrl());


        AuthenticatedUser user = getRequest().getAuthenticatedUser();
        ApiToken token = ctxt.authentication().findApiTokenByUser(user);
        if ((token == null) || (token.getExpireTime().before(new Date()))) {
            token = ctxt.authentication().generateApiTokenForUser(user);
        }
        performArchiveSubmission(version, token, requestedSettings);
        return ctxt.em().merge(version);
    }

    /**
     * This method is the only one that should be overwritten by other classes. Note
     * that this method may be called from the execute method above OR from a
     * workflow in which execute() is never called and therefore in which all
     * variables must be sent as method parameters. (Nominally version is set in the
     * constructor and could be dropped from the parameter list.)
     *
     * @param version           - the DatasetVersion to archive
     * @param token             - an API Token for the user performing this action
     * @param requestedSettings - a map of the names/values for settings required by this archiver (sent because this class is not part of the EJB context (by design) and has no direct access to service beans).
     */
    abstract public WorkflowStepResult performArchiveSubmission(DatasetVersion version, ApiToken token, Map<String, String> requestedSetttings);

    @Override
    public String describe() {
        return super.describe() + "DatasetVersion: [" + version.getId() + " (v"
                + version.getFriendlyVersionNumber() + ")]";
    }

}
