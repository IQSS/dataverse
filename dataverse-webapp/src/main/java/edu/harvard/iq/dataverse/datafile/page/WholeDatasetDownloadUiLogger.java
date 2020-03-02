package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.dataset.DownloadDatasetLogService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Stateless
public class WholeDatasetDownloadUiLogger {
    private static final Logger logger = Logger.getLogger(WholeDatasetDownloadUiLogger.class.getSimpleName());

    private DownloadDatasetLogService downloadDatasetLogService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public WholeDatasetDownloadUiLogger() { }

    @Inject
    public WholeDatasetDownloadUiLogger(DownloadDatasetLogService downloadDatasetLogService) {
        this.downloadDatasetLogService = downloadDatasetLogService;
    }

    // -------------------- LOGIC --------------------

    public void incrementLogIfDownloadingWholeDataset(List<FileMetadata> filesMetadata) {
        if (filesMetadata == null || filesMetadata.isEmpty()) {
            logger.log(Level.WARNING, "Empty or null file metadata list.");
            return;
        }

        long datasetId = extractDatasetId(filesMetadata);
        if (datasetId < 0) {
            logger.log(Level.WARNING, "Failed to extract dataset id for metadata: [{0}]", createMetadataInfo(filesMetadata));
            return;
        }

        // In following "if" the order of checking is important
        if (isUserDownloadingAllFilesFromCurrentVersion(filesMetadata)
            || isUserDownloadingAllFilesFromAnyVersion(filesMetadata)) {
            downloadDatasetLogService.incrementDownloadCountForDataset(datasetId);
        }
    }

    // -------------------- PRIVATE --------------------

    /**
     * Because from the UI we can only download files from the same version, it's sufficient
     * that we only compare the size of list of files being downloaded to the size of list of files
     * in current version
     */
    private boolean isUserDownloadingAllFilesFromCurrentVersion(List<FileMetadata> filesMetadata) {
        long currentVersionId = extractVersionIdOfDownloadedFiles(filesMetadata);
        int currentVersionFileCount = extractCurrentVersionFileCountFromMetadata(currentVersionId, filesMetadata);

        return filesMetadata.size() == currentVersionFileCount;
    }

    /**
     * Here we are extracting sets of ids of files from all the versions that have no more files than
     * list of files being downloaded and we check if any of these sets is wholly contained in the set of ids
     * of files being downloaded
     */
    private boolean isUserDownloadingAllFilesFromAnyVersion(List<FileMetadata> filesMetadata) {
        Set<Long> fileIdSet = mapFileMetadataToIds(filesMetadata);
        List<Set<Long>> fileIdSetsInActiveVersions = createPossibleSetsOfFileIdsFromVersions(filesMetadata, fileIdSet);

        return fileIdSetsInActiveVersions.stream()
                .anyMatch(fileIdSet::containsAll);
    }

    private Optional<FileMetadata> takeAnyFileMetadata(List<FileMetadata> filesMetadata) {
        return filesMetadata.stream()
                .findFirst();
    }

    private Long extractVersionIdOfDownloadedFiles(List<FileMetadata> filesMetadata) {
        return takeAnyFileMetadata(filesMetadata)
                .map(FileMetadata::getDatasetVersion)
                .map(DatasetVersion::getId)
                .orElse(-1L);
    }

    private Optional<Dataset> extractDataset(List<FileMetadata> filesMetadata) {
        return takeAnyFileMetadata(filesMetadata)
                .map(FileMetadata::getDataFile)
                .map(DataFile::getOwner);
    }

    private Long extractDatasetId(List<FileMetadata> filesMetadata) {
        return extractDataset(filesMetadata)
                .map(Dataset::getId)
                .orElse(-1L);
    }

    private Integer extractCurrentVersionFileCountFromMetadata(long currentVersionId, List<FileMetadata> filesMetadata) {
        return extractDataset(filesMetadata)
                .map(Dataset::getVersions)
                .orElseGet(Collections::emptyList).stream()
                .filter(v -> v != null && Long.valueOf(currentVersionId).equals(v.getId()))
                .findFirst()
                .map(DatasetVersion::getFileMetadatas)
                .map(List::size)
                .orElse(-1);
    }

    private List<Set<Long>> createPossibleSetsOfFileIdsFromVersions(List<FileMetadata> filesMetadata, Set<Long> fileIdSet) {
        return extractDataset(filesMetadata)
                .filter(d -> !d.hasActiveEmbargo())
                .map(Dataset::getVersions)
                .orElseGet(Collections::emptyList).stream()
                .filter(DatasetVersion::isReleased)
                .map(DatasetVersion::getFileMetadatas)
                .filter(f -> f.size() <= fileIdSet.size())
                .map(this::mapFileMetadataToIds)
                .collect(toList());
    }

    private Set<Long> mapFileMetadataToIds(List<FileMetadata> filesMetadata) {
        return filesMetadata.stream()
                .map(FileMetadata::getDataFile)
                .map(DataFile::getId)
                .collect(toSet());
    }

    private String createMetadataInfo(List<FileMetadata> filesMetadata) {
        return filesMetadata.stream()
                .map(m -> Optional.ofNullable(m)
                        .map(FileMetadata::getDataFile)
                        .map(DvObject::toString)
                        .orElse(StringUtils.EMPTY))
                .collect(Collectors.joining(";"));
    }
}
