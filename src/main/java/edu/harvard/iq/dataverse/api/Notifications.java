package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.workflows.WorkflowUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

@Stateless
@Path("notifications")
public class Notifications extends AbstractApiBean {

    @EJB
    MailServiceBean mailService;
    
    @GET
    @AuthRequired
    @Path("/all")
    public Response getAllNotificationsForUser(@Context ContainerRequestContext crc) {
        User user = getRequestUser(crc);
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
            notificationObjectBuilder.add("sentTimestamp", notification.getSendDateTimestamp());
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
    @AuthRequired
    @Path("/{id}")
    public Response deleteNotificationForUser(@Context ContainerRequestContext crc, @PathParam("id") long id) {
        User user = getRequestUser(crc);
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
    @AuthRequired
    @Path("/mutedEmails")
    public Response getMutedEmailsForUser(@Context ContainerRequestContext crc) {
        User user = getRequestUser(crc);
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
    @AuthRequired
    @Path("/mutedEmails/{typeName}")
    public Response muteEmailsForUser(@Context ContainerRequestContext crc, @PathParam("typeName") String typeName) {
        User user = getRequestUser(crc);
        if (!(user instanceof AuthenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, "Only an AuthenticatedUser can have notifications.");
        }

        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        Set<UserNotification.Type> mutedEmails = authenticatedUser.getMutedEmails();
        mutedEmails.add(notificationType);
        authenticatedUser.setMutedEmails(mutedEmails);
        authSvc.update(authenticatedUser);
        return ok("Notification emails of type " + typeName + " muted.");
    }

    @DELETE
    @AuthRequired
    @Path("/mutedEmails/{typeName}")
    public Response unmuteEmailsForUser(@Context ContainerRequestContext crc, @PathParam("typeName") String typeName) {
        User user = getRequestUser(crc);
        if (!(user instanceof AuthenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, "Only an AuthenticatedUser can have notifications.");
        }

        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        Set<UserNotification.Type> mutedEmails = authenticatedUser.getMutedEmails();
        mutedEmails.remove(notificationType);
        authenticatedUser.setMutedEmails(mutedEmails);
        authSvc.update(authenticatedUser);
        return ok("Notification emails of type " + typeName + " unmuted.");
    }

    @GET
    @AuthRequired
    @Path("/mutedNotifications")
    public Response getMutedNotificationsForUser(@Context ContainerRequestContext crc) {
        User user = getRequestUser(crc);
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
    @AuthRequired
    @Path("/mutedNotifications/{typeName}")
    public Response muteNotificationsForUser(@Context ContainerRequestContext crc, @PathParam("typeName") String typeName) {
        User user = getRequestUser(crc);
        if (!(user instanceof AuthenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, "Only an AuthenticatedUser can have notifications.");
        }

        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        Set<UserNotification.Type> mutedNotifications = authenticatedUser.getMutedNotifications();
        mutedNotifications.add(notificationType);
        authenticatedUser.setMutedNotifications(mutedNotifications);
        authSvc.update(authenticatedUser);
        return ok("Notification of type " + typeName + " muted.");
    }

    @DELETE
    @AuthRequired
    @Path("/mutedNotifications/{typeName}")
    public Response unmuteNotificationsForUser(@Context ContainerRequestContext crc, @PathParam("typeName") String typeName) {
        User user = getRequestUser(crc);
        if (!(user instanceof AuthenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, "Only an AuthenticatedUser can have notifications.");
        }

        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
        Set<UserNotification.Type> mutedNotifications = authenticatedUser.getMutedNotifications();
        mutedNotifications.remove(notificationType);
        authenticatedUser.setMutedNotifications(mutedNotifications);
        authSvc.update(authenticatedUser);
        return ok("Notification of type " + typeName + " unmuted.");
    }
}
