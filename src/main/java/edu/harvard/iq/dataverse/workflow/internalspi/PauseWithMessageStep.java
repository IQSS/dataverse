package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * A sample step that pauses the workflow.
 * 
 * @author michael
 */
public class PauseWithMessageStep implements WorkflowStep {
    
    private static final Logger logger = Logger.getLogger(PauseWithMessageStep.class.getName());
    
    /** Constant used by testing to simulate a failed step. */
    public static final String FAILURE_RESPONSE="fail";
    
    private final Map<String,String> params = new HashMap<>();

    public PauseWithMessageStep( Map<String,String> paramSet ) {
        params.putAll(paramSet);
    }
    
    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        final Pending result = new Pending();
        result.getData().putAll(params);
        return result;
    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        try (StringReader reader = new StringReader(externalData)) {
            JsonObject response = Json.createReader(reader).readObject();
            String status = response.getString("Status");
            String reason = null;
            String message = null;
            if (response.containsKey("Reason")) {
                reason = response.getString("Reason");
            }
            if (response.containsKey("Message")) {
                message = response.getString("Message");
            }
            switch (status) {
            case "Success":
                logger.log(Level.FINE, "AuthExt Worfklow Step Succeeded: " + reason);
                return new Success(reason, message);
            case "Failure":
                logger.log(Level.WARNING, "Remote system indicates workflow failed: {0}", reason);
                return new Failure(reason, message);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Remote system returned a bad reposonse: {0}", externalData);
        }
        return new Failure("Workflow failure: Response from remote server could not be parsed:" + externalData, null);
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        // nothing to roll back
    }
    
}
