package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.notification.NotificationObjectResolver;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.NotificationParametersUtil;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
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

import static edu.harvard.iq.dataverse.persistence.user.NotificationType.ASSIGNROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.GRANTFILEACCESSINFO;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REVOKEROLE;
import static java.util.stream.Collectors.joining;

@Stateless
public class UserNotificationMapper {

    private PermissionServiceBean permissionService;
    private NotificationObjectResolver notificationObjectResolver;
    private AuthenticatedUserRepository authenticatedUserRepository;
    private NotificationParametersUtil notificationParametersUtil;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public UserNotificationMapper() {
        this.notificationParametersUtil = new NotificationParametersUtil();
    }

    @Inject
    public UserNotificationMapper(PermissionServiceBean permissionService,
                                  NotificationObjectResolver notificationObjectResolver,
                                  AuthenticatedUserRepository authenticatedUserRepository) {
        this();
        this.permissionService = permissionService;
        this.notificationObjectResolver = notificationObjectResolver;
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

        notificationObjectResolver.resolve(NotificationObjectDTOVisitor.toDTO(userNotification, notificationDTO));
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
