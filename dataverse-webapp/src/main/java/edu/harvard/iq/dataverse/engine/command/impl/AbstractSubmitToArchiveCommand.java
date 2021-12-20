package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequiredPermissions(Permission.PublishDataset)
public abstract class AbstractSubmitToArchiveCommand extends AbstractCommand<DatasetVersion> {

    private final DatasetVersion version;
    private final Map<String, String> requestedSettings = new HashMap<>();
    private static final Logger logger = Logger.getLogger(AbstractSubmitToArchiveCommand.class.getName());

    private final AuthenticationServiceBean authenticationService;
    private final Clock clock;

    public AbstractSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version,
                                          AuthenticationServiceBean authenticationService, Clock clock) {
        super(aRequest, version.getDataset());
        this.version = version;
        this.authenticationService = authenticationService;
        this.clock = clock;
    }

    protected ApiToken getOrGenerateToken(AuthenticatedUser user) {
        if (user == null) {
            return null;
        }
        Boolean INACTIVE_KEY = Boolean.TRUE;
        Boolean ACTIVE_KEY = Boolean.FALSE;

        Timestamp now = Timestamp.from(clock.instant());
        List<ApiToken> tokens = authenticationService.findAllApiTokensByUser(user);
        Map<Boolean, List<ApiToken>> inactiveAndActiveTokens = tokens.stream()
                .collect(Collectors.groupingBy(t -> t.getExpireTime().before(now)));
        List<Long> idsToDelete = inactiveAndActiveTokens.getOrDefault(INACTIVE_KEY, Collections.emptyList()).stream()
                .map(ApiToken::getId)
                .collect(Collectors.toList());
        if (!idsToDelete.isEmpty()) {
            authenticationService.deleteApiTokensByIds(idsToDelete);
        }
        List<ApiToken> active = inactiveAndActiveTokens.getOrDefault(ACTIVE_KEY, Collections.emptyList());
        return active.isEmpty() ? authenticationService.generateApiTokenForUser(user) : active.get(0);
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

        performArchiveSubmission(version, requestedSettings, ctxt.citationFactory());
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
     * @param requestedSettings - a map of the names/values for settings required by this archiver (sent because this class is not part of the EJB context (by design) and has no direct access to service beans).
     */
    abstract public WorkflowStepResult performArchiveSubmission(
            DatasetVersion version, Map<String, String> requestedSettings, CitationFactory citationFactory);

    @Override
    public String describe() {
        return super.describe() + "DatasetVersion: [" + version.getId() + " (v"
                + version.getFriendlyVersionNumber() + ")]";
    }
}
