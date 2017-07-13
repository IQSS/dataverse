package edu.harvard.iq.dataverse.workflows;

import edu.harvard.iq.dataverse.DatasetVersion;
import java.util.List;

public class WorkflowUtil {

    public static String getMostRecentWorkflowComment(DatasetVersion datasetVersion) {
        List<WorkflowComment> workflowComments = datasetVersion.getWorkflowComments();
        if (workflowComments != null && !workflowComments.isEmpty()) {
            WorkflowComment workflowComment = workflowComments.get(workflowComments.size() - 1);
            return workflowComment.getMessage();
        }
        return null;
    }

}
