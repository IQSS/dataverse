package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowContextSource;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory responsible for creating a {@link WorkflowExecutionContext} from different sources of data.
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionContextFactory {

    private final SettingsServiceBean settings;

    private final DatasetRepository datasets;

    private final WorkflowRepository workflows;

    private final WorkflowExecutionRepository executions;

    private final RoleAssigneeServiceBean roleAssignees;

    private final AuthenticationServiceBean authentication;

    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public WorkflowExecutionContextFactory() {
        this(null, null, null, null, null, null);
    }

    @Inject
    public WorkflowExecutionContextFactory(SettingsServiceBean settings, DatasetRepository datasets,
                                           WorkflowRepository workflows, WorkflowExecutionRepository executions,
                                           RoleAssigneeServiceBean roleAssignees, AuthenticationServiceBean authentication) {
        this(settings, datasets, workflows, executions, roleAssignees, authentication, Clock.systemUTC());
    }

    public WorkflowExecutionContextFactory(SettingsServiceBean settings, DatasetRepository datasets,
                                           WorkflowRepository workflows, WorkflowExecutionRepository executions,
                                           RoleAssigneeServiceBean roleAssignees, AuthenticationServiceBean authentication,
                                           Clock clock) {
        this.settings = settings;
        this.datasets = datasets;
        this.workflows = workflows;
        this.executions = executions;
        this.roleAssignees = roleAssignees;
        this.authentication = authentication;
        this.clock = clock;
    }
    // -------------------- LOGIC --------------------

    /**
     * Creates starting {@link WorkflowExecutionContext} using initial data.
     * @param context provided {@link WorkflowContext} to use.
     * @param workflow a {@link Workflow} to execute.
     * @return ready to use {@link WorkflowExecutionContext}.
     */
    public WorkflowExecutionContext workflowExecutionContextOf(WorkflowContext context, Workflow workflow) {
        return create(context, workflow, context.asExecutionOf(workflow));
    }

    /**
     * Creates resuming {@link WorkflowExecutionContext} using data stored in {@link WorkflowExecution}.
     * @param execution provided {@link WorkflowExecution} to compose the context for.
     * @return ready to use {@link WorkflowExecutionContext}.
     */
    public WorkflowExecutionContext workflowExecutionContextOf(WorkflowExecution execution) {
        Workflow workflow = workflows.findById(execution.getWorkflowId()).orElseThrow(() ->
                new IllegalStateException("Executed workflow no longer exists"));
        return create(reCreateContext(execution), workflow, execution);
    }

    /**
     * Creates resuming {@link WorkflowExecutionContext} using data stored in {@link WorkflowExecutionMessage}.
     * @param message provided {@link WorkflowExecutionMessage} to compose the context for.
     * @return ready to use {@link WorkflowExecutionContext}.
     */
    public WorkflowExecutionContext workflowExecutionContextOf(WorkflowExecutionMessage message) {
        Workflow workflow = workflows.findById(message.getWorkflowId()).orElseThrow(() ->
                new IllegalStateException("Executed workflow no longer exists"));
        WorkflowExecution workflowExecution = executions.getById(message.getWorkflowExecutionId());
        return create(reCreateContext(message), workflow, workflowExecution);
    }

    // -------------------- PRIVATE --------------------

    private WorkflowExecutionContext create(WorkflowContext context, Workflow workflow, WorkflowExecution execution) {
        ApiToken apiToken = getCurrentApiToken(context.getRequest().getAuthenticatedUser());
        Map<String, Object> settings = retrieveRequestedSettings(workflow.getRequiredSettings());
        return new WorkflowExecutionContext(workflow, context, execution, apiToken, settings);
    }

    private WorkflowContext reCreateContext(WorkflowContextSource source) {
        Dataset dataset = datasets.findById(source.getDatasetId()).orElseThrow(() ->
                new IllegalStateException("Target dataset no longer exists"));
        DataverseRequest request = new DataverseRequest(
                (User) roleAssignees.getRoleAssignee(source.getUserId()),
                IpAddress.valueOf(source.getIpAddress()));
        return new WorkflowContext(
                WorkflowContext.TriggerType.valueOf(source.getTriggerType()),
                dataset, source.getMajorVersionNumber(), source.getMinorVersionNumber(),
                request, source.isDatasetExternallyReleased());
    }

    private ApiToken getCurrentApiToken(AuthenticatedUser au) {
        if (au != null) {
            ApiToken token = authentication.findApiTokenByUser(au);
            if ((token == null) || (token.getExpireTime().before(Timestamp.from(clock.instant())))) {
                token = authentication.generateApiTokenForUser(au);
            }
            return token;
        }
        return null;
    }

    private Map<String, Object> retrieveRequestedSettings(Map<String, String> requiredSettings) {
        Map<String, Object> retrievedSettings = new HashMap<String, Object>();
        for (String setting : requiredSettings.keySet()) {
            String settingType = requiredSettings.get(setting);
            switch (settingType) {
                case "string": {
                    retrievedSettings.put(setting, settings.get(setting));
                    break;
                }
                case "boolean": {
                    retrievedSettings.put(setting, settings.isTrue(settingType));
                    break;
                }
                case "long": {
                    retrievedSettings.put(setting, settings.getValueForKeyAsLong(SettingsServiceBean.Key.valueOf(setting)));
                    break;
                }
            }
        }
        return retrievedSettings;
    }
}
