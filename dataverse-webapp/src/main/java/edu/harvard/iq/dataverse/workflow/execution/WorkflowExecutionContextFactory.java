package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowContextSource;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionContextSource;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A factory responsible for creating a {@link WorkflowExecutionContext} from different sources of data.
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionContextFactory {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionContextFactory.class);

    private final SettingsServiceBean settings;

    private final DatasetVersionRepository datasetVersions;

    private final WorkflowRepository workflows;

    private final WorkflowExecutionRepository executions;

    private final RoleAssigneeServiceBean roleAssignees;

    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public WorkflowExecutionContextFactory() {
        this(null, null, null, null, null, null);
    }

    @Inject
    public WorkflowExecutionContextFactory(SettingsServiceBean settings, DatasetVersionRepository datasetVersions,
                                           WorkflowRepository workflows, WorkflowExecutionRepository executions,
                                           RoleAssigneeServiceBean roleAssignees) {
        this(settings, datasetVersions, workflows, executions, roleAssignees, Clock.systemUTC());
    }

    public WorkflowExecutionContextFactory(SettingsServiceBean settings, DatasetVersionRepository datasetVersions,
                                           WorkflowRepository workflows, WorkflowExecutionRepository executions,
                                           RoleAssigneeServiceBean roleAssignees, Clock clock) {
        this.settings = settings;
        this.datasetVersions = datasetVersions;
        this.workflows = workflows;
        this.executions = executions;
        this.roleAssignees = roleAssignees;
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
     * Creates resuming {@link WorkflowExecutionContext} using data stored in {@link WorkflowExecutionContextSource}.
     * @param source provided {@link WorkflowExecutionContextSource} to compose the context for.
     * @return ready to use {@link WorkflowExecutionContext}.
     */
    public WorkflowExecutionContext workflowExecutionContextOf(WorkflowExecutionContextSource source) {
        WorkflowExecution execution = executions.getById(source.getId());
        return workflowExecutionContextOf(execution);
    }

    /**
     * Creates resuming {@link WorkflowExecutionContext} using data stored in {@link WorkflowExecution}.
     * @param execution provided {@link WorkflowExecution} to compose the context for.
     * @return ready to use {@link WorkflowExecutionContext}.
     */
    public WorkflowExecutionContext workflowExecutionContextOf(WorkflowExecution execution) {
        Workflow workflow = workflows.getById(execution.getWorkflowId());
        log.trace("### Load {}", execution);
        return create(reCreateContext(execution), workflow, execution);
    }

    // -------------------- PRIVATE --------------------

    private WorkflowExecutionContext create(WorkflowContext context, Workflow workflow, WorkflowExecution execution) {
        Map<String, Object> settings = retrieveRequestedSettings(workflow.getRequiredSettings());
        return new WorkflowExecutionContext(workflow, context, execution, settings, clock);
    }

    private WorkflowContext reCreateContext(WorkflowContextSource source) {
        Awaitility.await(String.format("Dataset %s version %s.%s - wait until dataset exists", source.getDatasetId(), source.getVersionNumber(), source.getMinorVersionNumber()))
                .with()
                .pollDelay(Duration.ofMillis(100))
                .pollInterval(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(20))
                .until(() -> datasetVersions.findByDatasetIdAndVersionNumber(source), Optional::isPresent);
        DataverseRequest request = new DataverseRequest(
                (User) roleAssignees.getRoleAssignee(source.getUserId()),
                IpAddress.valueOf(source.getIpAddress()));
        return new WorkflowContext(
                WorkflowContext.TriggerType.valueOf(source.getTriggerType()),
                source.getDatasetId(), source.getVersionNumber(), source.getMinorVersionNumber(),
                request, source.isDatasetExternallyReleased());
    }

    private Map<String, Object> retrieveRequestedSettings(Map<String, String> requiredSettings) {
        Map<String, Object> retrievedSettings = new HashMap<>();
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
