package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Handler for notification objects, filling the searchLabel with appropriate
 * content.
 */
public class NotificationObjectSearchLabelVisitor implements NotificationObjectVisitor {

    private final UserNotification userNotification;

    // -------------------- CONSTRUCTORS --------------------

    private NotificationObjectSearchLabelVisitor(UserNotification notification) {
        this.userNotification = notification;
    }

    // -------------------- GETTERS --------------------

    @Override
    public UserNotification getNotification() {
        return userNotification;
    }

    // -------------------- LOGIC --------------------

    static public NotificationObjectSearchLabelVisitor onNotification(UserNotification notification) {
        return new NotificationObjectSearchLabelVisitor(notification);
    }


    @Override
    public void handleDataFile(DataFile dataFile) {
        userNotification.setSearchLabel(dataFile.getDisplayName());
    }

    @Override
    public void handleFileMetadata(FileMetadata fileMetadata) {
        DatasetVersion datasetVersion = fileMetadata.getDatasetVersion();
        Dataverse dataverse = datasetVersion.getDataset().getOwner();
        userNotification.setSearchLabel(concat(
                fileMetadata.getLabel(),
                datasetVersion.getParsedTitle(),
                dataverse.getDisplayName()));
    }

    @Override
    public void handleDataverse(Dataverse dataverse) {
        userNotification.setSearchLabel(dataverse.getDisplayName());
    }

    @Override
    public void handleDataset(Dataset dataset) {
        Dataverse dataverse = dataset.getOwner();
        userNotification.setSearchLabel(concat(dataset.getDisplayName(), dataverse.getDisplayName()));
    }

    @Override
    public void handleDatasetVersion(DatasetVersion version) {
        Dataverse dataverse = version.getDataset().getOwner();
        userNotification.setSearchLabel(concat(version.getParsedTitle(), dataverse.getDisplayName()));
    }

    @Override
    public void handleUser(AuthenticatedUser authenticatedUser) {
        userNotification.setSearchLabel(authenticatedUser.getEmail());
    }

    // -------------------- PRIVATE --------------------

    private String concat(String... labels) {
        return Arrays.stream(labels).filter(StringUtils::isNotBlank).collect(Collectors.joining(" "));
    }
}
