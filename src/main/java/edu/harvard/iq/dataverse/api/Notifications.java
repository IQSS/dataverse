package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
            JsonObjectBuilder notificationObjectBuilder = Json.createObjectBuilder();
            Type type = notification.getType();
            notificationObjectBuilder.add("type", type.toString());
            if (Type.RETURNEDDS.equals(type)) {
                Long objectId = notification.getObjectId();
                DatasetVersion datasetVersion = datasetVersionSvc.find(objectId);
                if (datasetVersion != null) {
                    try {
                        String message = datasetVersion.getWorkflowComments().get(0).getMessage();
                        if (message != null) {
                            notificationObjectBuilder.add("reasonForReturn", message);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (Type.SUBMITTEDDS.equals(type)) {
                try {
                    Long objectId = notification.getObjectId();
                    DatasetVersion datasetVersion = datasetVersionSvc.find(objectId);
                    if (datasetVersion != null) {
                        String message = datasetVersion.getWorkflowComments().get(0).getMessage();
                        if (message != null) {
                            notificationObjectBuilder.add("reasonForReturn", message);
                        }
                    }
                } catch (Exception e) {
                }
            }
            jsonArrayBuilder.add(notificationObjectBuilder);
        }
        JsonObjectBuilder result = Json.createObjectBuilder().add("notifications", jsonArrayBuilder);
        return ok(result);
    }

}
