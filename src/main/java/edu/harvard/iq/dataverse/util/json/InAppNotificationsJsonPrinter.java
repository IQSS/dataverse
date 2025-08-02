package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonRoleAssignments;

public class InAppNotificationsJsonPrinter {

    @EJB
    private static DataverseServiceBean dataverseService;

    @EJB
    private static DatasetServiceBean datasetService;

    @EJB
    private static DataFileServiceBean dataFileService;

    @EJB
    private static PermissionServiceBean permissionService;

    @EJB
    private static SystemConfig systemConfig;

    public static void addFieldsByType(NullSafeJsonBuilder notificationJson, AuthenticatedUser authenticatedUser, UserNotification userNotification) {
        Long objectId = userNotification.getObjectId();
        AuthenticatedUser requestor = userNotification.getRequestor();
        switch (userNotification.getType()) {
            case ASSIGNROLE:
            case REVOKEROLE:
                addRoleFields(notificationJson, authenticatedUser, userNotification);
            case CREATEDV:
                addCreateDataverseFields(notificationJson, userNotification);
            case REQUESTFILEACCESS:
                addRequestFileAccessFields(notificationJson, userNotification, requestor);
            case REQUESTEDFILEACCESS:
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
                addDataFileFields(notificationJson, userNotification);
            case DATASETCREATED:
                addDatasetCreatedFields(notificationJson, userNotification, requestor);
            case CREATEDS:
                addCreateDatasetFields(notificationJson, userNotification);
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case PUBLISHFAILED_PIDREG:
            case RETURNEDDS:
            case WORKFLOW_SUCCESS:
            case WORKFLOW_FAILURE:
            case STATUSUPDATED:
            case PIDRECONCILED:
            case CREATEACC:
                addCreateAccountFields(notificationJson);
            case CHECKSUMFAIL:
            case FILESYSTEMIMPORT:
            case GLOBUSUPLOADCOMPLETED:
            case GLOBUSDOWNLOADCOMPLETED:
            case GLOBUSUPLOADCOMPLETEDWITHERRORS:
            case GLOBUSUPLOADREMOTEFAILURE:
            case GLOBUSUPLOADLOCALFAILURE:
            case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
            case CHECKSUMIMPORT:
            case CONFIRMEMAIL:
            case APIGENERATED:
            case INGESTCOMPLETED:
            case INGESTCOMPLETEDWITHERRORS:
            case DATASETMENTIONED:
        }
    }

    private static void addRoleFields(NullSafeJsonBuilder notificationJson, AuthenticatedUser authenticatedUser, UserNotification userNotification) {
        Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
        if (dataverse != null) {
            notificationJson.add("roleAssignments", jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataverse)));
            notificationJson.add("dataverseAlias", dataverse.getAlias());
            notificationJson.add("dataverseDisplayName", dataverse.getDisplayName());
        } else {
            Dataset dataset = datasetService.find(userNotification.getObjectId());
            if (dataset != null) {
                notificationJson.add("roleAssignments", jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataverse)));
                notificationJson.add("datasetPersistentIdentifier", dataset.getGlobalId().asString());
                notificationJson.add("datasetDisplayName", dataset.getDisplayName());
            } else {
                DataFile datafile = dataFileService.find(userNotification.getObjectId());
                notificationJson.add("roleAssignments", jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataverse)));
                notificationJson.add("ownerPersistentIdentifier", datafile.getOwner().getGlobalId().asString());
                notificationJson.add("ownerDisplayName", datafile.getOwner().getDisplayName());
            }
        }
    }

    private static void addCreateDataverseFields(NullSafeJsonBuilder notificationJson, UserNotification userNotification) {
        Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
        if (dataverse != null) {
            notificationJson.add("dataverseAlias", dataverse.getAlias());
            notificationJson.add("dataverseDisplayName", dataverse.getDisplayName());
            notificationJson.add("ownerAlias", dataverse.getOwner().getAlias());
            notificationJson.add("ownerDisplayName", dataverse.getOwner().getDisplayName());
        }
        addGuidesFields(notificationJson);
    }

    private static void addCreateAccountFields(NullSafeJsonBuilder notificationJson) {
        notificationJson.add("rootDataverseName", dataverseService.findRootDataverse().getName());
        addGuidesFields(notificationJson);
    }

    private static void addRequestFileAccessFields(NullSafeJsonBuilder notificationJson, UserNotification userNotification, AuthenticatedUser requestor) {
        addRequestorFields(notificationJson, requestor);
        addDataFileFields(notificationJson, userNotification);
    }

    private static void addDataFileFields(NullSafeJsonBuilder notificationJson, UserNotification userNotification) {
        DataFile dataFile = dataFileService.find(userNotification.getObjectId());
        if (dataFile != null) {
            notificationJson.add("dataFileId", dataFile.getId());
            notificationJson.add("dataFileDisplayName", dataFile.getDisplayName());
        }
    }

    private static void addDatasetCreatedFields(NullSafeJsonBuilder notificationJson, UserNotification userNotification, AuthenticatedUser requestor) {
        addDatasetFields(notificationJson, userNotification);
        addRequestorFields(notificationJson, requestor);
    }

    private static void addRequestorFields(NullSafeJsonBuilder notificationJson, AuthenticatedUser requestor) {
        notificationJson.add("requestorFirstName", requestor.getFirstName());
        notificationJson.add("requestorLastName", requestor.getLastName());
        notificationJson.add("requestorEmail", requestor.getEmail());
    }

    private static void addDatasetFields(NullSafeJsonBuilder notificationJson, UserNotification userNotification) {
        Dataset dataset = datasetService.find(userNotification.getObjectId());
        if (dataset != null) {
            notificationJson.add("datasetPersistentIdentifier", dataset.getGlobalId().asString());
            notificationJson.add("datasetDisplayName", dataset.getDisplayName());
            notificationJson.add("ownerAlias", dataset.getOwner().getAlias());
            notificationJson.add("ownerDisplayName", dataset.getOwner().getDisplayName());
        }
    }

    private static void addCreateDatasetFields(NullSafeJsonBuilder notificationJson, UserNotification userNotification) {
        addGuidesFields(notificationJson);
        addDatasetFields(notificationJson, userNotification);
    }

    private static void addGuidesFields(NullSafeJsonBuilder notificationJson) {
        notificationJson.add("userGuidesBaseUrl", systemConfig.getGuidesBaseUrl());
        notificationJson.add("userGuidesVersion", systemConfig.getGuidesVersion());
    }
}
