package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Service bean for managing {@link Workflow}s
 *
 * @author michael
 * @author kaczynskid
 */
@Singleton
public class WorkflowServiceBean {

    private static final String WORKFLOW_ID_KEY = "WorkflowServiceBean.WorkflowId:";

    @Inject
    SettingsServiceBean settings;

    @Inject
    WorkflowRepository workflows;

    // -------------------- LOGIC --------------------

    public List<Workflow> listWorkflows() {
        return workflows.findAll();
    }

    public Optional<Workflow> getWorkflow(long workflowId) {
        return workflows.findById(workflowId);
    }

    public Workflow save(Workflow workflow) {
        return workflows.save(workflow);
    }

    /**
     * Deletes the workflow with the passed id (if possible).
     *
     * @param workflowId id of the workflow to be deleted.
     * @return {@code true} iff the workflow was deleted, {@code false} if it was not found.
     * @throws IllegalArgumentException iff the workflow is a default workflow.
     */
    public boolean deleteWorkflow(long workflowId) {
        Optional<Workflow> doomedOpt = getWorkflow(workflowId);
        if (doomedOpt.isPresent()) {
            // validate that this is not the default workflow
            for (TriggerType tp : TriggerType.values()) {
                String defaultWorkflowId = settings.get(workflowSettingKey(tp));
                if (StringUtils.isNotEmpty(defaultWorkflowId)
                        && Long.parseLong(defaultWorkflowId) == doomedOpt.get().getId()) {
                    throw new IllegalArgumentException("Workflow " + workflowId + " cannot be deleted as it is the default workflow for trigger " + tp.name());
                }
            }

            workflows.delete(doomedOpt.get());
            return true;
        } else {
            return false;
        }
    }

    public Optional<Workflow> getDefaultWorkflow(TriggerType type) {
        String defaultWorkflowId = settings.get(workflowSettingKey(type));
        if (StringUtils.isEmpty(defaultWorkflowId)) {
            return Optional.empty();
        }
        return getWorkflow(Long.parseLong(defaultWorkflowId));
    }

    /**
     * Sets the workflow of the default it.
     *
     * @param id   Id of the default workflow, or {@code null}, for disabling the
     *             default workflow.
     * @param type type of the workflow.
     */
    public void setDefaultWorkflowId(TriggerType type, Long id) {
        String workflowKey = workflowSettingKey(type);
        if (id == null) {
            settings.delete(workflowKey);
        } else {
            settings.set(workflowKey, id.toString());
        }
    }

    // -------------------- PRIVATE --------------------

    private String workflowSettingKey(TriggerType type) {
        return WORKFLOW_ID_KEY + type.name();
    }
}
