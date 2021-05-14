package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class DatasetReportService {

    private static final Logger logger = LoggerFactory.getLogger(DatasetReportService.class);

    private DatasetDao datasetDao;

    private GuestbookResponseServiceBean guestbookResponseService;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // -------------------- CONSTRUCTORS --------------------

    public DatasetReportService() { }

    @Inject
    public DatasetReportService(DatasetDao datasetDao, GuestbookResponseServiceBean guestbookResponseService) {
        this.datasetDao = datasetDao;
        this.guestbookResponseService = guestbookResponseService;
    }

    // -------------------- LOGIC --------------------

    @SuperuserRequired
    public void createReport(OutputStream outputStream) {
        try (Writer writer = new OutputStreamWriter(outputStream);
             BufferedWriter streamWriter = new BufferedWriter(writer);
             CSVPrinter csvPrinter = new CSVPrinter(streamWriter, CSVFormat.DEFAULT)) {
            csvPrinter.printRecord(new Record().getHeaders());
            processDatasets(csvPrinter);
        } catch (IOException ioe) {
            logger.warn("Exception during report creation: ", ioe);
        }
    }

    // -------------------- PRIVATE --------------------

    private void processDatasets(CSVPrinter csvPrinter) throws IOException {
        List<Long> datasetIds = datasetDao.findAllLocalDatasetIds();
        for (Long id : datasetIds) {
            Dataset dataset = datasetDao.find(id);
            if (dataset != null) {
                processDataset(dataset, csvPrinter);
            }
        }
    }

    private void processDataset(Dataset dataset, CSVPrinter csvPrinter) throws IOException {
        Record datasetRecord = createDatasetRecord(dataset);
        for (DatasetVersion version : dataset.getVersions()) {
            processDatasetVersion(version, csvPrinter, datasetRecord);
        }
    }

    private Record createDatasetRecord(Dataset dataset) {
        Record datasetRecord = new Record();
        datasetRecord.setDatasetTitle(dataset.getDisplayName());
        datasetRecord.setDatasetId(dataset.getId());
        GlobalId pid = dataset.getGlobalId();
        datasetRecord.setDatasetPID(pid != null ? pid.asString() : StringUtils.EMPTY);
        datasetRecord.setUnderEmbargo(dataset.hasActiveEmbargo());
        datasetRecord.setEmbargoDate(dataset.getEmbargoDate().getOrNull());
        return datasetRecord;
    }

    private void processDatasetVersion(DatasetVersion datasetVersion, CSVPrinter csvPrinter, Record datasetRecord) throws IOException {
        List<FileMetadata> allFilesMetadataSorted = datasetVersion.getAllFilesMetadataSorted();
        Record datasetVersionRecord = createDatasetVersionRecord(datasetVersion, datasetRecord);
        for (FileMetadata fileMetadata : allFilesMetadataSorted) {
            processFile(fileMetadata, csvPrinter, datasetVersionRecord);
        }
    }

    private Record createDatasetVersionRecord(DatasetVersion datasetVersion, Record datasetRecord) {
        Record datasetVersionRecord = new Record(datasetRecord);
        datasetVersionRecord.setDeaccessionData(datasetVersion.isDeaccessioned() ? datasetVersion.getVersionNote() : StringUtils.EMPTY);
        DatasetField depositDate = datasetVersion.getDatasetFieldByTypeName(DatasetFieldConstant.dateOfDeposit).orElse(null);
        datasetVersionRecord.setDepositDate(depositDate != null ? depositDate.getValue() : StringUtils.EMPTY);
        datasetVersionRecord.setVersionNumber(datasetVersion.getFriendlyVersionNumber());
        datasetVersionRecord.setDatasetVersionPublicationDate(datasetVersion.getPublicationDateAsString());
        datasetVersionRecord.setDatasetVersionState(Objects.toString(datasetVersion.getVersionState(), StringUtils.EMPTY));
        Date lastUpdateTime = datasetVersion.getLastUpdateTime();
        datasetVersionRecord.setLastModificationDate(lastUpdateTime != null ? dateFormatter.format(lastUpdateTime) : StringUtils.EMPTY);
        return datasetVersionRecord;
    }

    private void processFile(FileMetadata fileMetadata, CSVPrinter csvPrinter, Record datasetVersionRecord) throws IOException {
        DataFile dataFile = fileMetadata.getDataFile();
        Record fileRecord = createFileRecord(datasetVersionRecord, dataFile);
        csvPrinter.printRecord(fileRecord.getValues());
    }

    private Record createFileRecord(Record datasetVersionRecord, DataFile dataFile) {
        Record fileRecord = new Record(datasetVersionRecord);
        fileRecord.setFileName(dataFile.getDisplayName());
        fileRecord.setFileId(dataFile.getId());
        fileRecord.setChecksum(dataFile.getChecksumValue());
        fileRecord.setChecksumType(Objects.toString(dataFile.getChecksumType(), StringUtils.EMPTY));
        fileRecord.setSize(dataFile.getFilesize());
        Long uncompressedSize = dataFile.getUncompressedSize();
        fileRecord.setSizeDecompressed(uncompressedSize != 0L ? uncompressedSize : null);
        String tags = dataFile.getTags().stream()
                .map(DataFileTag::getTypeLabel)
                .collect(Collectors.joining(" "));
        fileRecord.setTags(tags);
        fileRecord.setDatafilePublicationDate(dataFile.getPublicationDateFormattedYYYYMMDD());
        String license = Optional.ofNullable(dataFile.getFileMetadata())
                .map(FileMetadata::getTermsOfUse)
                .map(FileTermsOfUse::getLicense)
                .map(License::getName).orElse(StringUtils.EMPTY);
        fileRecord.setLicense(license);
        fileRecord.setContentType(dataFile.getContentType());
        fileRecord.setNumberOfDownloads(guestbookResponseService.getCountGuestbookResponsesByDataFileId(dataFile.getId()));
        return fileRecord;
    }

    /**
     * This enum establishes the columns of CSV report and their order
     */
    enum FileDataField {
        FILE_ID("File ID"),
        FILE_NAME("File name"),
        CONTENT_TYPE("Content type"),
        CHECKSUM("Checksum"),
        CHECKSUM_TYPE("Checksum type"),
        DEPOSIT_DATE("Deposit date"),
        DATAFILE_PUBLICATION_DATE("Datafile publication date"),
        LICENSE("License"),
        TAGS("Tags"),
        NUMBER_OF_DOWNLOADS("Number of downloads"),
        SIZE("Size (bytes)"),
        SIZE_DECOMPRESSED("Size decompressed (bytes)"),
        DATASET_ID("Dataset ID"),
        DATASET_PID("Dataset PID"),
        DATASET_TITLE("Dataset title"),
        VERSION_NUMBER("Version No."),
        DATASET_VERSION_PUBLICATION_DATE("Dataset version publication date"),
        DATASET_VERSION_STATE("Dataset version state"),
        LAST_MODIFICATION_DATE("Last modification date"),
        DEACCESSION_DATA("Deaccession data"),
        UNDER_EMBARGO("Under embargo"),
        EMBARGO_DATE("Embargo date");

        private String displayName;

        // -------------------- CONSTRUCTORS --------------------

        FileDataField(String displayName) {
            this.displayName = displayName;
        }

        // -------------------- GETTERS --------------------

        public String getDisplayName() {
            return displayName;
        }
    }

    // Unfortunately this class cannot be private or package-private
    // as it causes IllegalAccessException during tests (probably
    // because of some classloader issues or maybe a bug in Weld)
    protected static class Record {
        private Map<FileDataField, Object> data;

        private static FileDataField[] KEYS = FileDataField.values();
        private static int FULL_RECORD_SIZE = KEYS.length;

        // -------------------- CONSTRUCTORS --------------------

        public Record() {
            this.data = new EnumMap<>(FileDataField.class);
        }

        public Record(Record other) {
            this.data = new EnumMap<>(other.data);
        }

        // -------------------- LOGIC --------------------

        /**
         * @return list of report headers in the same order as they are
         * declared in {@link FileDataField} enum (such order is guaranteed
         * by underlying {@link EnumMap}).
         */
        public Collection<Object> getHeaders() {
            return Arrays.stream(KEYS)
                    .map(FileDataField::getDisplayName)
                    .collect(Collectors.toList());
        }

        /**
         * @return record values in the same order as they are declared
         * in {@link FileDataField} enum (such order is guaranteed by
         * underlying {@link EnumMap}).
         */
        public Collection<Object> getValues() {
            return data.size() == FULL_RECORD_SIZE
                    ? data.values()
                    : Arrays.stream(KEYS)
                    .map(data::get)
                    .collect(Collectors.toList());
        }

        public void setFileName(String fileName) {
            data.put(FileDataField.FILE_NAME, fileName);
        }

        public void setFileId(Long fileId) {
            data.put(FileDataField.FILE_ID, fileId);
        }

        public void setChecksum(String checksum) {
            data.put(FileDataField.CHECKSUM, checksum);
        }

        public void setChecksumType(String checksumType) {
            data.put(FileDataField.CHECKSUM_TYPE, checksumType);
        }

        public void setDatasetTitle(String datasetTitle) {
            data.put(FileDataField.DATASET_TITLE, datasetTitle);
        }

        public void setDatasetId(Long datasetId) {
            data.put(FileDataField.DATASET_ID, datasetId);
        }

        public void setDatasetPID(String datasetPID) {
            data.put(FileDataField.DATASET_PID, datasetPID);
        }

        public void setUnderEmbargo(Boolean underEmbargo) {
            data.put(FileDataField.UNDER_EMBARGO, underEmbargo);
        }

        public void setEmbargoDate(Date embargoDate) {
            data.put(FileDataField.EMBARGO_DATE, embargoDate);
        }

        public void setLastModificationDate(String lastModificationDate) {
            data.put(FileDataField.LAST_MODIFICATION_DATE, lastModificationDate);
        }
        public void setDeaccessionData(String deaccessionData) {
            data.put(FileDataField.DEACCESSION_DATA, deaccessionData);
        }

        public void setDepositDate(String depositDate) {
            data.put(FileDataField.DEPOSIT_DATE, depositDate);
        }

        public void setSize(Long fileSize) {
            data.put(FileDataField.SIZE, fileSize);
        }

        public void setVersionNumber(String versionNumber) {
            data.put(FileDataField.VERSION_NUMBER, versionNumber);
        }

        public void setTags(String tags) {
            data.put(FileDataField.TAGS, tags);
        }

        public void setDatafilePublicationDate(String dataFilePublicationDate) {
            data.put(FileDataField.DATAFILE_PUBLICATION_DATE, dataFilePublicationDate);
        }

        public void setDatasetVersionPublicationDate(String publicationDate) {
            data.put(FileDataField.DATASET_VERSION_PUBLICATION_DATE, publicationDate);
        }

        public void setLicense(String license) {
            data.put(FileDataField.LICENSE, license);
        }

        public void setContentType(String contentType) {
            data.put(FileDataField.CONTENT_TYPE, contentType);
        }

        public void setSizeDecompressed(Long sizeDecompressed) {
            data.put(FileDataField.SIZE_DECOMPRESSED, sizeDecompressed);
        }

        public void setNumberOfDownloads(Long numberOfDownloads) {
            data.put(FileDataField.NUMBER_OF_DOWNLOADS, numberOfDownloads);
        }

        public void setDatasetVersionState(String versionState) {
            data.put(FileDataField.DATASET_VERSION_STATE, versionState);
        }
    }
}