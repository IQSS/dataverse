package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@RequiredPermissions(Permission.PublishDataset)
public abstract class AbstractSubmitToArchiveCommand implements Command<DatasetVersion> {

    private final DatasetVersion version;
    private final DataverseRequest request;
    private final Map<String, DvObject> affectedDvObjects;
    private final Map<String, String> requestedSettings = new HashMap<String, String>();
    private static final Logger logger = Logger.getLogger(AbstractSubmitToArchiveCommand.class.getName());
    
    public AbstractSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        this.request = aRequest;
        this.version = version;
        affectedDvObjects = new HashMap<>();
        affectedDvObjects.put("", version.getDataset());
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {

        String settings = ctxt.settings().getValueForKey(SettingsServiceBean.Key.ArchiverSettings);
        String[] settingsArray = settings.split(",");
        for (String setting : settingsArray) {
            setting = setting.trim();
            if (!setting.startsWith(":")) {
                logger.warning("Invalid Archiver Setting: " + setting);
            } else {
                requestedSettings.put(setting, ctxt.settings().get(setting));
            }
        }
        
        AuthenticatedUser user = request.getAuthenticatedUser();
        ApiToken token = ctxt.authentication().findApiTokenByUser(user);
        if ((token == null) || (token.getExpireTime().before(new Date()))) {
            token = ctxt.authentication().generateApiTokenForUser(user);
        }
        performArchiveSubmission(version, token, requestedSettings);
        return ctxt.em().merge(version);
    }

    @Override
    public Map<String, DvObject> getAffectedDvObjects() {

        return affectedDvObjects;
    }

    @Override
    public DataverseRequest getRequest() {
        return request;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return CH.permissionsRequired(getClass());
    }

    @Override
    public String describe() {
        // Is this ever used?
        return "DatasetVersion: " + version.getId() + " " + version.getDataset().getDisplayName() + ".v"
                + version.getFriendlyVersionNumber();
    }

    /**
     * This method is the only one that should be overwritten by other classes. Note
     * that this method may be called from the execute method above OR from a
     * workflow in which execute() is never called and therefore in which all
     * variables must be sent as method parameters. (Nominally version is set in the
     * constructor and could be dropped from the parameter list.)
     * 
     * @param version - the DatasetVersion to archive
     * @param token - an API Token for the user performing this action
     * @param requestedSettings - a map of the names/values for settings required by this archiver (sent because this class is not part of the EJB context (by design) and has no direct access to service beans).
     */
    abstract public WorkflowStepResult performArchiveSubmission(DatasetVersion version, ApiToken token, Map<String, String> requestedSetttings);

}
