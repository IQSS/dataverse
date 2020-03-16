package edu.harvard.iq.dataverse.datafile.page;

import com.rometools.utils.Lists;
import edu.harvard.iq.dataverse.dataset.DownloadDatasetLogService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Stateless
public class WholeDatasetDownloadLogger {
    private static final Logger logger = Logger.getLogger(WholeDatasetDownloadLogger.class.getSimpleName());

    private DownloadDatasetLogService downloadDatasetLogService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public WholeDatasetDownloadLogger() { }

    @Inject
    public WholeDatasetDownloadLogger(DownloadDatasetLogService downloadDatasetLogService) {
        this.downloadDatasetLogService = downloadDatasetLogService;
    }

    // -------------------- LOGIC --------------------

    public void incrementLogIfDownloadingWholeDataset(List<DataFile> filesToDownload) {
        if (Lists.isEmpty(filesToDownload)) {
            logger.warning("Empty or null file metadata list.");
            return;
        }

        long startTime = System.currentTimeMillis();
        Map<Dataset, List<DataFile>> filesByDataset = groupFilesByDatasetsAndUpdateLogIfNeeded(filesToDownload);
        long totalTime = System.currentTimeMillis() - startTime;

        logger.log(Level.FINE, "Checking for whole dataset download in datasets [{0}] took: {1} ms",
                new Object[] { extractDatasetIds(filesByDataset), totalTime });
    }

    // -------------------- PRIVATE --------------------

    private Map<Dataset, List<DataFile>> groupFilesByDatasetsAndUpdateLogIfNeeded(List<DataFile> filesToDownload) {
        Map<Dataset, List<DataFile>> filesByDataset = filesToDownload.stream()
                .filter(f -> f != null && f.getOwner() != null)
                .collect(Collectors.groupingBy(DataFile::getOwner));

        filesByDataset.forEach(this::updateLogOnWholeDatasetDownload);

        return filesByDataset;
    }

    private void updateLogOnWholeDatasetDownload(Dataset dataset, List<DataFile> dataFiles) {
        if (isUserDownloadingAllFilesFromAnyVersion(dataset, dataFiles)) {
            downloadDatasetLogService.logWholeSetDownload(dataset.getId());
        }
    }

    /**
     * Here we are extracting sets of ids of files from all the versions that have no more files than
     * list of files being downloaded and we check if any of these sets is wholly contained in the set of ids
     * of files being downloaded
     */
    private boolean isUserDownloadingAllFilesFromAnyVersion(Dataset dataset, List<DataFile> dataFiles) {
        Set<Long> fileIdSet = extractIdsOfFiles(dataFiles);
        List<Set<Long>> fileIdSetsInActiveVersions = createPossibleSetsOfFileIdsFromVersions(dataset, fileIdSet);

        return fileIdSetsInActiveVersions.stream()
                .anyMatch(fileIdSet::containsAll);
    }

    private Set<Long> extractIdsOfFiles(List<DataFile> dataFiles) {
        return dataFiles.stream()
                .map(DataFile::getId)
                .collect(toSet());
    }

    private List<Set<Long>> createPossibleSetsOfFileIdsFromVersions(Dataset dataset, Set<Long> fileIdSet) {
        return Optional.ofNullable(dataset)
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

    private List<Long> extractDatasetIds(Map<Dataset, List<DataFile>> filesByDataset) {
        return filesByDataset.keySet().stream()
                .map(Dataset::getId)
                .collect(toList());
    }
}
