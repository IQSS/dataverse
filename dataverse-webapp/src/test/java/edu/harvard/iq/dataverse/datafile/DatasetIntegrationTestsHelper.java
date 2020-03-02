package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.sql.Timestamp;
import java.time.Instant;

public class DatasetIntegrationTestsHelper {

    public static final long DRAFT_DATASET_WITH_FILES_ID = 52;

    public static void publishDataset(Dataset dataset, AuthenticatedUser user) {
        Timestamp currentTimestamp = Timestamp.from(Instant.now());
        dataset.setPublicationDate(currentTimestamp);
        dataset.setModificationTime(currentTimestamp);
        dataset.setReleaseUser(user);

        DatasetVersion version = dataset.getLatestVersion();
        version.setVersion(13L);
        version.setVersionNumber(1L);
        version.setMinorVersionNumber(0L);
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        version.setLastUpdateTime(currentTimestamp);
        version.setReleaseTime(currentTimestamp);

        Dataverse owner = dataset.getOwner();
        owner.setPublicationDate(currentTimestamp);
        owner.setModificationTime(currentTimestamp);

        for (DataFile dataFile : dataset.getFiles()) {
            dataFile.setPublicationDate(currentTimestamp);
            // Following line is required because, when the fileMetadata
            // are not included in persistence context, then on deletion
            // JPA provider tries to delete entities in wrong order, and
            // that causes sql error (more precisely: it starts removing
            // from FileTermsOfUse entities instead of FileMetadata).
            dataFile.getFileMetadata();
        }
    }
}
