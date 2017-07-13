package edu.harvard.iq.dataverse.workflows;

public class WorkflowAction {

    /**
     * Each action belongs to a workflow, such as the "publication" or "request
     * access" workflow.
     *
     * - The "publication" workflow, which might include the author clicking
     * "Submit for Review" and the curator clicking "Return to Author" or
     * "Publish".
     *
     * - The "request access" workflow, which might include users asking for
     * access to files and curators either granting or denying access.
     * Requesting a contributor role for a dataset is just an idea at this
     * point, included to show how other entities might be in play in the
     * future.
     *
     * It's certainly possible that other workflows may be considered in the
     * future such as "invite collaborator" (
     * https://github.com/IQSS/dataverse/issues/1334 ) and other workflows we
     * haven't even considered.
     *
     * Some workflows might look like a conversation or a tennis match with a
     * half a dozen clicks of "Submit for Review" and "Return to Author" before
     * the dataset version is ultimately published.
     */
    public enum Type {
        RETURN_TO_AUTHOR,
        // These are potential future action types for existing functionality.
        SUBMIT_FOR_REVIEW,
        PUBLISH,
        REQUEST_FILE_ACCESS,
        GRANT_FILE_ACCESS,
        DENY_FILE_ACCESS,
        // This is functionality we don't even have yet.
        REQUEST_DATASET_CONTRIBUTOR_ACCESS,
        GRANT_DATASET_CONTRIBUTOR_ACCESS,
        DATASET_CONTRIBUTOR_ACCESS
    };

    public enum WorkFlow {
        PUBLICATION,
        REQUEST_ACCESS,
    };

    private Object target;
    private Type type;
    private WorkFlow workFlow;

    public WorkflowAction(Object target, Type type) {
        this.target = target;
        this.type = type;
        switch (this.type) {
            case RETURN_TO_AUTHOR:
                this.workFlow = WorkFlow.PUBLICATION;
                break;
            case SUBMIT_FOR_REVIEW:
                this.workFlow = WorkFlow.PUBLICATION;
                break;
            case PUBLISH:
                this.workFlow = WorkFlow.PUBLICATION;
                break;
            case REQUEST_FILE_ACCESS:
                this.workFlow = WorkFlow.REQUEST_ACCESS;
                break;
            case GRANT_FILE_ACCESS:
                this.workFlow = WorkFlow.REQUEST_ACCESS;
                break;
            case DENY_FILE_ACCESS:
                this.workFlow = WorkFlow.REQUEST_ACCESS;
                break;
            default:
                break;
        }
    }

    public Object getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    public WorkFlow getWorkFlow() {
        return workFlow;
    }

}
