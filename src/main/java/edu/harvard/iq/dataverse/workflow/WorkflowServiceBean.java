package edu.harvard.iq.dataverse.workflow;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Service bean for managing and executing {@link Workflow}s
 * @author michael
 */
@Stateless
public class WorkflowServiceBean {
    
    private static final Logger logger = Logger.getLogger(WorkflowServiceBean.class.getName());
    private static final String WORKFLOW_ID_KEY="WorkflowServiceBean.defaultWorkflowId";
    
    @PersistenceContext
    EntityManager em;
    
    @EJB
    SettingsServiceBean settings;
    
    @EJB
    RoleAssigneeServiceBean roleAssignees;
    
    final Map<String, WorkflowStepSPI> providers = new HashMap<>();
    
    public WorkflowServiceBean() {
        providers.put(":internal", new InternalWorkflowStepSP() );
        
        // TODO scan SPIs, load.
    }
    
    /**
     * Starts executing workflow {@code wf} under the passed context.
     * 
     * @param wf the workflow to execute.
     * @param ctxt the context in which the workflow is executed.
     */
    public void start( Workflow wf, WorkflowContext ctxt ) {
        forward(wf, ctxt, 0);
    }
    
    /**
     * Starting the resume process for a pending workflow. We first delete the pending
     * workflow to minimize double invocation, and then asynchronously resume the work.
     * @param pending The workflow to resume.
     * @param body the response from the remote system.
     * @see #doResume(edu.harvard.iq.dataverse.workflow.PendingWorkflowInvocation, java.lang.String) 
     */
    public void resume(PendingWorkflowInvocation pending, String body) {
        em.remove(pending);
        doResume(pending, body);
    }

    @Asynchronous
    private void doResume(PendingWorkflowInvocation pending, String body) {
        Workflow wf = pending.getWorkflow();
        List<WorkflowStepData> stepsLeft = wf.getSteps().subList(pending.getPendingStepIdx(), wf.getSteps().size());
        WorkflowStep pendingStep = createStep(stepsLeft.get(0));
        final WorkflowContext ctxt = pending.reCreateContext(roleAssignees);
        
        WorkflowStepResult res = pendingStep.resume(ctxt, pending.getLocalData(), body);
        if ( res instanceof Failure ) {
            rollback(wf, ctxt, (Failure)res, pending.getPendingStepIdx()-1);
        } else if ( res instanceof Pending ) {
            pauseAndAwait(wf, ctxt, (Pending)res, pending.getPendingStepIdx());
        }
        forward( wf, ctxt, pending.getPendingStepIdx()+1 );
    }
    
    @Asynchronous
    private void forward( Workflow wf, WorkflowContext ctxt, int idx ) {
        WorkflowStepData wsd = wf.getSteps().get(idx);
        WorkflowStep step = createStep(wsd);
        WorkflowStepResult res = step.run(ctxt);
            if ( res != WorkflowStepResult.OK ) {
                if ( res == WorkflowStepResult.OK ) {
                    if ( idx == wf.getSteps().size()-1 ) {
                        workflowCompleted(wf, ctxt);
                    } else {
                        forward(wf, ctxt, ++idx);
                    }
                    
                } else if ( res instanceof Failure ) {
                    logger.log(Level.WARNING, "Workflow {0} failed: {1}", new Object[]{ctxt.getInvocationId(), ((Failure)res).getReason()});
                    rollback(wf, ctxt, (Failure)res, idx-1);
                    
                } else if ( res instanceof Pending ) {
                    pauseAndAwait(wf, ctxt, (Pending)res, idx);
                }
            }
    }
    
    @Asynchronous
    private void rollback(Workflow wf, WorkflowContext ctxt, Failure failure, int idx) {
        WorkflowStepData wsd = wf.getSteps().get(idx);
        logger.log(Level.INFO, "{0} rollback done", ctxt.getInvocationId());
        createStep(wsd).rollback(ctxt, failure);
        if ( idx > 0 ) {
            rollback(wf, ctxt, failure, --idx);
        } 
    }

    private void pauseAndAwait(Workflow wf, WorkflowContext ctxt, Pending pendingRes, int idx) {
        PendingWorkflowInvocation pending = new PendingWorkflowInvocation(wf, ctxt, pendingRes);
        pending.setPendingStepIdx(idx);
        em.persist(pending);
    }
    
    
    @Asynchronous
    private void workflowCompleted(Workflow wf, WorkflowContext ctxt) {
        // TODO SBGrid: if this is a "pre-release" workflow, execute the finalize publication command.
    }
    
    public List<Workflow> listWorkflows() {
        return em.createNamedQuery("Workflow.listAll").getResultList();
    }
    
    public Optional<Workflow> getWorkflow(long workflowId) {
        return Optional.ofNullable(em.find(Workflow.class, workflowId));
    }
    
    public Workflow save( Workflow workflow ) {
        if ( workflow.getId()==null ) {
            em.persist(workflow);
            em.flush();
            return workflow;
        } else {
            return em.merge(workflow);
        }
    }
    
    public int deleteWorkflow( long workflowId ) {
        Optional<Workflow> doomedOpt = getWorkflow(workflowId);
        if ( doomedOpt.isPresent() ) {
            // validate that this is not the default workflow
            String defaultWorkflowId = settings.get(WORKFLOW_ID_KEY);
            if ( defaultWorkflowId != null &&
                    Long.parseLong(defaultWorkflowId)==doomedOpt.get().getId() ) {
                throw new IllegalArgumentException("Cannot delete the deafult workflow id");
            }
            
            em.remove(doomedOpt.get());
            return 1;
        } else {
            return 0;
        }
    }
    
    public List<PendingWorkflowInvocation> listPendingInvocations() {
        return em.createNamedQuery("PendingWorkflowInvocation.listAll")
                 .getResultList();
    }
    
    public PendingWorkflowInvocation getPendingWorkflow( String invocationId ) {
        return em.find(PendingWorkflowInvocation.class, invocationId );
    }
    
    public Optional<Workflow> getDefaultWorkflow() {
        String defaultWorkflowId = settings.get(WORKFLOW_ID_KEY);
        if ( defaultWorkflowId==null ) return Optional.empty();
        return getWorkflow(Long.parseLong(defaultWorkflowId));
    }
    
    /**
     * Sets the workflow of the default it.
     * @param id Id of the default workflow, or {@code null}, for disabling the default workflow.
     */
    public void setDefaultWorkflowId( Long id ) {
        if ( id == null ) {
            settings.delete(WORKFLOW_ID_KEY);
        } else {
            settings.set(WORKFLOW_ID_KEY, id.toString());
        }
    }

    private WorkflowStep createStep(WorkflowStepData wsd) {
        WorkflowStepSPI provider = providers.get(wsd.getProviderId());
        if ( provider == null ) {
            logger.log(Level.SEVERE, "Cannot find a step provider with id ''{0}''", wsd.getProviderId());
            throw new IllegalArgumentException( "Bad WorkflowStepSPI id: '" + wsd.getProviderId() + "'");
        }
        return provider.getStep(wsd.getStepType(), wsd.getStepParameters());
    }

}
