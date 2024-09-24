package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadataRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Optional;

import static edu.harvard.iq.dataverse.persistence.user.NotificationType.ASSIGNROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CHECKSUMFAIL;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CHECKSUMIMPORT;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEACC;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDV;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.FILESYSTEMIMPORT;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.GRANTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.GRANTFILEACCESSINFO;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.MAPLAYERDELETEFAILED;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.MAPLAYERUPDATED;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.PUBLISHEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REJECTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REJECTFILEACCESSINFO;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REQUESTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.RETURNEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REVOKEROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.SUBMITTEDDS;

/**
 * Lookup of the notification object based on the type of notification
 * and registered object id.
 */
@Stateless
public class NotificationObjectResolver {

    @Inject
    private DataverseRepository dataverseRepository;
    @Inject
    private DatasetRepository datasetRepository;
    @Inject
    private DataFileRepository dataFileRepository;
    @Inject
    private DatasetVersionRepository datasetVersionRepository;
    @Inject
    private FileMetadataRepository fileMetadataRepository;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement*/
    public NotificationObjectResolver() {
    }

    public NotificationObjectResolver(DataverseRepository dataverseRepository, DatasetRepository datasetRepository, DataFileRepository dataFileRepository, DatasetVersionRepository datasetVersionRepository, FileMetadataRepository fileMetadataRepository) {
        this.dataverseRepository = dataverseRepository;
        this.datasetRepository = datasetRepository;
        this.dataFileRepository = dataFileRepository;
        this.datasetVersionRepository = datasetVersionRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    // -------------------- LOGIC --------------------

    public void resolve(NotificationObjectVisitor visitor) {
        UserNotification userNotification = visitor.getNotification();

        Long objectId = userNotification.getObjectId();
        if (objectId == null) {
            if (CREATEACC.equals(userNotification.getType())) {
                visitor.handleUser(userNotification.getUser());
            }
            return;
        }

        switch (userNotification.getType()) {
            case ASSIGNROLE:
            case REVOKEROLE:
                // Can either be a dataverse, dataset or datafile, so search all
                Optional<Dataverse> dataverse = dataverseRepository.findById(objectId);

                if (dataverse.isPresent()) {
                    visitor.handleDataverse(dataverse.get());
                } else {
                    Optional<Dataset> dataset = datasetRepository.findById(objectId);
                    if (dataset.isPresent()) {
                        visitor.handleDataset(dataset.get());
                    } else {
                        dataFileRepository.findById(objectId).ifPresent(visitor::handleDataFile);
                    }
                }

                break;
            case CREATEDV:
                dataverseRepository.findById(objectId).ifPresent(visitor::handleDataverse);
                break;

            case REQUESTFILEACCESS:
                dataFileRepository.findById(objectId)
                        .map(DataFile::getOwner)
                        .ifPresent(visitor::handleDataset);
                break;
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
            case CHECKSUMFAIL:
            case GRANTFILEACCESSINFO:
            case REJECTFILEACCESSINFO:
                datasetRepository.findById(objectId).ifPresent(visitor::handleDataset);
                break;

            case MAPLAYERUPDATED:
            case CREATEDS:
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case RETURNEDDS:
            case FILESYSTEMIMPORT:
            case CHECKSUMIMPORT:
                datasetVersionRepository.findById(objectId).ifPresent(visitor::handleDatasetVersion);
                break;

            case MAPLAYERDELETEFAILED:
                fileMetadataRepository.findById(objectId).ifPresent(visitor::handleFileMetadata);
                break;

            default:
                if (!isBaseNotification(userNotification)) {
                    datasetRepository.findById(objectId)
                            .ifPresent(visitor::handleDataset);
                }
                break;
        }
    }

    // -------------------- PRIVATE --------------------

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
}
