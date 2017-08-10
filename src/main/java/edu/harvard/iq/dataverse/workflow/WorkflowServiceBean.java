package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RemoveLockCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.internalspi.InternalWorkflowStepSP;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepData;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Service bean for managing and executing {@link Workflow}s
 *
 * @author michael
 */
@Stateless
public class WorkflowServiceBean {

    private static final Logger logger = Logger.getLogger(WorkflowServiceBean.class.getName());
    private static final String WORKFLOW_ID_KEY = "WorkflowServiceBean.WorkflowId:";

    @PersistenceContext
    EntityManager em;

    @EJB
    SettingsServiceBean settings;

    @EJB
    RoleAssigneeServiceBean roleAssignees;

    @EJB
    EjbDataverseEngine engine;
    
    final Map<String, WorkflowStepSPI> providers = new HashMap<>();

    public WorkflowServiceBean() {
        providers.put(":internal", new InternalWorkflowStepSP());

        logger.log(Level.INFO, "Searching for workflow step providers...");
//        ServiceLoader<WorkflowStepSPI> loader = ServiceLoader.load(WorkflowStepSPI.class);
//        try {
//            for ( WorkflowStepSPI wss : loader ) {
//                logger.log(Level.INFO, "Found WorkflowStepProvider: {0}", wss.getClass().getCanonicalName());
//                providers.put( wss.getClass().getCanonicalName(), wss );
//            }
//            logger.log(Level.INFO, "Searching for Workflow Step Providers done.");
//        } catch (NoClassDefFoundError ncdfe) {
//            logger.log(Level.WARNING, "Class not found: " + ncdfe.getMessage(), ncdfe);
//        } catch (ServiceConfigurationError serviceError) {
//            logger.log(Level.WARNING, "Service Error loading workflow step providers: " + serviceError.getMessage(), serviceError);
//        }
        
    }
    
    /**
     * Starts executing workflow {@code wf} under the passed context.
     *
     * @param wf the workflow to execute.
     * @param ctxt the context in which the workflow is executed.
     */
    public void start(Workflow wf, WorkflowContext ctxt) {
        forward(wf, ctxt, 0);
    }

    /**
     * Starting the resume process for a pending workflow. We first delete the
     * pending workflow to minimize double invocation, and then asynchronously
     * resume the work.
     *
     * @param pending The workflow to resume.
     * @param body the response from the remote system.
     * @see
     * #doResume(edu.harvard.iq.dataverse.workflow.PendingWorkflowInvocation,
     * java.lang.String)
     */
    public void resume(PendingWorkflowInvocation pending, String body) {
        em.remove(em.merge(pending));
        doResume(pending, body);
    }
    
    @Asynchronous
    private void forward(Workflow wf, WorkflowContext ctxt, int idx) {
        WorkflowStepData wsd = wf.getSteps().get(idx);
        WorkflowStep step = createStep(wsd);
        WorkflowStepResult res = step.run(ctxt);
        
        if (res == WorkflowStepResult.OK) {
            if (idx == wf.getSteps().size() - 1) {
                workflowCompleted(wf, ctxt);
            } else {
                forward(wf, ctxt, ++idx);
            }

        } else if (res instanceof Failure) {
            logger.log(Level.WARNING, "Workflow {0} failed: {1}", new Object[]{ctxt.getInvocationId(), ((Failure) res).getReason()});
            rollback(wf, ctxt, (Failure) res, idx - 1);

        } else if (res instanceof Pending) {
            pauseAndAwait(wf, ctxt, (Pending) res, idx);
        }
    }
    
    @Asynchronous
    private void doResume(PendingWorkflowInvocation pending, String body) {
        Workflow wf = pending.getWorkflow();
        List<WorkflowStepData> stepsLeft = wf.getSteps().subList(pending.getPendingStepIdx(), wf.getSteps().size());
        WorkflowStep pendingStep = createStep(stepsLeft.get(0));
        final WorkflowContext ctxt = pending.reCreateContext(roleAssignees);

        WorkflowStepResult res = pendingStep.resume(ctxt, pending.getLocalData(), body);
        if (res instanceof Failure) {
            rollback(wf, ctxt, (Failure) res, pending.getPendingStepIdx() - 1);
        } else if (res instanceof Pending) {
            pauseAndAwait(wf, ctxt, (Pending) res, pending.getPendingStepIdx());
        } else {
            forward(wf, ctxt, pending.getPendingStepIdx() + 1);
        }
    }

    @Asynchronous
    private void rollback(Workflow wf, WorkflowContext ctxt, Failure failure, int idx) {
        WorkflowStepData wsd = wf.getSteps().get(idx);
        logger.log(Level.INFO, "{0} rollback of step {1}", new Object[]{ctxt.getInvocationId(), idx});
        try {
            createStep(wsd).rollback(ctxt, failure);
        } finally {
            if (idx > 0) {
                rollback(wf, ctxt, failure, --idx);
            } else {
                unlockDataset(ctxt);
            }
        }
    }
    
    /**
     * Unlocks the dataset after the workflow is over. 
     * @param ctxt 
     */
    @Asynchronous
    private void unlockDataset( WorkflowContext ctxt ) {
        try {
            engine.submit( new RemoveLockCommand(ctxt.getRequest(), ctxt.getDataset()) );
        } catch (CommandException ex) {
            logger.log(Level.SEVERE, "Cannot unlock dataset after rollback: " + ex.getMessage(), ex);
        }
    }
    
    private void pauseAndAwait(Workflow wf, WorkflowContext ctxt, Pending pendingRes, int idx) {
        PendingWorkflowInvocation pending = new PendingWorkflowInvocation(wf, ctxt, pendingRes);
        pending.setPendingStepIdx(idx);
        em.persist(pending);
    }

    @Asynchronous
    private void workflowCompleted(Workflow wf, WorkflowContext ctxt) {
        logger.log(Level.INFO, "Workflow {0} completed.", ctxt.getInvocationId());
        if ( ctxt.getType() == TriggerType.PrePublishDataset ) {
            try {
                engine.submit( new FinalizeDatasetPublicationCommand(ctxt.getDataset(), ctxt.getDoiProvider(), ctxt.getRequest()) );
                unlockDataset(ctxt);
                
            } catch (CommandException ex) {
                logger.log(Level.SEVERE, "Exception finalizing workflow " + ctxt.getInvocationId() +": " + ex.getMessage(), ex);
                rollback(wf, ctxt, new Failure("Exception while finalizing the publication: " + ex.getMessage()), wf.steps.size()-1);
            }
        }
    }

    public List<Workflow> listWorkflows() {
        return em.createNamedQuery("Workflow.listAll").getResultList();
    }

    public Optional<Workflow> getWorkflow(long workflowId) {
        return Optional.ofNullable(em.find(Workflow.class, workflowId));
    }

    public Workflow save(Workflow workflow) {
        if (workflow.getId() == null) {
            em.persist(workflow);
            em.flush();
            return workflow;
        } else {
            return em.merge(workflow);
        }
    }

    /**
     * Deletes the workflow with the passed id (if possible).
     * @param workflowId id of the workflow to be deleted.
     * @return {@code true} iff the workflow was deleted, {@code false} if it was not found.
     * @throws IllegalArgumentException iff the workflow is a default workflow.
     */
    public boolean deleteWorkflow(long workflowId) {
        Optional<Workflow> doomedOpt = getWorkflow(workflowId);
        if (doomedOpt.isPresent()) {
            // validate that this is not the default workflow
            for ( WorkflowContext.TriggerType tp : WorkflowContext.TriggerType.values() ) {
                String defaultWorkflowId = settings.get(workflowSettingKey(tp));
                if (defaultWorkflowId != null
                        && Long.parseLong(defaultWorkflowId) == doomedOpt.get().getId()) {
                    throw new IllegalArgumentException("Workflow " + workflowId + " cannot be deleted as it is the default workflow for trigger " + tp.name() );
                }
            }

            em.remove(doomedOpt.get());
            return true;
        } else {
            return false;
        }
    }

    public List<PendingWorkflowInvocation> listPendingInvocations() {
        return em.createNamedQuery("PendingWorkflowInvocation.listAll")
                .getResultList();
    }

    public PendingWorkflowInvocation getPendingWorkflow(String invocationId) {
        return em.find(PendingWorkflowInvocation.class, invocationId);
    }

    public Optional<Workflow> getDefaultWorkflow( WorkflowContext.TriggerType type ) {
        String defaultWorkflowId = settings.get(workflowSettingKey(type));
        if (defaultWorkflowId == null) {
            return Optional.empty();
        }
        return getWorkflow(Long.parseLong(defaultWorkflowId));
    }

    /**
     * Sets the workflow of the default it.
     *
     * @param id Id of the default workflow, or {@code null}, for disabling the
     * default workflow.
     * @param type type of the workflow.
     */
    public void setDefaultWorkflowId(WorkflowContext.TriggerType type, Long id) {
        String workflowKey = workflowSettingKey(type);
        if (id == null) {
            settings.delete(workflowKey);
        } else {
            settings.set(workflowKey, id.toString());
        }
    }

    private String workflowSettingKey(WorkflowContext.TriggerType type) {
        return WORKFLOW_ID_KEY+type.name();
    }

    private WorkflowStep createStep(WorkflowStepData wsd) {
        WorkflowStepSPI provider = providers.get(wsd.getProviderId());
        if (provider == null) {
            logger.log(Level.SEVERE, "Cannot find a step provider with id ''{0}''", wsd.getProviderId());
            throw new IllegalArgumentException("Bad WorkflowStepSPI id: '" + wsd.getProviderId() + "'");
        }
        return provider.getStep(wsd.getStepType(), wsd.getStepParameters());
    }

}
