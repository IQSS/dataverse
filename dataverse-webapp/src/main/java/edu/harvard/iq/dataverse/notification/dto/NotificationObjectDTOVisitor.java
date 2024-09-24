package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectVisitor;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;

/**
 * Notification object visitor adding the actual object to the DTO.
 */
class NotificationObjectDTOVisitor implements NotificationObjectVisitor {

    private final UserNotification userNotification;
    private final UserNotificationDTO notificationDTO;

    // -------------------- CONSTRUCTORS --------------------

    private NotificationObjectDTOVisitor(UserNotification userNotification, UserNotificationDTO notificationDTO) {
        this.userNotification = userNotification;
        this.notificationDTO = notificationDTO;
    }

    // -------------------- GETTERS --------------------

    @Override
    public UserNotification getNotification() {
        return userNotification;
    }

    // -------------------- LOGIC --------------------

    static public NotificationObjectDTOVisitor toDTO(UserNotification userNotification, UserNotificationDTO notificationDTO) {
        return new NotificationObjectDTOVisitor(userNotification, notificationDTO);
    }


    @Override
    public void handleDataFile(DataFile dataFile) {
        notificationDTO.setTheDataFileObject(dataFile);
    }

    @Override
    public void handleFileMetadata(FileMetadata fileMetadata) {
        notificationDTO.setTheFileMetadataObject(fileMetadata);
    }

    @Override
    public void handleDataverse(Dataverse dataverse) {
        notificationDTO.setTheDataverseObject(dataverse);
    }

    @Override
    public void handleDataset(Dataset dataset) {
        notificationDTO.setTheDatasetObject(dataset);
    }

    @Override
    public void handleDatasetVersion(DatasetVersion version) {
        notificationDTO.setTheDatasetVersionObject(version);
    }

    @Override
    public void handleUser(AuthenticatedUser authenticatedUser) {
        notificationDTO.setTheAuthenticatedUserObject(authenticatedUser);
    }
}
