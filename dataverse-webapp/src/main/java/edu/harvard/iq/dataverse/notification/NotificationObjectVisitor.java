package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;

/**
 * Interface allowing to handle the object which triggered the notification.
 */
public interface NotificationObjectVisitor {

    /**
     * The visited notification.
     */
    UserNotification getNotification();

    void handleDataFile(DataFile dataFile);

    void handleFileMetadata(FileMetadata fileMetadata);

    void handleDataverse(Dataverse dataverse);

    void handleDataset(Dataset dataset);

    void handleDatasetVersion(DatasetVersion version);

    void handleUser(AuthenticatedUser authenticatedUser);
}
