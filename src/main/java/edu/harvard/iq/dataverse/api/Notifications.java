package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

import java.util.List;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Stateless
@Path("notifications")
@Tag(name = "Notifications", description = "User notification lookup and notification preference operations.")
public class Notifications extends AbstractApiBean {

    @GET
    @AuthRequired
    @Path("/all")
    @Operation(summary = "Lists user notifications",
            description = "Returns notifications for the authenticated user with optional unread filtering, formatting, and pagination.")
    public Response getAllNotificationsForUser(@Context ContainerRequestContext crc,
                                               @Parameter(description = "Limit results to unread notifications.")
                                               @QueryParam("onlyUnread") boolean onlyUnread,
                                               @Parameter(description = "Format notifications for in-app display.")
                                               @QueryParam("inAppNotificationFormat") boolean inAppNotificationFormat,
                                               @Parameter(description = "Maximum number of notifications to return.")
                                               @QueryParam("limit") Integer limit,
                                               @Parameter(description = "Number of notifications to skip before returning results.")
                                               @QueryParam("offset") Integer offset) {
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            List<UserNotification> userNotifications = userNotificationSvc.findByUser(authenticatedUser.getId(), onlyUnread, limit, offset);
            long userNotificationTotalCount = userNotificationSvc.findTotalCountByUser(authenticatedUser.getId(), onlyUnread);
            return ok(json(userNotifications, authenticatedUser, inAppNotificationFormat), userNotificationTotalCount);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("/unreadCount")
    @Operation(summary = "Returns unread notification count",
            description = "Returns the number of unread notifications for the authenticated user.")
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
    @Operation(summary = "Marks a notification as read",
            description = "Marks one notification as read when it belongs to the authenticated user.")
    public Response markNotificationAsReadForUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Notification id to mark as read.", required = true)
            @PathParam("id") long id) {
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
    @Operation(summary = "Deletes a notification",
            description = "Deletes one notification when it belongs to the authenticated user.")
    public Response deleteNotificationForUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Notification id to delete.", required = true)
            @PathParam("id") long id) {
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            Long userId = authenticatedUser.getId();
            Optional<UserNotification> notification = userNotificationSvc.findByUser(userId).stream().filter(x -> x.getId().equals(id)).findFirst();

            if (notification.isPresent()) {
                userNotificationSvc.delete(notification.get());
                return ok("Notification " + id + " deleted.");
            }

            return notFound("Notification " + id + " not found.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("/mutedEmails")
    @Operation(summary = "Lists muted notification emails",
            description = "Returns notification types whose emails are muted for the authenticated user.")
    public Response getMutedEmailsForUser(@Context ContainerRequestContext crc) {
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            JsonArrayBuilder mutedEmails = Json.createArrayBuilder();
            authenticatedUser.getMutedEmails().stream().forEach(
                    x -> mutedEmails.add(jsonObjectBuilder().add("name", x.name()).add("description", x.getDescription()))
            );
            JsonObjectBuilder result = Json.createObjectBuilder().add("mutedEmails", mutedEmails);
            return ok(result);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("/mutedEmails/{typeName}")
    @Operation(summary = "Mutes notification emails",
            description = "Adds the specified notification type to the authenticated user's muted email preferences.")
    public Response muteEmailsForUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Notification type name whose emails are muted.", required = true)
            @PathParam("typeName") String typeName) {
        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            Set<UserNotification.Type> mutedEmails = authenticatedUser.getMutedEmails();
            mutedEmails.add(notificationType);
            authenticatedUser.setMutedEmails(mutedEmails);
            authSvc.update(authenticatedUser);
            return ok("Notification emails of type " + typeName + " muted.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("/mutedEmails/{typeName}")
    @Operation(summary = "Unmutes notification emails",
            description = "Removes the specified notification type from the authenticated user's muted email preferences.")
    public Response unmuteEmailsForUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Notification type name whose emails are unmuted.", required = true)
            @PathParam("typeName") String typeName) {
        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            Set<UserNotification.Type> mutedEmails = authenticatedUser.getMutedEmails();
            mutedEmails.remove(notificationType);
            authenticatedUser.setMutedEmails(mutedEmails);
            authSvc.update(authenticatedUser);
            return ok("Notification emails of type " + typeName + " unmuted.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("/mutedNotifications")
    @Operation(summary = "Lists muted in-app notifications",
            description = "Returns notification types whose in-app notifications are muted for the authenticated user.")
    public Response getMutedNotificationsForUser(@Context ContainerRequestContext crc) {
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            JsonArrayBuilder mutedNotifications = Json.createArrayBuilder();
            authenticatedUser.getMutedNotifications().stream().forEach(
                    x -> mutedNotifications.add(jsonObjectBuilder().add("name", x.name()).add("description", x.getDescription()))
            );
            JsonObjectBuilder result = Json.createObjectBuilder().add("mutedNotifications", mutedNotifications);
            return ok(result);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("/mutedNotifications/{typeName}")
    @Operation(summary = "Mutes in-app notifications",
            description = "Adds the specified notification type to the authenticated user's muted in-app notification preferences.")
    public Response muteNotificationsForUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Notification type name whose in-app notifications are muted.", required = true)
            @PathParam("typeName") String typeName) {
        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            Set<UserNotification.Type> mutedNotifications = authenticatedUser.getMutedNotifications();
            mutedNotifications.add(notificationType);
            authenticatedUser.setMutedNotifications(mutedNotifications);
            authSvc.update(authenticatedUser);
            return ok("Notification of type " + typeName + " muted.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("/mutedNotifications/{typeName}")
    @Operation(summary = "Unmutes in-app notifications",
            description = "Removes the specified notification type from the authenticated user's muted in-app notification preferences.")
    public Response unmuteNotificationsForUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Notification type name whose in-app notifications are unmuted.", required = true)
            @PathParam("typeName") String typeName) {
        UserNotification.Type notificationType;
        try {
            notificationType = UserNotification.Type.valueOf(typeName);
        } catch (Exception ignore) {
            return notFound("Notification type " + typeName + " not found.");
        }
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            Set<UserNotification.Type> mutedNotifications = authenticatedUser.getMutedNotifications();
            mutedNotifications.remove(notificationType);
            authenticatedUser.setMutedNotifications(mutedNotifications);
            authSvc.update(authenticatedUser);
            return ok("Notification of type " + typeName + " unmuted.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
}
