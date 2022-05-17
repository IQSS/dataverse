package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.workflows.WorkflowUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

@Stateless
@Path("notifications")
public class Notifications extends AbstractApiBean {

    @EJB
    MailServiceBean mailService;
    
    @GET
    @Path("/all")
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
            JsonArrayBuilder reasonsForReturn = Json.createArrayBuilder();
            Type type = notification.getType();
            notificationObjectBuilder.add("id", notification.getId());
            notificationObjectBuilder.add("type", type.toString());
            /* FIXME - Re-add reasons for return if/when they are added to the notifications page.
            
            if (Type.RETURNEDDS.equals(type) || Type.SUBMITTEDDS.equals(type)) {
                JsonArrayBuilder reasons = getReasonsForReturn(notification);
                for (JsonValue reason : reasons.build()) {
                    reasonsForReturn.add(reason);
                }
                notificationObjectBuilder.add("reasonsForReturn", reasonsForReturn);
            }
            */
            Object objectOfNotification =  mailService.getObjectOfNotification(notification);
            if (objectOfNotification != null){
                String subjectText = MailUtil.getSubjectTextBasedOnNotification(notification, objectOfNotification);
                String messageText = mailService.getMessageTextBasedOnNotification(notification, objectOfNotification, null, notification.getRequestor());
                notificationObjectBuilder.add("subjectText", subjectText);
                notificationObjectBuilder.add("messageText", messageText);
            }
            jsonArrayBuilder.add(notificationObjectBuilder);
        }
        JsonObjectBuilder result = Json.createObjectBuilder().add("notifications", jsonArrayBuilder);
        return ok(result);
    }

    private JsonArrayBuilder getReasonsForReturn(UserNotification notification) {
        Long objectId = notification.getObjectId();
        return WorkflowUtil.getAllWorkflowComments(datasetVersionSvc.find(objectId));
    }

    @DELETE
    @Path("/{id}")
    public Response deleteNotificationForUser(@PathParam("id") long id) {
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
        Long userId = authenticatedUser.getId();
        Optional<UserNotification> notification = userNotificationSvc.findByUser(userId).stream().filter(x -> x.getId().equals(id)).findFirst();

        if (notification.isPresent()) {
            userNotificationSvc.delete(notification.get());
            return ok("Notification " + id + " deleted.");
        }

        return notFound("Notification " + id + " not found.");
    }

    @GET
    @Path("/mutedEmails")
    public Response getMutedEmailsForUser() {
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
        JsonArrayBuilder mutedEmails = Json.createArrayBuilder();
        authenticatedUser.getMutedEmails().stream().forEach(
            x -> mutedEmails.add(jsonObjectBuilder().add("name", x.name()).add("description", x.getDescription()))
        );
        JsonObjectBuilder result = Json.createObjectBuilder().add("mutedEmails", mutedEmails);
        return ok(result);
    }

    @PUT
    @Path("/mutedEmails/{typeName}")
    public Response muteEmailsForUser(@PathParam("typeName") String typeName) {
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

        UserNotification.Type notificationType = UserNotification.Type.valueOf(typeName);
        if (notificationType != null) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            Set<UserNotification.Type> mutedEmails = authenticatedUser.getMutedEmails();
            mutedEmails.add(notificationType);
            authenticatedUser.setMutedEmails(mutedEmails);
            authSvc.update(authenticatedUser);
            return ok("Notification emails of type " + typeName + " muted.");
        }
        
        return notFound("Notification type " + typeName + " not found.");
    }

    @DELETE
    @Path("/mutedEmails/{typeName}")
    public Response unmuteEmailsForUser(@PathParam("typeName") String typeName) {
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

        UserNotification.Type notificationType = UserNotification.Type.valueOf(typeName);
        if (notificationType != null) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            Set<UserNotification.Type> mutedEmails = authenticatedUser.getMutedEmails();
            mutedEmails.remove(notificationType);
            authenticatedUser.setMutedEmails(mutedEmails);
            authSvc.update(authenticatedUser);
            return ok("Notification emails of type " + typeName + " unmuted.");
        }
        
        return notFound("Notification type " + typeName + " not found.");
    }

    @GET
    @Path("/mutedNotifications")
    public Response getMutedNotificationsForUser() {
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
        JsonArrayBuilder mutedNotifications = Json.createArrayBuilder();
        authenticatedUser.getMutedNotifications().stream().forEach(
            x -> mutedNotifications.add(jsonObjectBuilder().add("name", x.name()).add("description", x.getDescription()))
        );
        JsonObjectBuilder result = Json.createObjectBuilder().add("mutedNotifications", mutedNotifications);
        return ok(result);
    }

    @PUT
    @Path("/mutedNotifications/{typeName}")
    public Response muteNotificationsForUser(@PathParam("typeName") String typeName) {
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

        UserNotification.Type notificationType = UserNotification.Type.valueOf(typeName);
        if (notificationType != null) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            Set<UserNotification.Type> mutedNotifications = authenticatedUser.getMutedNotifications();
            mutedNotifications.add(notificationType);
            authenticatedUser.setMutedNotifications(mutedNotifications);
            authSvc.update(authenticatedUser);
            return ok("Notification of type " + typeName + " muted.");
        }
        
        return notFound("Notification type " + typeName + " not found.");
    }

    @DELETE
    @Path("/mutedNotifications/{typeName}")
    public Response unmuteNotificationsForUser(@PathParam("typeName") String typeName) {
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

        UserNotification.Type notificationType = UserNotification.Type.valueOf(typeName);
        if (notificationType != null) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            Set<UserNotification.Type> mutedNotifications = authenticatedUser.getMutedNotifications();
            mutedNotifications.remove(notificationType);
            authenticatedUser.setMutedNotifications(mutedNotifications);
            authSvc.update(authenticatedUser);
            return ok("Notification of type " + typeName + " unmuted.");
        }
        
        return notFound("Notification type " + typeName + " not found.");
    }
}
