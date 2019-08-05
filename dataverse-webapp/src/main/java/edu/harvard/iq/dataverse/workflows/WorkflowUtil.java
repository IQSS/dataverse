package edu.harvard.iq.dataverse.workflows;

import edu.harvard.iq.dataverse.common.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import java.util.List;

import static edu.harvard.iq.dataverse.common.NullSafeJsonBuilder.jsonObjectBuilder;

public class WorkflowUtil {

    public static JsonArrayBuilder getAllWorkflowComments(DatasetVersion datasetVersion) {
        JsonArrayBuilder workflowCommentsAsJson = Json.createArrayBuilder();
        List<WorkflowComment> workflowComments = datasetVersion.getWorkflowComments();
        for (WorkflowComment workflowComment : workflowComments) {
            NullSafeJsonBuilder workflowCommentAsJson = jsonObjectBuilder();
            workflowCommentAsJson.add("workflowCommentId", workflowComment.getId());
            workflowCommentAsJson.add("message", workflowComment.getMessage());
            workflowCommentAsJson.add("createTime", Util.getDateTimeFormat().format(workflowComment.getCreated()));
            workflowCommentAsJson.add("commentBy", workflowComment.getAuthenticatedUser().getIdentifier());
            workflowCommentAsJson.add("datasetId", datasetVersion.getDataset().getId());
            workflowCommentAsJson.add("datasetVersionId", datasetVersion.getId());
            workflowCommentAsJson.add("datasetTitle", datasetVersion.getTitle());
            workflowCommentsAsJson.add(workflowCommentAsJson);
        }
        return workflowCommentsAsJson;
    }
}
