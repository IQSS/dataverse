package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.Optional;
import java.util.Set;

import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

@Stateless
@Path("notifications")
public class Notifications extends AbstractApiBean {

    @GET
    @AuthRequired
    @Path("/all")
    public Response getAllNotificationsForUser(@Context ContainerRequestContext crc, @QueryParam("inAppNotificationFormat") boolean inAppNotificationFormat) {
        User user = getRequestUser(crc);
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
        }
        return ok(Json.createObjectBuilder().add("notifications", json(userNotificationSvc.findByUser(authenticatedUser.getId()), inAppNotificationFormat)));
    }

    @GET
    @AuthRequired
    @Path("/unreadCount")
    public Response getUnreadNotificationsCountForUser(@Context ContainerRequestContext crc) {
        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
            long unreadCount = userNotificationSvc.getUnreadNotificationCountByUser(au.getId());
            return ok(Json.createObjectBuilder()
                    .add("unreadCount", unreadCount));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("/{id}/markAsRead")
    public Response markNotificationAsReadForUser(@Context ContainerRequestContext crc, @PathParam("id") long id) {
        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
            Long userId = au.getId();
            Optional<UserNotification> notification = userNotificationSvc.findByUser(userId).stream().filter(x -> x.getId().equals(id)).findFirst();
            if (notification.isPresent()) {
                UserNotification saved = userNotificationSvc.markAsRead(notification.get());
                if (saved.isReadNotification()) {
                    return ok("Notification " + id + " marked as read.");
                } else {
                    return badRequest("Notification " + id + " could not be marked as read.");
                }
            } else {
                return notFound("Notification " + id + " not found.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("/{id}")
    public Response deleteNotificationForUser(@Context ContainerRequestContext crc, @PathParam("id") long id) {
        User user = getRequestUser(crc);
        if (!(user instanceof AuthenticatedUser)) {
            // It's unlikely we'll reach this error. A Guest doesn't have an API token and would have been blocked above.
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("notifications.errors.unauthenticatedUser"));
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
