package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import static edu.harvard.iq.dataverse.UserNotification.Type.ASSIGNROLE;
import static edu.harvard.iq.dataverse.UserNotification.Type.CREATEACC;
import static edu.harvard.iq.dataverse.UserNotification.Type.GRANTFILEACCESS;
import static edu.harvard.iq.dataverse.UserNotification.Type.REJECTFILEACCESS;
import static edu.harvard.iq.dataverse.UserNotification.Type.REQUESTEDFILEACCESS;
import static edu.harvard.iq.dataverse.UserNotification.Type.REQUESTFILEACCESS;
import static edu.harvard.iq.dataverse.UserNotification.Type.SUBMITTEDDS;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
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

    private static final String OBJECT_ID = "objectId";
    private static final String INSTALLATION_BRAND_NAME = "installationBrandName";
    private static final String DATASET_TITLE = "datasetTitle";
    private static final String DATASET_RELATIVE_URL_TO_ROOT_WITH_SPA = "datasetRelativeUrlToRootWithSpa";
    private static final String MANAGE_FILE_PERMISSIONS_RELATIVE_URL_TO_ROOT_WITH_SPA = "manageFilePermissionsRelativeUrlToRootWithSpa";
    private static final String PARENT_COLLECTION_NAME = "parentCollectionName";
    private static final String PARENT_COLLECTION_URL = "parentCollectionRelativeUrlToRootWithSpa";
    private static final String REQUESTOR_FIRST_NAME = "requestorFirstName";
    private static final String REQUESTOR_LAST_NAME = "requestorLastName";
    private static final String REQUESTOR_EMAIL = "requestorEmail";
    private static final String USER_GUIDE_URL = "userGuideUrl";
    private static final String USER_GUIDE_TABULAR_INGEST_URL = "userGuideTabularIngestUrl";

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
            notificationObjectBuilder.add("displayAsRead", notification.isReadNotification());
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
            Long objectId = notification.getObjectId();
            AuthenticatedUser requestor = notification.getRequestor();
            // Add extra fields so the SPA (and other clients) can generate in-app notifications.
            // These are organized roughly in order (create account first) and then by how often they're used.
            switch (type) {
                case CREATEACC -> {
                    notificationObjectBuilder.add(INSTALLATION_BRAND_NAME, BrandingUtil.getInstallationBrandName());
                    notificationObjectBuilder.add(USER_GUIDE_URL, systemConfig.getGuidesBaseUrl() + "/" + systemConfig.getGuidesVersion());
                }
                case CREATEDS -> {
                }
                case CREATEDV -> {
                }
                case INGESTCOMPLETED -> {
                    Dataset dataset = datasetSvc.find(objectId);
                    if (dataset != null) {
                        String PID = dataset.getGlobalId().asString();
                        notificationObjectBuilder.add(DATASET_RELATIVE_URL_TO_ROOT_WITH_SPA, systemConfig.SPA_PREFIX + "/datasets?persistentId=" + PID);
                        notificationObjectBuilder.add(DATASET_TITLE, dataset.getCurrentName());
                        notificationObjectBuilder.add(USER_GUIDE_TABULAR_INGEST_URL, systemConfig.getGuidesBaseUrl() + "/" + systemConfig.getGuidesVersion() + "/user/dataset-management.html#tabular-data-files");
                    }
                }
                case INGESTCOMPLETEDWITHERRORS -> {
                }
                case PUBLISHEDDS -> {
                    if (objectId != null) {
                        DatasetVersion publishedDatasetVersion = datasetVersionSvc.find(notification.getObjectId());
                        if (publishedDatasetVersion != null) {
                            notificationObjectBuilder.add(DATASET_TITLE, publishedDatasetVersion.getTitle());
                            String PID = publishedDatasetVersion.getDataset().getGlobalId().asString();
                            notificationObjectBuilder.add(DATASET_RELATIVE_URL_TO_ROOT_WITH_SPA, systemConfig.SPA_PREFIX + "/datasets?persistentId=" + PID);
                            Dataverse parentCollection = publishedDatasetVersion.getDataset().getOwner();
                            notificationObjectBuilder.add(PARENT_COLLECTION_NAME, parentCollection.getName());
                            notificationObjectBuilder.add(PARENT_COLLECTION_URL, systemConfig.SPA_PREFIX + "/collections/" + parentCollection.getAlias());
                        }
                    }
                }
                case REQUESTFILEACCESS -> {
                    DataFile requestedFile = fileSvc.find(objectId);
                    String datasetTitle = requestedFile.getOwner().getCurrentName();
                    notificationObjectBuilder.add(REQUESTOR_FIRST_NAME, requestor.getFirstName());
                    notificationObjectBuilder.add(REQUESTOR_LAST_NAME, requestor.getLastName());
                    notificationObjectBuilder.add(REQUESTOR_EMAIL, requestor.getEmail());
                    notificationObjectBuilder.add(DATASET_TITLE, datasetTitle);
                    notificationObjectBuilder.add(MANAGE_FILE_PERMISSIONS_RELATIVE_URL_TO_ROOT_WITH_SPA, systemConfig.SPA_PREFIX + "/permissions-manage-files?Id=" + objectId);
                }
                case REQUESTEDFILEACCESS -> {
                }
                case GRANTFILEACCESS -> {
                }
                case REJECTFILEACCESS -> {
                }
                case SUBMITTEDDS -> {
                    if (objectId != null) {
                        notificationObjectBuilder.add(OBJECT_ID, objectId);
                        DatasetVersion submittedDatasetVersion = datasetVersionSvc.find(notification.getObjectId());
                        if (submittedDatasetVersion != null) {
                            notificationObjectBuilder.add(DATASET_TITLE, submittedDatasetVersion.getTitle());
                            String PID = submittedDatasetVersion.getDataset().getGlobalId().asString();
                            notificationObjectBuilder.add(DATASET_RELATIVE_URL_TO_ROOT_WITH_SPA, systemConfig.SPA_PREFIX + "/datasets?persistentId=" + PID + "&version=DRAFT");
                            notificationObjectBuilder.add(REQUESTOR_FIRST_NAME, requestor.getFirstName());
                            notificationObjectBuilder.add(REQUESTOR_LAST_NAME, requestor.getLastName());
                            notificationObjectBuilder.add(REQUESTOR_EMAIL, requestor.getEmail());
                            Dataverse parentCollection = submittedDatasetVersion.getDataset().getOwner();
                            notificationObjectBuilder.add(PARENT_COLLECTION_NAME, parentCollection.getName());
                            notificationObjectBuilder.add(PARENT_COLLECTION_URL, systemConfig.SPA_PREFIX + "/collections/" + parentCollection.getAlias());
                        }
                    }
                }
                case RETURNEDDS -> {
                    if (objectId != null) {
                        DatasetVersion submittedDatasetVersion = datasetVersionSvc.find(notification.getObjectId());
                        if (submittedDatasetVersion != null) {
                            notificationObjectBuilder.add(DATASET_TITLE, submittedDatasetVersion.getTitle());
                            String PID = submittedDatasetVersion.getDataset().getGlobalId().asString();
                            notificationObjectBuilder.add(DATASET_RELATIVE_URL_TO_ROOT_WITH_SPA, systemConfig.SPA_PREFIX + "/datasets?persistentId=" + PID + "&version=DRAFT");
                            Dataverse parentCollection = submittedDatasetVersion.getDataset().getOwner();
                            notificationObjectBuilder.add(PARENT_COLLECTION_NAME, parentCollection.getName());
                            notificationObjectBuilder.add(PARENT_COLLECTION_URL, systemConfig.SPA_PREFIX + "/collections/" + parentCollection.getAlias());
                        }
                    }
                }
                case ASSIGNROLE -> {
                }
                case REVOKEROLE -> {
                }
                default -> {
                }
            }

            notificationObjectBuilder.add("sentTimestamp", notification.getSendDateTimestamp());
            jsonArrayBuilder.add(notificationObjectBuilder);
        }
        JsonObjectBuilder result = Json.createObjectBuilder().add("notifications", jsonArrayBuilder);
        return ok(result);
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

    private JsonArrayBuilder getReasonsForReturn(UserNotification notification) {
        Long objectId = notification.getObjectId();
        return WorkflowUtil.getAllWorkflowComments(datasetVersionSvc.find(objectId));
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
