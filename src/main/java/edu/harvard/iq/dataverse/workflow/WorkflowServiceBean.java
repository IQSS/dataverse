package edu.harvard.iq.dataverse.workflow;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.Dataset;
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
     * @return {@code Boolean.TRUE} if the workflow completed successfully. {@code Boolean.FALSE} if it failed. {@code null} if the workflow is waiting for external system.
     */
    public Boolean start( Workflow wf, WorkflowContext ctxt ) {
        
        logger.log(Level.INFO, "Startng workflow {0} over dataset {1} ({2})", new Object[]{wf.getId(), ctxt.getDataset().getIdentifier(), ctxt.getInvocationId()});
        int idx=0;
        for ( WorkflowStepData wsd : wf.getSteps() ) {
            WorkflowStep curStep = createStep(wsd);
            WorkflowStepResult res = curStep.run(ctxt);
            if ( res != WorkflowStepResult.OK ) {
                if ( res instanceof Failure ) {
                    logger.log(Level.WARNING, "Workflow {0} failed: {1}", new Object[]{ctxt.getInvocationId(), ((Failure)res).getReason()});
                    rollback(wf, ctxt, (Failure)res, idx);
                    return Boolean.FALSE;
                } else if ( res instanceof Pending ) {
                    pauseAndAwait(wf, ctxt, (Pending)res, idx);
                    return null;
                }
            }
            idx++;
        }
        return Boolean.TRUE;
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

    private void rollback(Workflow wf, WorkflowContext ctxt, Failure failure, int idx) {
        List<WorkflowStepData> toRollBack = wf.getSteps().subList(0, idx+1);
        Collections.reverse(toRollBack);
        toRollBack.forEach((WorkflowStepData wsd) -> {
            logger.log(Level.INFO, "{0} rolling back step {1}", new Object[]{ctxt.getInvocationId(), wsd.toString()});
            createStep(wsd).rollback(ctxt, failure);
        });
        logger.log(Level.INFO, "{0} rollback done", ctxt.getInvocationId());
    }

    private void pauseAndAwait(Workflow wf, WorkflowContext ctxt, Pending pendingRes, int idx) {
        PendingWorkflowInvocation pending = new PendingWorkflowInvocation(wf, ctxt, pendingRes);
        em.persist(pending);
    }
}
