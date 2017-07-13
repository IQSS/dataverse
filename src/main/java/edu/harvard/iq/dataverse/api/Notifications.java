package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.workflows.WorkflowUtil;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

@Path("notifications")
public class Notifications extends AbstractApiBean {

    @GET
    @Path("all")
    public Response getAllNotificationsForUser() {
        User user;
        try {
            user = findUserOrDie();
        } catch (WrappedResponse ex) {
            return error(Response.Status.UNAUTHORIZED, "You must supply an API token.");
        }
        if (user == null) {
            return error(Response.Status.BAD_REQUEST, "A user could not be found based on the API token.");
        }
        if (!(user instanceof AuthenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, "Only an AuthenticatedUser can have notifications.");
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        List<UserNotification> notifications = userNotificationSvc.findByUser(authenticatedUser.getId());
        for (UserNotification notification : notifications) {
            NullSafeJsonBuilder notificationObjectBuilder = jsonObjectBuilder();
            Type type = notification.getType();
            notificationObjectBuilder.add("type", type.toString());
            if (Type.RETURNEDDS.equals(type) || Type.SUBMITTEDDS.equals(type)) {
                // TODO: consider returning all reasons instead, so you can see the history.
                notificationObjectBuilder.add("reasonForReturn", getReasonForReturn(notification));
            }
            jsonArrayBuilder.add(notificationObjectBuilder);
        }
        JsonObjectBuilder result = Json.createObjectBuilder().add("notifications", jsonArrayBuilder);
        return ok(result);
    }

    private String getReasonForReturn(UserNotification notification) {
        Long objectId = notification.getObjectId();
        return WorkflowUtil.getMostRecentWorkflowComment(datasetVersionSvc.find(objectId));
    }

}
