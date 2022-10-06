package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.NotificationParametersUtil;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadataRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

import static edu.harvard.iq.dataverse.persistence.user.NotificationType.*;
import static java.util.stream.Collectors.joining;

@Stateless
public class UserNotificationMapper {

    private DataverseRepository dataverseRepository;
    private DatasetRepository datasetRepository;
    private DatasetVersionRepository datasetVersionRepository;
    private DataFileRepository dataFileRepository;
    private FileMetadataRepository fileMetadataRepository;
    private PermissionServiceBean permissionService;
    private UserNotificationService userNotificationService;
    private AuthenticatedUserRepository authenticatedUserRepository;
    private NotificationParametersUtil notificationParametersUtil;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public UserNotificationMapper() {
        this.notificationParametersUtil = new NotificationParametersUtil();
    }

    @Inject
    public UserNotificationMapper(DataverseRepository dataverseRepository, DatasetRepository datasetRepository,
                                  DatasetVersionRepository datasetVersionRepository, DataFileRepository dataFileRepository,
                                  FileMetadataRepository fileMetadataRepository, PermissionServiceBean permissionService,
                                  UserNotificationService userNotificationService, AuthenticatedUserRepository authenticatedUserRepository) {
        this();
        this.dataverseRepository = dataverseRepository;
        this.datasetRepository = datasetRepository;
        this.datasetVersionRepository = datasetVersionRepository;
        this.dataFileRepository = dataFileRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.permissionService = permissionService;
        this.userNotificationService = userNotificationService;
        this.authenticatedUserRepository = authenticatedUserRepository;
    }

    // -------------------- LOGIC --------------------

    public UserNotificationDTO toDTO(UserNotification userNotification) {
        UserNotificationDTO notificationDTO = new UserNotificationDTO();
        Map<String, String> parameters = notificationParametersUtil.getParameters(userNotification);

        notificationDTO.setId(userNotification.getId());
        notificationDTO.setDisplayAsRead(userNotification.isReadNotification());
        notificationDTO.setSendDate(userNotification.getSendDate());
        notificationDTO.setType(userNotification.getType());
        notificationDTO.setAdditionalMessage(parameters.get(NotificationParameter.MESSAGE.key()));

        AuthenticatedUser requestor = findRequestor(parameters);
        notificationDTO.setRequestorName(getRequestorName(userNotification, requestor));
        notificationDTO.setRequestorEmail(getRequestorEmail(userNotification, requestor));
        notificationDTO.setRejectedOrGrantedBy(GRANTFILEACCESSINFO.equals(userNotification.getType())
                ? parameters.get(NotificationParameter.GRANTED_BY.key())
                : parameters.get(NotificationParameter.REJECTED_BY.key()));

        assignNotificationObject(notificationDTO, userNotification);
        assignRoles(notificationDTO, userNotification);
        notificationDTO.setReplyTo(parameters.get(NotificationParameter.REPLY_TO.key()));

        return notificationDTO;
    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUser findRequestor(Map<String, String> parameters) {
        String requestorId = parameters.get(NotificationParameter.REQUESTOR_ID.key());
        Long id = requestorId != null ? Long.valueOf(requestorId) : null;
        if (id == null) {
            return null;
        }
        return authenticatedUserRepository.findById(id).orElse(null);
    }

    private void assignNotificationObject(UserNotificationDTO notificationDTO, UserNotification userNotification) {
        Long objectId = userNotification.getObjectId();

        switch (userNotification.getType()) {
            case ASSIGNROLE:
            case REVOKEROLE:
                // Can either be a dataverse, dataset or datafile, so search all
                dataverseRepository.findById(objectId).ifPresent(notificationDTO::setTheDataverseObject);

                if (notificationDTO.getTheObject() == null) {
                    datasetRepository.findById(objectId).ifPresent(notificationDTO::setTheDatasetObject);
                }
                if (notificationDTO.getTheObject() == null) {
                    dataFileRepository.findById(objectId).ifPresent(notificationDTO::setTheDataFileObject);
                }

                break;
            case CREATEDV:
                dataverseRepository.findById(objectId).ifPresent(notificationDTO::setTheDataverseObject);
                break;

            case REQUESTFILEACCESS:
                dataFileRepository.findById(objectId)
                    .map(DataFile::getOwner)
                    .ifPresent(notificationDTO::setTheDatasetObject);
                break;
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
            case CHECKSUMFAIL:
            case GRANTFILEACCESSINFO:
            case REJECTFILEACCESSINFO:
                datasetRepository.findById(objectId).ifPresent(notificationDTO::setTheDatasetObject);
                break;

            case MAPLAYERUPDATED:
            case CREATEDS:
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case RETURNEDDS:
            case FILESYSTEMIMPORT:
            case CHECKSUMIMPORT:
                datasetVersionRepository.findById(objectId).ifPresent(notificationDTO::setTheDatasetVersionObject);
                break;

            case MAPLAYERDELETEFAILED:
                fileMetadataRepository.findById(objectId).ifPresent(notificationDTO::setTheFileMetadataObject);
                break;

            case CREATEACC:
                notificationDTO.setTheAuthenticatedUserObject(userNotification.getUser());
                break;
            default:
                if (!isBaseNotification(userNotification)) {
                    datasetRepository.findById(objectId)
                            .ifPresent(notificationDTO::setTheDatasetObject);
                }
                break;
            }
    }

    private void assignRoles(UserNotificationDTO notificationDTO, UserNotification userNotification) {
        String notificationType = userNotification.getType();

        if (ASSIGNROLE.equals(notificationType) || REVOKEROLE.equals(notificationType)) {
            notificationDTO.setRoleString(
                    getRoleStringFromUser(userNotification.getUser(), (DvObject)notificationDTO.getTheObject()));
        }
    }

    private String getRoleStringFromUser(AuthenticatedUser au, DvObject dvObj) {
        // Find user's role(s) for given dataverse/dataset
        Set<RoleAssignment> roles = permissionService.getRolesOfUser(au, dvObj);

        if (roles.isEmpty()) {
            return "[Unknown]";
        }

        return roles.stream()
                .map(roleAssignment -> roleAssignment.getRole().getName())
                .collect(joining("/"));
    }

    /**
    Returns true if notification display is handled by main Dataverse repository.
    <p>
    Note that external notifications only works if object associated with notification ({@link #getObjectId()})
    is a {@link Dataset}
    <p>
    Notifications should be redesigned to properly support external notifications.
    */
   private boolean isBaseNotification(UserNotification userNotification) {
       return NotificationType.getTypes().contains(userNotification.getType());
   }

   private String getRequestorName(UserNotification notification, AuthenticatedUser requestor) {
       if (requestor == null || requestor.getFirstName() == null || requestor.getLastName() == null) {
           return BundleUtil.getStringFromBundle("notification.email.info.unavailable");
       }
       return requestor.getFirstName() + " " + requestor.getLastName();
   }

   private String getRequestorEmail(UserNotification notification, AuthenticatedUser requestor) {
       if (requestor == null || StringUtils.isBlank(requestor.getEmail())) {
           return BundleUtil.getStringFromBundle("notification.email.info.unavailable");
       }
       return requestor.getEmail();
   }
}
