package edu.harvard.iq.dataverse.workflows;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.api.Util;

import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public class WorkflowUtil {

    private static final Logger logger = Logger.getLogger(WorkflowUtil.class.getName());
    
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

    public static WorkflowStepResult parseResponse(String externalData) {
        try (StringReader reader = new StringReader(externalData)) {
            JsonObject response = Json.createReader(reader).readObject();
            String status = null;
            //Lower case is documented, upper case is deprecated
            if(response.containsKey("status")) {
                status= response.getString("status");
            }else if(response.containsKey("Status")) {
                status= response.getString("Status");
            }
            String reason = null;
            String message = null;
            if (response.containsKey("reason")) {
                reason = response.getString("reason");
            }else if (response.containsKey("Reason")) {
                reason = response.getString("Reason");
            }
            if (response.containsKey("message")) {
                message = response.getString("message");
            }else if (response.containsKey("Message")) {
                message = response.getString("Message");
            }
            switch (status) {
            case "success":
            case "Success":
                logger.log(Level.FINE, "AuthExt Worfklow Step Succeeded: " + reason);
                return new Success(reason, message);
            case "failure":
            case "Failure":
                logger.log(Level.WARNING, "Remote system indicates workflow failed: {0}", reason);
                return new Failure(reason, message);
            default:
                logger.log(Level.WARNING, "Remote system returned a response with no \"status\" key or bad status value: {0}", escapeHtml4(externalData));
                return new Failure("Workflow failure: Response from remote server doesn't have valid \"status\":" + escapeHtml4(externalData), null);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Remote system returned a bad response: {0}", externalData);
        }
        //In general, the remote workflow service creating the response is trusted, but, if it's causing an error, escape the result to avoid issues in the UI
        return new Failure("Workflow failure: Response from remote server could not be parsed:" + escapeHtml4(externalData), null);

    }
    
    
}
