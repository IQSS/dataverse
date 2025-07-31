package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
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
            // Copied from DataverseUserPage... WIP
            case ASSIGNROLE:
                Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
                if (dataverse != null) {
                    notificationJson.add("roleAssignments", jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataverse)));
                    notificationJson.add("dataverseAlias", dataverse.getAlias());
                    notificationJson.add("dataverseDisplayName", dataverse.getDisplayName());
                } else {
                    Dataset dataset = datasetService.find(userNotification.getObjectId());
                    if (dataset != null) {
                        /*userNotification.setRoleString(getRoleStringFromUser(authenticatedUser, dataset));
                        userNotification.setTheObject(dataset);*/
                    } else {
                        /*DataFile datafile = dataFileService.find(userNotification.getObjectId());
                        userNotification.setRoleString(getRoleStringFromUser(authenticatedUser, datafile));
                        userNotification.setTheObject(datafile);*/
                    }
                }
            case REVOKEROLE:
            case CREATEDV:
            case REQUESTFILEACCESS:
            case REQUESTEDFILEACCESS:
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
            case DATASETCREATED:
            case CREATEDS:
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case PUBLISHFAILED_PIDREG:
            case RETURNEDDS:
            case WORKFLOW_SUCCESS:
            case WORKFLOW_FAILURE:
            case STATUSUPDATED:
            case PIDRECONCILED:
            case CREATEACC:
                notificationJson.add("rootDataverseName", dataverseService.findRootDataverse().getName());
                notificationJson.add("userGuideUrl", systemConfig.getGuidesUrl());
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
}
