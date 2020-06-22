package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.handler.WorkflowFailureHandler;
import edu.harvard.iq.dataverse.workflow.handler.WorkflowSuccessHandler;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType.PrePublishDataset;
import static java.util.Collections.emptyMap;

/**
 * Service bean for executing {@link Workflow}s
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionServiceBean {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionServiceBean.class);

    @Inject
    private SettingsServiceBean settings;

    @Inject
    private DatasetRepository datasets;

    @Inject
    private WorkflowRepository workflows;

    @Inject
    private WorkflowStepRegistry steps;

    @Inject
    private WorkflowExecutionRepository executions;

    @EJB
    private RoleAssigneeServiceBean roleAssignees;

    @EJB
    private EjbDataverseEngine engine;

    @Inject
    private Instance<WorkflowSuccessHandler> workflowSuccessHandlers;

    @Inject
    private Instance<WorkflowFailureHandler> workflowFailureHandlers;

    private final Clock clock;

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowExecutionServiceBean() {
        this(Clock.systemUTC());
    }

    public WorkflowExecutionServiceBean(Clock clock) {
        this.clock = clock;
    }

    // -------------------- LOGIC --------------------

    /**
     * Starts executing workflow {@code wf} under the passed context.
     *
     * @param workflow   the workflow to execute.
     * @param ctx the context in which the workflow is executed.
     * @throws CommandException If the dataset could not be locked.
     */
    @Asynchronous
    public void start(Workflow workflow, WorkflowContext ctx)  {
        lockDatasetForWorkflow(ctx);
        WorkflowExecutionContext context = create(ctx, workflow, ctx.asExecutionOf(workflow));
        context.start(executions, clock);
        executeSteps(context, null);
    }

    public Optional<WorkflowExecution> getPendingWorkflow(String invocationId) {
        return executions.findByInvocationId(invocationId)
                .filter(WorkflowExecution::isPaused);
    }

    /**
     * Starting the resume process for a pending workflow. We first delete the
     * pending workflow to minimize double invocation, and then asynchronously
     * resume the work.
     *
     * @param execution Workflow execution to resume.
     * @param externalData the response from the remote system.
     */
    @Asynchronous
    public void resume(WorkflowExecution execution, String externalData) {
        Workflow wf = workflows.findById(execution.getWorkflowId()).orElseThrow(() ->
                new IllegalStateException("Executed workflow no longer exists"));
        WorkflowContext ctx = reCreateContext(execution);
        WorkflowExecutionContext executionCtx = create(ctx, wf, execution);
        executeSteps(executionCtx, externalData);
    }

    // -------------------- PRIVATE --------------------

    private WorkflowContext reCreateContext(WorkflowExecution execution) {
        Dataset dataset = datasets.findById(execution.getId()).orElseThrow(() ->
                new IllegalStateException("Target dataset no longer exists"));
        DataverseRequest request = new DataverseRequest(
                (User) roleAssignees.getRoleAssignee(execution.getUserId()),
                IpAddress.valueOf(execution.getIpAddress()));
        return new WorkflowContext(
                TriggerType.valueOf(execution.getTriggerType()),
                dataset, execution.getMajorVersionNumber(), execution.getMajorVersionNumber(),
                request, execution.isDatasetExternallyReleased());
    }

    private void lockDatasetForWorkflow(WorkflowContext ctx) {
        log.trace("Creating workflow lock");
        runInNewTransaction(() ->
                datasets.lockDataset(ctx.getDataset(), ctx.getRequest().getAuthenticatedUser(), DatasetLock.Reason.Workflow)
        );
    }
    private void unlockDatasetForWorkflow(WorkflowContext ctx) {
        log.trace("Removing workflow lock");
        runInNewTransaction(() ->
                datasets.unlockDataset(ctx.getDataset(), DatasetLock.Reason.Workflow)
        );
    }

    /**
     * Execute the passed workflow, starting from {@code initialStepIdx}.
     *
     * @param ctx Execution context to run the workflow in.
     * @param externalData the response from the remote system.
     */
    private void executeSteps(WorkflowExecutionContext ctx, String externalData) {
        Map<String, String> stepParameters = new HashMap<>();
        while (ctx.hasMoreStepsToExecute()) {
            WorkflowExecutionStepContext step = ctx.nextStepToExecute(executions);
            try {
                WorkflowStepResult stepResult;
                if (step.isPaused()) {
                    stepResult = getInNewTransaction(
                            step.resume(externalData, steps, clock)
                    );
                } else {
                    stepResult = getInNewTransaction(
                            step.start(stepParameters, steps, clock)
                    );
                }

                if (stepResult instanceof Success) {
                    log.info("{} finished successfully", step);
                    step.success(stepResult.getData(), clock);
                    ctx.save(datasets);
                    ctx = refresh(ctx);
                    stepParameters.clear();
                    stepParameters.putAll(stepResult.getData());

                } else if (stepResult instanceof Failure) {
                    log.warn(String.format("%s failed - rolling back", step), ((Failure) stepResult).getReason());
                    step.failure(stepResult.getData(), clock);
                    rollback(ctx, (Failure) stepResult);
                    return;

                } else if (stepResult instanceof Pending) {
                    log.info("{} paused", step);
                    step.pause(stepResult.getData(), clock);
                    return;
                }
            } catch (Exception e) {
                log.warn(String.format("%s failed - rolling back", step), e);
                step.failure(emptyMap(), clock);
                rollback(ctx, new Failure(e.getMessage()));
                return;
            }
        }

        workflowCompleted(ctx);
    }

    private void workflowCompleted(WorkflowExecutionContext ctx) {
        log.info("Workflow {} completed.", ctx.getInvocationId());

        try {
            ctx.finish(executions, clock);

            unlockDatasetForWorkflow(ctx);
            if (ctx.getType() == PrePublishDataset) {
                engine.submit(new FinalizeDatasetPublicationCommand(ctx.getDataset(), ctx.getRequest(),
                        ctx.isDatasetExternallyReleased()));
            }
        } catch (CommandException ex) {
            log.error("Exception finalizing workflow " + ctx.getInvocationId() + ": " + ex.getMessage(), ex);
            rollback(ctx, new Failure("Exception while finalizing the publication: " + ex.getMessage()));
        }

        workflowSuccessHandlers.forEach(success -> success.handleSuccess(ctx));
    }

    private void rollback(WorkflowExecutionContext ctx, Failure failure) {
        WorkflowExecutionContext refreshedCtx = refresh(ctx);
        refreshedCtx.finish(executions, clock);

        while (refreshedCtx.hasMoreStepsToRollback()) {
            WorkflowExecutionStepContext step = refreshedCtx.nextStepToRollback(executions);
            try {
                log.info("{} - rollback", step);
                runInNewTransaction(
                        step.rollback(failure, steps, clock)
                );
            } catch (Exception e) {
                log.warn(String.format("%s - rollback error", step), e);
            }
        }

        try {
            unlockDatasetForWorkflow(refreshedCtx);
        } catch (CommandException ex) {
            log.error("Error restoring dataset locks state after rollback: " + ex.getMessage(), ex);
        }

        workflowFailureHandlers.forEach(failureHandler -> failureHandler.handleFailure(refreshedCtx));
    }

    private WorkflowExecutionContext refresh(WorkflowExecutionContext ctx) {
        return create(ctx, ctx.getWorkflow(), ctx.getExecution());
    }

    private WorkflowExecutionContext create(WorkflowContext ctx, Workflow workflow, WorkflowExecution execution) {
        ApiToken apiToken = getCurrentApiToken(ctx.getRequest().getAuthenticatedUser());
        Map<String, Object> settings = retrieveRequestedSettings(workflow.getRequiredSettings());
        return new WorkflowExecutionContext(workflow, ctx, execution, apiToken, settings);
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

    private ApiToken getCurrentApiToken(AuthenticatedUser au) {
        if (au != null) {
            CommandContext ctxt = engine.getContext();
            ApiToken token = ctxt.authentication().findApiTokenByUser(au);
            if ((token == null) || (token.getExpireTime().before(new Date()))) {
                token = ctxt.authentication().generateApiTokenForUser(au);
            }
            return token;
        }
        return null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    <T> T getInNewTransaction(Supplier<T> logic) {
        return logic.get();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    void runInNewTransaction(Runnable logic) {
        logic.run();
    }
}
