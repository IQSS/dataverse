package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import static edu.harvard.iq.dataverse.dataset.DatasetUtil.getLocaleCurationStatusLabel;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonRoleAssignments;

/**
 * A helper class to build a JSON representation of a UserNotification.
 * <p>
 * It is responsible for adding the correct fields to a JSON object based on the
 * notification type.
 */
@Stateless
public class InAppNotificationsJsonPrinter {

    public static final String KEY_ROLE_ASSIGNMENTS = "roleAssignments";
    public static final String KEY_DATAVERSE_ALIAS = "dataverseAlias";
    public static final String KEY_DATAVERSE_DISPLAY_NAME = "dataverseDisplayName";
    public static final String KEY_DATASET_PERSISTENT_ID = "datasetPersistentIdentifier";
    public static final String KEY_DATASET_DISPLAY_NAME = "datasetDisplayName";
    public static final String KEY_OWNER_PERSISTENT_ID = "ownerPersistentIdentifier";
    public static final String KEY_OWNER_ALIAS = "ownerAlias";
    public static final String KEY_OWNER_DISPLAY_NAME = "ownerDisplayName";
    public static final String KEY_REQUESTOR_FIRST_NAME = "requestorFirstName";
    public static final String KEY_REQUESTOR_LAST_NAME = "requestorLastName";
    public static final String KEY_REQUESTOR_EMAIL = "requestorEmail";
    public static final String KEY_DATAFILE_ID = "dataFileId";
    public static final String KEY_DATAFILE_DISPLAY_NAME = "dataFileDisplayName";
    public static final String KEY_ROOT_DATAVERSE_NAME = "rootDataverseName";
    public static final String KEY_GUIDES_BASE_URL = "userGuidesBaseUrl";
    public static final String KEY_GUIDES_VERSION = "userGuidesVersion";
    public static final String KEY_GUIDES_SECTION_PATH = "userGuidesSectionPath";
    public static final String KEY_CURATION_STATUS = "currentCurationStatus";
    public static final String KEY_ADDITIONAL_INFO = "additionalInfo";

    public static final String GUIDES_SECTION_PATH_DATAVERSE_MANAGEMENT_HTML = "user/dataverse-management.html";
    public static final String GUIDES_SECTION_PATH_DATASET_MANAGEMENT_HTML = "user/dataset-management.html";
    public static final String GUIDES_SECTION_PATH_USER_HTML = "user/index.html";

    @EJB
    private DataverseServiceBean dataverseService;
    @EJB
    private DatasetServiceBean datasetService;
    @EJB
    private DatasetVersionServiceBean datasetVersionService;
    @EJB
    private DataFileServiceBean dataFileService;
    @EJB
    private PermissionServiceBean permissionService;
    @EJB
    private SystemConfig systemConfig;

    /**
     * Populates a JSON builder with fields specific to the notification type.
     *
     * @param notificationJson  The JSON builder to add fields to.
     * @param authenticatedUser The user receiving the notification.
     * @param userNotification  The notification object containing the details.
     */
    public void addFieldsByType(final NullSafeJsonBuilder notificationJson, final AuthenticatedUser authenticatedUser, final UserNotification userNotification) {
        final AuthenticatedUser requestor = userNotification.getRequestor();

        switch (userNotification.getType()) {
            case ASSIGNROLE:
            case REVOKEROLE:
                addRoleFields(notificationJson, authenticatedUser, userNotification);
                break;
            case CREATEDV:
                addCreateDataverseFields(notificationJson, userNotification);
                break;
            case REQUESTFILEACCESS:
                addRequestFileAccessFields(notificationJson, userNotification, requestor);
                break;
            case REQUESTEDFILEACCESS:
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
                addDataFileFields(notificationJson, userNotification);
                break;
            case DATASETCREATED:
                addDatasetCreatedFields(notificationJson, userNotification, requestor);
                break;
            case CREATEDS:
                addCreateDatasetFields(notificationJson, userNotification);
                break;
            case SUBMITTEDDS:
                addSubmittedDatasetFields(notificationJson, userNotification, requestor);
                break;
            case PUBLISHEDDS:
            case PUBLISHFAILED_PIDREG:
            case RETURNEDDS:
            case WORKFLOW_SUCCESS:
            case WORKFLOW_FAILURE:
            case PIDRECONCILED:
            case FILESYSTEMIMPORT:
            case CHECKSUMIMPORT:
                addDatasetVersionFields(notificationJson, userNotification);
                break;
            case STATUSUPDATED:
                addDatasetVersionFields(notificationJson, userNotification, true);
                break;
            case CREATEACC:
                addCreateAccountFields(notificationJson);
                break;
            case GLOBUSUPLOADCOMPLETED:
            case GLOBUSDOWNLOADCOMPLETED:
            case GLOBUSUPLOADCOMPLETEDWITHERRORS:
            case GLOBUSUPLOADREMOTEFAILURE:
            case GLOBUSUPLOADLOCALFAILURE:
            case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
            case CHECKSUMFAIL:
                addDatasetFields(notificationJson, userNotification);
                break;
            case INGESTCOMPLETED:
            case INGESTCOMPLETEDWITHERRORS:
                addIngestFields(notificationJson, userNotification);
                break;
            case DATASETMENTIONED:
                addDatasetMentionedFields(notificationJson, userNotification);
                break;
        }
    }

    private void addRoleFields(final NullSafeJsonBuilder notificationJson, final AuthenticatedUser authenticatedUser, final UserNotification userNotification) {
        Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
        if (dataverse != null) {
            notificationJson.add(KEY_ROLE_ASSIGNMENTS, jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataverse)));
            notificationJson.add(KEY_DATAVERSE_ALIAS, dataverse.getAlias());
            notificationJson.add(KEY_DATAVERSE_DISPLAY_NAME, dataverse.getDisplayName());
        } else {
            Dataset dataset = datasetService.find(userNotification.getObjectId());
            if (dataset != null) {
                notificationJson.add(KEY_ROLE_ASSIGNMENTS, jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataset)));
                notificationJson.add(KEY_DATASET_PERSISTENT_ID, dataset.getGlobalId().asString());
                notificationJson.add(KEY_DATASET_DISPLAY_NAME, dataset.getDisplayName());
            } else {
                DataFile datafile = dataFileService.find(userNotification.getObjectId());
                notificationJson.add(KEY_ROLE_ASSIGNMENTS, jsonRoleAssignments(permissionService.getEffectiveRoleAssignments(authenticatedUser, datafile)));
                notificationJson.add(KEY_OWNER_PERSISTENT_ID, datafile.getOwner().getGlobalId().asString());
                notificationJson.add(KEY_OWNER_DISPLAY_NAME, datafile.getOwner().getDisplayName());
            }
        }
    }

    private void addCreateDataverseFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        final Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
        if (dataverse != null) {
            notificationJson.add(KEY_DATAVERSE_ALIAS, dataverse.getAlias());
            notificationJson.add(KEY_DATAVERSE_DISPLAY_NAME, dataverse.getDisplayName());
            Dataverse owner = dataverse.getOwner();
            if (owner != null) {
                notificationJson.add(KEY_OWNER_ALIAS, owner.getAlias());
                notificationJson.add(KEY_OWNER_DISPLAY_NAME, owner.getDisplayName());
            }
        }
        addGuidesFields(notificationJson, GUIDES_SECTION_PATH_DATAVERSE_MANAGEMENT_HTML);
    }

    private void addCreateAccountFields(final NullSafeJsonBuilder notificationJson) {
        notificationJson.add(KEY_ROOT_DATAVERSE_NAME, dataverseService.findRootDataverse().getName());
        addGuidesFields(notificationJson, GUIDES_SECTION_PATH_USER_HTML);
    }

    private void addRequestFileAccessFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification, final AuthenticatedUser requestor) {
        addRequestorFields(notificationJson, requestor);
        addDataFileFields(notificationJson, userNotification);
    }

    private void addDataFileFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        final DataFile dataFile = dataFileService.find(userNotification.getObjectId());
        if (dataFile != null) {
            notificationJson.add(KEY_DATAFILE_ID, dataFile.getId());
            notificationJson.add(KEY_DATAFILE_DISPLAY_NAME, dataFile.getDisplayName());
        }
    }

    private void addDatasetCreatedFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification, final AuthenticatedUser requestor) {
        addDatasetFields(notificationJson, userNotification);
        addRequestorFields(notificationJson, requestor);
    }

    private void addRequestorFields(final NullSafeJsonBuilder notificationJson, final AuthenticatedUser requestor) {
        notificationJson.add(KEY_REQUESTOR_FIRST_NAME, requestor.getFirstName());
        notificationJson.add(KEY_REQUESTOR_LAST_NAME, requestor.getLastName());
        notificationJson.add(KEY_REQUESTOR_EMAIL, requestor.getEmail());
    }

    private void addDatasetFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        final Dataset dataset = datasetService.find(userNotification.getObjectId());
        if (dataset != null) {
            notificationJson.add(KEY_DATASET_PERSISTENT_ID, dataset.getGlobalId().asString());
            notificationJson.add(KEY_DATASET_DISPLAY_NAME, dataset.getDisplayName());
            notificationJson.add(KEY_OWNER_ALIAS, dataset.getOwner().getAlias());
            notificationJson.add(KEY_OWNER_DISPLAY_NAME, dataset.getOwner().getDisplayName());
        }
    }

    private void addCreateDatasetFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        addGuidesFields(notificationJson, GUIDES_SECTION_PATH_DATASET_MANAGEMENT_HTML);
        addDatasetVersionFields(notificationJson, userNotification);
    }

    private void addGuidesFields(final NullSafeJsonBuilder notificationJson) {
        addGuidesFields(notificationJson, null);
    }

    private void addGuidesFields(final NullSafeJsonBuilder notificationJson, String guidesSectionPath) {
        notificationJson.add(KEY_GUIDES_BASE_URL, systemConfig.getGuidesBaseUrl());
        notificationJson.add(KEY_GUIDES_VERSION, systemConfig.getGuidesVersion());

        if (guidesSectionPath != null) {
            notificationJson.add(KEY_GUIDES_SECTION_PATH, guidesSectionPath);
        }
    }

    private void addSubmittedDatasetFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification, final AuthenticatedUser requestor) {
        addDatasetFields(notificationJson, userNotification);
        addRequestorFields(notificationJson, requestor);
    }

    private void addDatasetVersionFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        addDatasetVersionFields(notificationJson, userNotification, false);
    }

    private void addDatasetVersionFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification, final boolean addCurationStatus) {
        final DatasetVersion datasetVersion = datasetVersionService.find(userNotification.getObjectId());
        if (datasetVersion != null) {
            Dataset dataset = datasetVersion.getDataset();
            notificationJson.add(KEY_DATASET_PERSISTENT_ID, dataset.getGlobalId().asString());
            notificationJson.add(KEY_DATASET_DISPLAY_NAME, dataset.getDisplayName());
            notificationJson.add(KEY_OWNER_ALIAS, dataset.getOwner().getAlias());
            notificationJson.add(KEY_OWNER_DISPLAY_NAME, dataset.getOwner().getDisplayName());
            if (addCurationStatus) {
                notificationJson.add(KEY_CURATION_STATUS, getLocaleCurationStatusLabel(datasetVersion.getCurrentCurationStatus()));
            }
        }
    }

    private void addIngestFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        addDatasetFields(notificationJson, userNotification);
        addGuidesFields(notificationJson);
    }

    private void addDatasetMentionedFields(final NullSafeJsonBuilder notificationJson, final UserNotification userNotification) {
        addDatasetFields(notificationJson, userNotification);
        notificationJson.add(KEY_ADDITIONAL_INFO, userNotification.getAdditionalInfo());
    }
}
