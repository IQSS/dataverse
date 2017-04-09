package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.workflow.internalspi.InternalWorkflowStepSP;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author michael
 */
@Stateless
public class WorkflowServiceBean {
    
    @PersistenceContext
    EntityManager em;
    
    final Map<String, WorkflowStepSPI> providers = new HashMap<>();
    
    public WorkflowServiceBean() {
        providers.put(":internal", new InternalWorkflowStepSP() );
        
        // TODO (next phase) scan SPIs, load.
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
        // TODO check that the workflow is not the default WF.
        Optional<Workflow> doomedOpt = getWorkflow(workflowId);
        if ( doomedOpt.isPresent() ) {
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
    
}
