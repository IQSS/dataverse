package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIOConstants;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.DataTableRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadataRepository;
import edu.harvard.iq.dataverse.persistence.datafile.MapLayerMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Stateless
public class UningestService {
    private static final Logger logger = LoggerFactory.getLogger(UningestService.class);

    private DataAccess dataAccess;
    private DataTableRepository dataTableRepository;
    private DataFileRepository dataFileRepository;
    private FileMetadataRepository fileMetadataRepository;
    private MapLayerMetadataServiceBean mapLayerMetadataService;
    private DatasetVersionRepository datasetVersionRepository;
    private DatasetVersionServiceBean datasetVersionService;

    // -------------------- CONSTRUCTORS --------------------

    public UningestService() {
        this.dataAccess = DataAccess.dataAccess();
    }

    @Inject
    public UningestService(DataTableRepository dataTableRepository, DataFileRepository dataFileRepository,
                           FileMetadataRepository fileMetadataRepository, MapLayerMetadataServiceBean mapLayerMetadataService,
                           DatasetVersionRepository datasetVersionRepository, DatasetVersionServiceBean datasetVersionService) {
        this();
        this.mapLayerMetadataService = mapLayerMetadataService;
        this.datasetVersionRepository = datasetVersionRepository;
        this.dataTableRepository = dataTableRepository;
        this.dataFileRepository = dataFileRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.datasetVersionService = datasetVersionService;
    }

    // -------------------- LOGIC --------------------

    public void uningest(DataFile file, AuthenticatedUser user) {
        if (file == null || user == null) {
            throw new IllegalArgumentException("File and user parameters cannot be null: "
                    + String.format("file: [%s], user: [%s]", file, user));
        }
        DataFile uningested = uningestFile(file, user);
        recalculateUNFForCurrentVersion(uningested);
    }

    // -------------------- PRIVATE --------------------

    private DataFile uningestFile(DataFile file, AuthenticatedUser user) {
        // File to uningest cannot be already present in any published version
        if (file.getFileMetadatas().size() > 1 || !file.getFileMetadata().getDatasetVersion().isDraft()) {
            throw new IllegalArgumentException("Only fresh files from draft can be uningested!");
        }
        // Variable file is updated in order to avoid OptimisticLockException
        return file.isTabularData()
                ? uningestTabularFile(file, user)
                : uningestFileWithoutDataTable(file, user);
    }

    private DataFile uningestTabularFile(DataFile file, AuthenticatedUser user) {
        long originalSize;
        StorageIO<DataFile> storage;
        try {
            storage = dataAccess.getStorageIO(file);
            storage.open();
            originalSize = storage.getAuxObjectSize(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);
        } catch (IOException ioe) {
            logger.warn("Uningest problem, rethrowing: ", ioe);
            throw new RuntimeException(ioe);
        }

        // Restore size & format
        file.setFilesize(originalSize);
        String originalFormat = file.getDataTable().getOriginalFileFormat();
        file.setContentType(originalFormat);

        // Remove data table
        DataTable dataTable = file.getDataTable();
        dataTableRepository.deleteById(dataTable.getId());
        file.setDataTable(null);

        // Remove tags (only properly ingested file can have these tags)
        file.setTags(null);

        // Remove ingest reports, tags, status; finally merge
        // Variable is updated in order to avoid OptimisticLockException
        file = uningestFileWithoutDataTable(file, user);

        // Restore original file name
        // As we're only uningesting files that are not published yet, it's sufficient
        // to operate only on the newest metadata (ie. from draft)
        FileMetadata fileMetadata = file.getFileMetadata();
        String filename = fileMetadata.getLabel();
        String extensionToRemove = StringUtil.substringIncludingLast(filename, ".");
        if (StringUtils.isNotBlank(extensionToRemove)) {
            fileMetadata.setLabel(filename.replace(extensionToRemove, FileUtil.generateOriginalExtension(originalFormat)));
            fileMetadataRepository.save(fileMetadata);
        }

        // Finally revert original file & delete the files produced by ingest
        try {
            storage.revertBackupAsAux(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);
            storage.deleteAllAuxObjects();
        } catch (IOException ioe) {
            logger.warn("Uningest problem, rethrowing: ", ioe);
            throw new RuntimeException(ioe);
        }

        return file;
    }

    private DataFile uningestFileWithoutDataTable(DataFile file, AuthenticatedUser user) {
        // Remove ingest reports and set status
        Optional.ofNullable(file.getIngestReports()).ifPresent(List::clear);
        file.setIngestStatus(DataFile.INGEST_STATUS_NONE);

        // Merge entity
        file = dataFileRepository.save(file);

        // Remove world map data
        return removeWorldMapData(file, user);
    }

    private DataFile removeWorldMapData(DataFile file, AuthenticatedUser user) {
        MapLayerMetadata mapLayerMetadata = mapLayerMetadataService.findMetadataByDatafile(file);
        if (mapLayerMetadata == null || user == null) {
            return file;
        }
        try {
            mapLayerMetadataService.deleteMapLayerFromWorldMap(file, user);
        } catch (IOException e) {
            logger.warn("Unable to delete WorldMap file – may not have existed. Data File id: " + file.getId());
        }
        mapLayerMetadataService.deleteMapLayerMetadataObject(mapLayerMetadata, user);

        return dataFileRepository.save(file);
    }

    private void recalculateUNFForCurrentVersion(DataFile file) {
        DatasetVersion version = file.getFileMetadata().getDatasetVersion();
        version.setUNF(null);
        datasetVersionRepository.save(version);
        datasetVersionService.fixMissingUnf(version.getId().toString(), true);
    }

    // -------------------- SETTERS --------------------

    void setDataAccess(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }
}

