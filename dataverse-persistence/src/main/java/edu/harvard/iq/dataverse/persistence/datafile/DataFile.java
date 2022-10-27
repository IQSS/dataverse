package edu.harvard.iq.dataverse.persistence.datafile;

import com.google.gson.annotations.Expose;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.FileSizeUtil;
import edu.harvard.iq.dataverse.common.FriendlyFileTypeUtil;
import edu.harvard.iq.dataverse.common.files.mime.PackageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.ShapefileMimeType;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestReport;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author gdurand
 */
@NamedQueries({
        @NamedQuery(name = "DataFile.removeFromDatasetVersion",
                query = "DELETE FROM FileMetadata f WHERE f.datasetVersion.id=:versionId and f.dataFile.id=:fileId"),
        @NamedQuery(name = "DataFile.findDataFileByIdProtocolAuth",
                query = "SELECT s FROM DataFile s WHERE s.identifier=:identifier AND s.protocol=:protocol AND s.authority=:authority")
})
@NamedNativeQueries({
        @NamedNativeQuery(name = "Datafile.findDataForSolrResults2", query =
                DataFile.FIND_DATA_FOR_SOLR_RESULTS_QUERY_BASE + "WHERE t0.ID IN (?, ?)"),
        @NamedNativeQuery(name = "Datafile.findDataForSolrResults6", query =
                DataFile.FIND_DATA_FOR_SOLR_RESULTS_QUERY_BASE + "WHERE t0.ID IN (?, ?, ?, ?, ?, ?)"),
        @NamedNativeQuery(name = "Datafile.findDataForSolrResults10", query =
                DataFile.FIND_DATA_FOR_SOLR_RESULTS_QUERY_BASE + "WHERE t0.ID IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
})
@Entity
@Table(indexes = {@Index(columnList = "ingeststatus"),
        @Index(columnList = "checksumvalue"),
        @Index(columnList = "contenttype")
})
public class DataFile extends DvObject implements Comparable<DataFile> {
    private static final long serialVersionUID = 1L;

    public static final String TARGET_URL = "/file.xhtml?persistentId=";
    public static final char INGEST_STATUS_NONE = 'A';
    public static final char INGEST_STATUS_SCHEDULED = 'B';
    public static final char INGEST_STATUS_INPROGRESS = 'C';
    public static final char INGEST_STATUS_ERROR = 'D';

    public static final Long ROOT_DATAFILE_ID_DEFAULT = -1L;


    static final String FIND_DATA_FOR_SOLR_RESULTS_QUERY_BASE =
            "SELECT t0.ID, t0.CREATEDATE, " +
            "t0.PUBLICATIONDATE, t0.PREVIEWIMAGEAVAILABLE, t0.STORAGEIDENTIFIER, t0.AUTHORITY, t0.PROTOCOL, t0.IDENTIFIER, " +
            "t1.CONTENTTYPE, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, t1.CHECKSUMTYPE, " +
            "t2.ID, t2.IDENTIFIER, t2.AUTHORITY, t2.STORAGEIDENTIFIER, t3.ID, t3.UNF, t3.CASEQUANTITY, t3.VARQUANTITY " +
            "FROM DVOBJECT t0 JOIN DATAFILE t1 ON t0.ID = t1.ID JOIN DVOBJECT t2 ON t0.OWNER_ID = t2.ID " +
            "LEFT JOIN DATATABLE t3 ON t3.DATAFILE_ID = t0.ID ";

    @Expose
    @NotBlank
    @Column(nullable = false)
    @Pattern(regexp = "^.*/.*$", message = "{contenttype.slash}")
    private String contentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChecksumType checksumType;

    /**
     * Examples include "f622da34d54bdc8ee541d6916ac1c16f" as an MD5 value or
     * "3a484dfdb1b429c2e15eb2a735f1f5e4d5b04ec6" as a SHA-1 value"
     */
    @Column(nullable = false)
    private String checksumValue;

    // start: FILE REPLACE ATTRIBUTES

    /** For the initial version of a file, this will be equivalent to the ID
     * Default is -1 until the intial id is generated
     */
    @Expose
    @Column(nullable = false)
    private Long rootDataFileId;

    /** null for initial version; subsequent versions will point to the previous file */
    @Expose
    @Column
    private Long previousDataFileId;
    // end: FILE REPLACE ATTRIBUTES

    @Expose
    @Column
    private Long filesize; // Number of bytes in file. Allows 0 and null, negative numbers not permitted

    @Expose
    @Column(columnDefinition = "TEXT", nullable = true, name = "prov_entityname")
    private String provEntityName;

    //    Tabular (formerly "subsettable") data files have DataTable objects
    //    associated with them:

    @OneToOne(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @BatchFetch(BatchFetchType.JOIN)
    private DataTable dataTable;

    @OneToMany(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},
            orphanRemoval = true)
    private List<IngestReport> ingestReports;

    @OneToOne(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @BatchFetch(BatchFetchType.JOIN)
    private IngestRequest ingestRequest;

    @OneToMany(mappedBy = "dataFile", orphanRemoval = true,
            cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataFileTag> dataFileTags = new ArrayList<>();

    @OneToMany(mappedBy = "dataFile", orphanRemoval = true,
            cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<FileMetadata> fileMetadatas;

    @OneToMany(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<GuestbookResponse> guestbookResponses;

    @OneToOne(mappedBy = "thumbnailFile")
    @BatchFetch(BatchFetchType.JOIN)
    private Dataset thumbnailForDataset;

    @ManyToMany
    @JoinTable(name = "fileaccessrequests",
            joinColumns = @JoinColumn(name = "datafile_id"),
            inverseJoinColumns = @JoinColumn(name = "authenticated_user_id"))
    private List<AuthenticatedUser> fileAccessRequesters;

    @Column
    private Long uncompressedSize = 0L;

    /**
     * This flag has only meaning for ingestable files and when unset prevents
     * the file from being ingested.
     */
    @Transient
    private Boolean includedInIngest = Boolean.TRUE;

    private char ingestStatus = INGEST_STATUS_NONE;

    // -------------------- CONSTRUCTORS --------------------

    public DataFile() {
        this.fileMetadatas = new ArrayList<>();
        initFileReplaceAttributes();
    }

    public DataFile(String contentType) {
        this();
        this.contentType = contentType;
    }

    // -------------------- GETTERS --------------------

    public List<GuestbookResponse> getGuestbookResponses() {
        return guestbookResponses;
    }

    public DataTable getDataTable() {
        return dataTable;
    }

    public List<DataFileTag> getTags() {
        return dataFileTags;
    }

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public IngestRequest getIngestRequest() {
        return ingestRequest;
    }

    public String getContentType() {
        return contentType;
    }

    public ChecksumType getChecksumType() {
        return checksumType;
    }

    public String getChecksumValue() {
        return this.checksumValue;
    }

    public int getIngestStatus() {
        return ingestStatus;
    }

    public Boolean getIncludedInIngest() {
        return includedInIngest;
    }

    public Dataset getThumbnailForDataset() {
        return thumbnailForDataset;
    }

    public List<AuthenticatedUser> getFileAccessRequesters() {
        return fileAccessRequesters;
    }

    public String getProvEntityName() {
        return provEntityName;
    }

    public Long getRootDataFileId() {
        return rootDataFileId;
    }

    public Long getPreviousDataFileId() {
        return previousDataFileId;
    }

    public Long getUncompressedSize() {
        return uncompressedSize;
    }

    public List<IngestReport> getIngestReports() {
        return ingestReports;
    }

    @Override
    public boolean isEffectivelyPermissionRoot() {
        return false;
    }

    @Override
    public Dataset getOwner() {
        return (Dataset) super.getOwner();
    }

    // -------------------- LOGIC --------------------

    public List<String> getTagLabels() {
        List<DataFileTag> currentDataTags = this.getTags();
        List<String> tagStrings = new ArrayList<>();

        if (currentDataTags != null && !currentDataTags.isEmpty()) {
            for (DataFileTag element : currentDataTags) {
                tagStrings.add(element.getTypeLabel());
            }
        }
        return tagStrings;
    }

    public void addTag(DataFileTag tag) {
        dataFileTags.add(tag);
    }

    public IngestReport getIngestReport() {
        return ingestReports != null && !ingestReports.isEmpty()
                ? ingestReports.get(0) : null;
    }

    public void setIngestReport(IngestReport report) {
        if (ingestReports == null) {
            ingestReports = new ArrayList<>();
        } else {
            ingestReports.clear();
        }
        ingestReports.add(report);
    }

    public boolean isTabularData() {
        return dataTable != null;
    }

    public String getOriginalFileFormat() {
        return isTabularData() && getDataTable() != null
                ? getDataTable().getOriginalFileFormat() : null;
    }

    public Long getOriginalFileSize() {
        return isTabularData() && getDataTable() != null
                ? getDataTable().getOriginalFileSize() : null;
    }

    @Override
    public boolean isAncestorOf(DvObject other) {
        return equals(other);
    }

    /**
     * A user-friendly version of the "original format":
     */
    public String getOriginalFormatLabel() {
        return FriendlyFileTypeUtil.getUserFriendlyOriginalType(this);
    }

    public String getFriendlyType() {
        return FriendlyFileTypeUtil.getUserFriendlyFileType(this);
    }

    public String getFriendlyTypeForIndex(Locale locale) {
        return FriendlyFileTypeUtil.getUserFriendlyFileType(this, locale);
    }

    public String getDescription() {
        FileMetadata fmd = getLatestFileMetadata();
        return fmd != null ? fmd.getDescription() : null;
    }

    public void setDescription(String description) {
        FileMetadata fmd = getLatestFileMetadata();
        if (fmd != null) {
            fmd.setDescription(description);
        }
    }

    public FileMetadata getFileMetadata() {
        return getLatestFileMetadata();
    }

    public FileMetadata getLatestFileMetadata() {
        FileMetadata fmd = null;

        // for newly added or harvested, just return the one fmd
        if (fileMetadatas.size() == 1) {
            return fileMetadatas.get(0);
        }

        for (FileMetadata fileMetadata : fileMetadatas) {
            // if it finds a draft, return it
            if (fileMetadata.getDatasetVersion().getVersionState().equals(VersionState.DRAFT)) {
                return fileMetadata;
            }

            // otherwise return the one with the latest version number
            if (fmd == null
                    || fileMetadata.getDatasetVersion().getVersionNumber().compareTo(fmd.getDatasetVersion().getVersionNumber()) > 0
                    || (fileMetadata.getDatasetVersion().getVersionNumber().compareTo(fmd.getDatasetVersion().getVersionNumber()) == 0 &&
                        fileMetadata.getDatasetVersion().getMinorVersionNumber().compareTo(fmd.getDatasetVersion().getMinorVersionNumber()) > 0)) {
                fmd = fileMetadata;
            }
        }
        return fmd;
    }

    public long getFilesize() {
        // -1 means "unknown"
        return this.filesize != null ? this.filesize : -1;
    }

    public void setFilesize(long filesize) {
        if (filesize < 0) {
            return;
        }
        this.filesize = filesize;
    }

    /**
     * Converts the stored size of the file in bytes to
     * a user-friendly value in KB, MB or GB.
     */
    public String getFriendlySize() {
        return FileSizeUtil.bytesToHumanReadable(filesize);
    }

    public String getOriginalChecksumType() {
        return BundleUtil.getStringFromBundle("file.originalChecksumType", this.checksumType.toString());
    }

    // Does the contentType indicate a shapefile?
    public boolean isShapefileType() {
        return ShapefileMimeType.SHAPEFILE_FILE_TYPE.getMimeValue().equalsIgnoreCase(this.contentType);
    }

    public boolean isImage() {
        // Some browsers (Chrome?) seem to identify FITS files as mime
        // type "image/fits" on upload; this is both incorrect (the official
        // mime type for FITS is "application/fits", and problematic: then
        // the file is identified as an image, and the page will attempt to
        // generate a preview - which of course is going to fail...
        if ("image/fits".equalsIgnoreCase(contentType)) {
            return false;
        }
        // a pdf file is an "image" for practical purposes (we will attempt to
        // generate thumbnails and previews for them)
        return contentType != null
                && (contentType.startsWith("image/") || "application/pdf".equalsIgnoreCase(contentType));
    }

    public boolean isFilePackage() {
        return PackageMimeType.DATAVERSE_PACKAGE.getMimeValue().equalsIgnoreCase(contentType);
    }

    public boolean isIngestScheduled() {
        return ingestStatus == INGEST_STATUS_SCHEDULED;
    }

    public boolean isIngestInProgress() {
        return ingestStatus == INGEST_STATUS_SCHEDULED || ingestStatus == INGEST_STATUS_INPROGRESS;
    }

    public boolean isIngestProblem() {
        return ingestStatus == INGEST_STATUS_ERROR;
    }

    public void setIngestScheduled() {
        ingestStatus = INGEST_STATUS_SCHEDULED;
    }

    public void setIngestInProgress() {
        ingestStatus = INGEST_STATUS_INPROGRESS;
    }

    public void setIngestProblem() {
        ingestStatus = INGEST_STATUS_ERROR;
    }

    public void setIngestDone() {
        ingestStatus = INGEST_STATUS_NONE;
    }

    // 8/10/2014 - Using the current "open access" url
    public String getMapItFileDownloadURL(String serverName) {
        return this.getId() != null && serverName != null
                ? serverName + "/api/access/datafile/" + this.getId() : null;
    }

    /*
     * If this is tabular data, the corresponding dataTable may have a UNF -
     * "numeric fingerprint" signature - generated:
     */
    public String getUnf() {
        // isTabularData() method above verifies that that this file
        // has a datDatable associated with it, so the line below is
        // safe, in terms of a NullPointerException:
        return this.isTabularData() ? this.getDataTable().getUnf() : null;
    }

    public boolean isHarvested() {
        // (storageIdentifier is not nullable - so no need to check for null
        // pointers below):
        if (getStorageIdentifier().startsWith("http://") || this.getStorageIdentifier().startsWith("https://")) {
            return true;
        }

        Dataset ownerDataset = this.getOwner();
        return ownerDataset != null && ownerDataset.isHarvested();
    }

    public String getRemoteArchiveURL() {
        return isHarvested() ? this.getOwner().getRemoteArchiveURL() : null;
    }

    @Override
    protected String toStringExtras() {
        FileMetadata fmd = getLatestFileMetadata();
        return "label:" + (fmd != null ? fmd.getLabel() : "[no metadata]");
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    @Override
    public String getDisplayName() {
        return getLatestFileMetadata().getLabel();
    }

    @Override
    public int compareTo(DataFile other) {
        return this.getDisplayName().toUpperCase().compareTo(other.getDisplayName().toUpperCase());
    }

    /**
     * Check if the Geospatial Tag has been assigned to this file
     */
    public boolean hasGeospatialTag() {
        return this.dataFileTags.stream()
                .anyMatch(DataFileTag::isGeospatialTag);
    }

    public String getPublicationDateFormattedYYYYMMDD() {
        return getPublicationDate() != null
                ? new SimpleDateFormat("yyyy-MM-dd").format(getPublicationDate()) : null;
    }

    public String getCreateDateFormattedYYYYMMDD() {
        return getCreateDate() != null
                ? new SimpleDateFormat("yyyy-MM-dd").format(getCreateDate()) : null;
    }

    // -------------------- PRIVATE --------------------

    /**
     * All constructors should use this method
     * to initialize this file replace attributes
     */
    private void initFileReplaceAttributes() {
        this.rootDataFileId = ROOT_DATAFILE_ID_DEFAULT;
        this.previousDataFileId = null;
    }

    // -------------------- SETTERS --------------------

    public void setRootDataFileId(Long rootDataFileId) {
        this.rootDataFileId = rootDataFileId;
    }

    public void setPreviousDataFileId(Long previousDataFileId) {
        this.previousDataFileId = previousDataFileId;
    }

    public void setUncompressedSize(Long uncompressedSize) {
        this.uncompressedSize = uncompressedSize;
    }

    public void setDataTable(DataTable dataTable) {
        this.dataTable = dataTable;
    }

    public void setTags(List<DataFileTag> dataFileTags) {
        this.dataFileTags = dataFileTags;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public void setIngestRequest(IngestRequest ingestRequest) {
        this.ingestRequest = ingestRequest;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setOwner(Dataset dataset) {
        super.setOwner(dataset);
    }

    public void setChecksumType(ChecksumType checksumType) {
        this.checksumType = checksumType;
    }

    public void setChecksumValue(String checksumValue) {
        this.checksumValue = checksumValue;
    }

    public void setIngestStatus(char ingestStatus) {
        this.ingestStatus = ingestStatus;
    }

    public void setAsThumbnailForDataset(Dataset dataset) {
        this.thumbnailForDataset = dataset;
    }

    public void setIncludedInIngest(Boolean includedInIngest) {
        this.includedInIngest = includedInIngest;
    }

    public void setFileAccessRequesters(List<AuthenticatedUser> fileAccessRequesters) {
        this.fileAccessRequesters = fileAccessRequesters;
    }

    public void setProvEntityName(String name) {
        this.provEntityName = name;
    }

    public void setGuestbookResponses(List<GuestbookResponse> guestbookResponses) {
        this.guestbookResponses = guestbookResponses;
    }

    // -------------------- equals & hashCode --------------------

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataFile)) {
            return false;
        }
        DataFile other = (DataFile) object;
        return Objects.equals(getId(), other.getId());
    }

    // -------------------- INNER CLASSES --------------------

    /**
     * End users will see "SHA-1" (with a hyphen) rather than "SHA1" in the GUI
     * and API but in the "datafile" table we persist "SHA1" (no hyphen) for
     * type safety (using keys of the enum). In the "setting" table, we persist
     * "SHA-1" (with a hyphen) to match the GUI and the "Algorithm Name" list at
     * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
     * <p>
     * The list of types should be limited to the list above in the technote
     * because the string gets passed into MessageDigest.getInstance() and you
     * can't just pass in any old string.
     */
    public enum ChecksumType {

        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");

        private final String text;

        ChecksumType(final String text) {
            this.text = text;
        }

        public static ChecksumType fromString(String text) {
            if (text != null) {
                for (ChecksumType checksumType : ChecksumType.values()) {
                    if (text.equals(checksumType.text)) {
                        return checksumType;
                    }
                }
            }
            throw new IllegalArgumentException("ChecksumType must be one of these values: "
                    + Arrays.asList(ChecksumType.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
    }
}