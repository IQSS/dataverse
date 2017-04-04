package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import edu.harvard.iq.dataverse.workflow.Workflow;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * API Endpoint for managing workflows.
 * @author michael
 */
@Path("admin/workflows")
@Stateless
public class Workflows extends AbstractApiBean {
    
    @POST
    public Response addWorkflow(JsonObject jsonWorkflow) {
        JsonParser jp = new JsonParser();
        try {
            Workflow wf = jp.parseWorkflow(jsonWorkflow);
            return ok(json(wf));
        } catch (JsonParseException ex) {
            return badRequest("Can't parse Json: " + ex.getMessage());
        }
    }
}
