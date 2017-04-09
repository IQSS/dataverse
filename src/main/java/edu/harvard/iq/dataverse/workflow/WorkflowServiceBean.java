package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.internalspi.InternalWorkflowStepSP;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Service bean for managing {@link Workflow}s.
 * @author michael
 */
@Stateless
public class WorkflowServiceBean {
        
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
    
    public void setDefaultWorkflowId( Long id ) {
        if ( id == null ) {
            settings.delete(WORKFLOW_ID_KEY);
        } else {
            settings.set(WORKFLOW_ID_KEY, id.toString());
        }
    }
}
