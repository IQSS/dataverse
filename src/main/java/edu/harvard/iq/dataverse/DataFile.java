package edu.harvard.iq.dataverse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datasetutility.FileSizeChecker;
import edu.harvard.iq.dataverse.ingest.IngestReport;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author gdurand
 */
@NamedQueries({
	@NamedQuery( name="DataFile.removeFromDatasetVersion",
		query="DELETE FROM FileMetadata f WHERE f.datasetVersion.id=:versionId and f.dataFile.id=:fileId"),
        @NamedQuery(name = "DataFile.findByCreatorId",
                query = "SELECT o FROM DataFile o WHERE o.creator.id=:creatorId"),
        @NamedQuery(name = "DataFile.findByReleaseUserId",
                query = "SELECT o FROM DataFile o WHERE o.releaseUser.id=:releaseUserId"),
        @NamedQuery(name="DataFile.findDataFileByIdProtocolAuth",
                query="SELECT s FROM DataFile s WHERE s.identifier=:identifier AND s.protocol=:protocol AND s.authority=:authority"),
        @NamedQuery(name="DataFile.findDataFileThatReplacedId",
                query="SELECT s.id FROM DataFile s WHERE s.previousDataFileId=:identifier")
})
@NamedNativeQuery(
        name = "DataFile.getDataFileInfoForPermissionIndexing",
        query = "SELECT fm.label, df.id, dvo.publicationDate " +
                "FROM filemetadata fm " +
                "JOIN datafile df ON fm.datafile_id = df.id " +
                "JOIN dvobject dvo ON df.id = dvo.id " +
                "WHERE fm.datasetversion_id = ?",
        resultSetMapping = "DataFileInfoMapping"
    )
    @SqlResultSetMapping(
        name = "DataFileInfoMapping",
        classes = @ConstructorResult(
            targetClass = SolrIndexServiceBean.DataFileProxy.class,
            columns = {
                @ColumnResult(name = "label", type = String.class),
                @ColumnResult(name = "id", type = Long.class),
                @ColumnResult(name = "publicationDate", type = Date.class)
            }
        )
    )
@Entity
@Table(indexes = {@Index(columnList="ingeststatus")
        , @Index(columnList="checksumvalue")
        , @Index(columnList="contenttype")
        , @Index(columnList="restricted")})
public class DataFile extends DvObject implements Comparable {
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());
    private static final long serialVersionUID = 1L;
    public static final String TARGET_URL = "/file.xhtml?persistentId=";
    public static final char INGEST_STATUS_NONE = 65;
    public static final char INGEST_STATUS_SCHEDULED = 66;
    public static final char INGEST_STATUS_INPROGRESS = 67;
    public static final char INGEST_STATUS_ERROR = 68; 
    
    public static final Long ROOT_DATAFILE_ID_DEFAULT = (long) -1;
    
    @Expose
    @NotBlank
    @Column( nullable = false )
    @Pattern(regexp = "^.*/.*$", message = "{contenttype.slash}")
    private String contentType;

//    @Expose    
//    @SerializedName("storageIdentifier")
//    @Column( nullable = false )
//    private String fileSystemName;

    /**
     * End users will see "SHA-1" (with a hyphen) rather than "SHA1" in the GUI
     * and API but in the "datafile" table we persist "SHA1" (no hyphen) for
     * type safety (using keys of the enum). In the "setting" table, we persist
     * "SHA-1" (with a hyphen) to match the GUI and the "Algorithm Name" list at
     * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
     *
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

        private ChecksumType(final String text) {
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
            throw new IllegalArgumentException("ChecksumType must be one of these values: " + Arrays.asList(ChecksumType.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
    }

    //@Expose
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChecksumType checksumType;

    /**
     * Examples include "f622da34d54bdc8ee541d6916ac1c16f" as an MD5 value or
     * "3a484dfdb1b429c2e15eb2a735f1f5e4d5b04ec6" as a SHA-1 value"
     */
    //@Expose
    @Column(nullable = false)
    private String checksumValue;

    
    /* start: FILE REPLACE ATTRIBUTES */
    
    // For the initial version of a file, this will be equivalent to the ID
    // Default is -1 until the intial id is generated
    @Expose
    @Column(nullable=false)
    private Long rootDataFileId;

    /**
     * @todo We should have consistency between "Id" vs "ID" for rootDataFileId
     * vs. previousDataFileId.
     */
    // null for initial version; subsequent versions will point to the previous file
    //
    @Expose
    @Column(nullable=true)
    private Long previousDataFileId;
    /* endt: FILE REPLACE ATTRIBUTES */
    
    
    
    @Expose
    @Column(nullable=true)
    private Long filesize;      // Number of bytes in file.  Allows 0 and null, negative numbers not permitted

    @Expose
    private boolean restricted;
    
    @Expose
    @Column(columnDefinition = "TEXT", nullable = true, name="prov_entityname")
    private String provEntityName;
    
    /*Add when we integrate with provCPL*/
    //The id given for the datafile by CPL.
//    @Column(name="prov_cplid") //( nullable=false )
//    private int provCplId;
    
    /*
        Tabular (formerly "subsettable") data files have DataTable objects
        associated with them:
    */
    
    @OneToMany(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataTable> dataTables;
    
    @OneToMany(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<AuxiliaryFile> auxiliaryFiles;
   
    @OneToMany(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<IngestReport> ingestReports;
    
    @OneToOne(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private IngestRequest ingestRequest;
    
    @OneToMany(mappedBy = "dataFile", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataFileTag> dataFileTags;
    
    @OneToMany(mappedBy="dataFile", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<FileMetadata> fileMetadatas;
    
    @OneToMany(mappedBy="dataFile", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<GuestbookResponse> guestbookResponses;

    @OneToMany(mappedBy="dataFile",fetch = FetchType.LAZY,cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<FileAccessRequest> fileAccessRequests;

    @ManyToMany
    @JoinTable(name = "fileaccessrequests",
    joinColumns = @JoinColumn(name = "datafile_id"),
    inverseJoinColumns = @JoinColumn(name = "authenticated_user_id"))
    private List<AuthenticatedUser> fileAccessRequesters;

    
    public List<FileAccessRequest> getFileAccessRequests(){
        return fileAccessRequests;
    }
    
    public List<FileAccessRequest> getFileAccessRequests(FileAccessRequest.RequestState state){
        return fileAccessRequests.stream().filter(far -> far.getState() == state).collect(Collectors.toList());
    }

    public void setFileAccessRequests(List<FileAccessRequest> fARs){
        this.fileAccessRequests = fARs;
    }
    
    public List<GuestbookResponse> getGuestbookResponses() {
        return guestbookResponses;
    }

    public void setGuestbookResponses(List<GuestbookResponse> guestbookResponses) {
        this.guestbookResponses = guestbookResponses;
    }

    private char ingestStatus = INGEST_STATUS_NONE; 
    
    @OneToOne(mappedBy = "thumbnailFile")
    private Dataset thumbnailForDataset;

    @ManyToOne
    @JoinColumn(name="embargo_id")
    private Embargo embargo;

    public Embargo getEmbargo() {
        return embargo;
    }

    public void setEmbargo(Embargo embargo) {
        this.embargo = embargo;
    }

    @ManyToOne
    @JoinColumn(name="retention_id")
    private Retention retention;

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    public DataFile() {
        this.fileMetadatas = new ArrayList<>();
        initFileReplaceAttributes();
    }    

    public DataFile(String contentType) {
        this.contentType = contentType;
        this.fileMetadatas = new ArrayList<>();
        initFileReplaceAttributes();
    }

    /*
    Used in manage file permissions UI 
    to easily display those files that have been deleted in the current draft 
    or previous version which may have roles assigned or pending requests for access
    */
   
    @Transient
    private Boolean deleted;

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
    
    /*
    For use during file upload so that the user may delete 
    files that have already been uploaded to the current dataset version
    */
    
    @Transient
    private boolean markedAsDuplicate;

    public boolean isMarkedAsDuplicate() {
        return markedAsDuplicate;
    }

    public void setMarkedAsDuplicate(boolean markedAsDuplicate) {
        this.markedAsDuplicate = markedAsDuplicate;
    }
    
    @Transient
    private String duplicateFilename;

    public String getDuplicateFilename() {
        return duplicateFilename;
    }

    public void setDuplicateFilename(String duplicateFilename) {
        this.duplicateFilename = duplicateFilename;
    }

    public List<AuxiliaryFile> getAuxiliaryFiles() {
        return auxiliaryFiles;
    }

    public void setAuxiliaryFiles(List<AuxiliaryFile> auxiliaryFiles) {
        this.auxiliaryFiles = auxiliaryFiles;
    }
    
    
    
    
       
    /**
     * All constructors should use this method
     * to initialize this file replace attributes
     */
    private void initFileReplaceAttributes(){
        this.rootDataFileId = ROOT_DATAFILE_ID_DEFAULT;
        this.previousDataFileId = null;
    }
    
    @Override
    public boolean isEffectivelyPermissionRoot() {
        return false;
    }
    
    public List<DataTable> getDataTables() {
        return dataTables;
    }

    public void setDataTables(List<DataTable> dataTables) {
        this.dataTables = dataTables;
    }
    
    public DataTable getDataTable() {
        if ( getDataTables() != null && getDataTables().size() > 0 ) {
            return getDataTables().get(0);
        } else {
            return null;
        }
    }

    public void setDataTable(DataTable dt) {
        if (this.getDataTables() == null) {
            this.setDataTables(new ArrayList<>());
        } else {
            this.getDataTables().clear();
        }

        this.getDataTables().add(dt);
    }
    
    public List<DataFileTag> getTags() {
        return dataFileTags;
    }
    
    public List<String> getTagLabels(){
        
        List<DataFileTag> currentDataTags = this.getTags();
        List<String> tagStrings = new ArrayList<>();
        
        if (( currentDataTags != null)&&(!currentDataTags.isEmpty())){
                       
            for (DataFileTag element : currentDataTags) {
                tagStrings.add(element.getTypeLabel());
            }
        }
        return tagStrings;
    }

    public JsonArrayBuilder getTagLabelsAsJsonArrayBuilder(){
        
        List<DataFileTag> currentDataTags = this.getTags();

        JsonArrayBuilder builder = Json.createArrayBuilder();
        
        if ( (currentDataTags == null)||(currentDataTags.isEmpty())){
            return builder;
        }
        
        
        for (DataFileTag element : currentDataTags) {
            builder.add(element.getTypeLabel());            
        }
        return builder;
    }
    public void setTags(List<DataFileTag> dataFileTags) {
        this.dataFileTags = dataFileTags;
    }

    public void addUniqueTagByLabel(String tagLabel) throws IllegalArgumentException {
        if (tagExists(tagLabel)) {
            return;
        }
        DataFileTag tag = new DataFileTag();
        tag.setTypeByLabel(tagLabel);
        tag.setDataFile(this);
        addTag(tag);
    }

    public void addTag(DataFileTag tag) {
        if (dataFileTags == null) {
            dataFileTags = new ArrayList<>();
        } 

        dataFileTags.add(tag);
    }
    
    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }
    
    public IngestReport getIngestReport() {
        if ( ingestReports != null && ingestReports.size() > 0 ) {
            return ingestReports.get(0);
        } else {
            return null;
        }
    }

    public void setIngestReport(IngestReport report) {
        if (ingestReports == null) {
            ingestReports = new ArrayList<>();
        } else {
            ingestReports.clear();
        }

        ingestReports.add(report);
    }
    
    public IngestRequest getIngestRequest() {
        return ingestRequest;
    }
    
    public void setIngestRequest(IngestRequest ingestRequest) {
        this.ingestRequest = ingestRequest;
    }
    
    public String getIngestReportMessage() {
        if ( ingestReports != null && ingestReports.size() > 0 ) {
            if (ingestReports.get(0).getReport() != null && !"".equals(ingestReports.get(0).getReport())) {
                return ingestReports.get(0).getReport();
            }
        }
        return BundleUtil.getStringFromBundle("file.ingestFailed");
    }
    
    public boolean isTabularData() {
        return getDataTables() != null && getDataTables().size() > 0; 
    }
    
    public String getOriginalFileFormat() {
        if (isTabularData()) {
            DataTable dataTable = getDataTable();
            if (dataTable != null) {
                return dataTable.getOriginalFileFormat();
            }
        }
        return null;
    }
    
    public Long getOriginalFileSize() {
        if (isTabularData()) {
            DataTable dataTable = getDataTable();
            if (dataTable != null) {
                return dataTable.getOriginalFileSize();
            }
        }
        return null;
    }
    
    public String getOriginalFileName() {
        if (isTabularData()) {
            DataTable dataTable = getDataTable();
            if (dataTable != null) {
                return dataTable.getOriginalFileName() != null ? dataTable.getOriginalFileName()
                        : getDerivedOriginalFileName();
            }
        }
        return null;
    }

    
    private String getDerivedOriginalFileName() {
        FileMetadata fm = getFileMetadata();
        String filename = fm.getLabel();
        String originalExtension = FileUtil.generateOriginalExtension(getOriginalFileFormat());
        String extensionToRemove = StringUtil.substringIncludingLast(filename, ".");
        if (StringUtil.nonEmpty(extensionToRemove)) {
            return filename.replaceAll(extensionToRemove + "$", originalExtension);
        } else{
            return filename + originalExtension ;
        }        
    }

    @Override
    public boolean isAncestorOf( DvObject other ) {
        return equals(other);
    }
    
    /*
     * A user-friendly version of the "original format":
     */
    public String getOriginalFormatLabel() {
        return FileUtil.getUserFriendlyOriginalType(this);
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFriendlyType() {
        return FileUtil.getUserFriendlyFileType(this);
    }
    
    @Override
    public Dataset getOwner() {
        return (Dataset) super.getOwner();
    }

    public void setOwner(Dataset dataset) {
        super.setOwner(dataset);
    }
    
    public String getDescription() {
        FileMetadata fmd = getLatestFileMetadata();
        
        if (fmd == null) {
            return null;
        }
        return fmd.getDescription();
    }

    public void setDescription(String description) {
        FileMetadata fmd = getLatestFileMetadata();
        
        if (fmd != null) {
            fmd.setDescription(description);
        }
    }

    public FileMetadata getDraftFileMetadata() {
        FileMetadata latestFileMetadata = getLatestFileMetadata();
        if (latestFileMetadata.getDatasetVersion().isDraft()) {
            return latestFileMetadata;
        }
        return null;
    }
    
    public FileMetadata getFileMetadata() {
        return getLatestFileMetadata();
    }

    public FileMetadata getLatestFileMetadata() {
        FileMetadata resultFileMetadata = null;

        if (fileMetadatas.size() == 1) {
            return fileMetadatas.get(0);
        }

        for (FileMetadata fileMetadata : fileMetadatas) {
            if (fileMetadata.getDatasetVersion().getVersionState().equals(VersionState.DRAFT)) {
                return fileMetadata;
            }
            resultFileMetadata = getTheNewerFileMetadata(resultFileMetadata, fileMetadata);
        }

        return resultFileMetadata;
    }

    public FileMetadata getLatestPublishedFileMetadata() throws UnsupportedOperationException {
        FileMetadata resultFileMetadata = fileMetadatas.stream()
                .filter(metadata -> !metadata.getDatasetVersion().getVersionState().equals(VersionState.DRAFT))
                .reduce(null, DataFile::getTheNewerFileMetadata);

        if (resultFileMetadata == null) {
            throw new UnsupportedOperationException("No published metadata version for DataFile " + this.getId());
        }

        return resultFileMetadata;
    }

    public static FileMetadata getTheNewerFileMetadata(FileMetadata current, FileMetadata candidate) {
        if (current == null) {
            return candidate;
        }

        DatasetVersion currentVersion = current.getDatasetVersion();
        DatasetVersion candidateVersion = candidate.getDatasetVersion();

        if (DatasetVersion.compareByVersion.compare(candidateVersion, currentVersion) > 0) {
            return candidate;
        }

        return current;
    }

    /**
     * Get property filesize, number of bytes
     * @return value of property filesize.
     */
    public long getFilesize() {
        if (this.filesize == null) {
            // -1 means "unknown"
            return -1;
        }
        return this.filesize;
    }

    /**
     * Set property filesize in bytes
     * 
     * Allow nulls, but not negative numbers.
     * 
     * @param filesize new value of property filesize.
     */
    public void setFilesize(long filesize) {
        if (filesize < 0){
            return;
        }
       this.filesize = filesize;
    }

    /**
     * Converts the stored size of the file in bytes to 
     * a user-friendly value in KB, MB or GB.
     * @return 
     */
    public String getFriendlySize() {
        if (filesize != null) {
            return FileSizeChecker.bytesToHumanReadable(filesize);
        } else {
            return BundleUtil.getStringFromBundle("file.sizeNotAvailable");
        }
    }
    
    public boolean isRestricted() {
        return restricted;
    }

    
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public ChecksumType getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(ChecksumType checksumType) {
        this.checksumType = checksumType;
    }

    public String getChecksumValue() {
        return this.checksumValue;
    }

    public void setChecksumValue(String checksumValue) {
        this.checksumValue = checksumValue;
    }

    public String getOriginalChecksumType() {
        return BundleUtil.getStringFromBundle("file.originalChecksumType", Arrays.asList(this.checksumType.toString()) );
    }

    public StorageIO<DataFile> getStorageIO() throws IOException {
        StorageIO<DataFile> storageIO = DataAccess.getStorageIO(this);
        
        if (storageIO == null) {
            throw new IOException("Failed to create storageIO for datafile.");
        }
        
        return storageIO; 
    }
    
    /*
        Does the contentType indicate a shapefile?
    */
    public boolean isShapefileType(){
        if (this.contentType==null){
            return false;
        }
        return ShapefileHandler.SHAPEFILE_FILE_TYPE.equalsIgnoreCase(this.contentType);
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
        return (contentType != null && (contentType.startsWith("image/") || contentType.equalsIgnoreCase("application/pdf")));
    }
    
    public boolean isFilePackage() {
        return DataFileServiceBean.MIME_TYPE_PACKAGE_FILE.equalsIgnoreCase(contentType);
    }

    public void setIngestStatus(char ingestStatus) {
        this.ingestStatus = ingestStatus; 
    }    
   
    public boolean isIngestScheduled() {
        return (ingestStatus == INGEST_STATUS_SCHEDULED);
    }
    
    public boolean isIngestInProgress() {
        return ((ingestStatus == INGEST_STATUS_SCHEDULED) || (ingestStatus == INGEST_STATUS_INPROGRESS));
    }
    
    public boolean isIngestProblem() {
        return (ingestStatus == INGEST_STATUS_ERROR);
    }
    
    public void SetIngestScheduled() {
        ingestStatus = INGEST_STATUS_SCHEDULED;
    }
    
    public void SetIngestInProgress() {
        ingestStatus = INGEST_STATUS_INPROGRESS;
    }
    
    public void SetIngestProblem() {
        ingestStatus = INGEST_STATUS_ERROR;
    }
    
    public void setIngestDone() {
        ingestStatus = INGEST_STATUS_NONE;
    }
    
    public int getIngestStatus() {
        return ingestStatus; 
    }
    
    public Dataset getThumbnailForDataset() {
        return thumbnailForDataset;
    }
    
    public void setAsThumbnailForDataset(Dataset dataset) {
        thumbnailForDataset = dataset;
    }

    /*
        8/10/2014 - Using the current "open access" url
    */
    public String getMapItFileDownloadURL(String serverName){
        if ((this.getId() == null)||(serverName == null)){
            return null;
        }
        return serverName + "/api/access/datafile/" + this.getId();
    }
    
    /* 
     * If this is tabular data, the corresponding dataTable may have a UNF -
     * "numeric fingerprint" signature - generated:
     */
    
    public String getUnf() {
        if (this.isTabularData()) {
            // (isTabularData() method above verifies that that this file 
            // has a datDatable associated with it, so the line below is 
            // safe, in terms of a NullPointerException: 
            return this.getDataTable().getUnf();
        }
        return null; 
    }

    public List<AuthenticatedUser> getFileAccessRequesters() {
        return fileAccessRequesters;
    }

    public void setFileAccessRequesters(List<AuthenticatedUser> fileAccessRequesters) {
        this.fileAccessRequesters = fileAccessRequesters;
    }


    public void addFileAccessRequest(FileAccessRequest request) {
        if (this.fileAccessRequests == null) {
            this.fileAccessRequests = new ArrayList<>();
        }

        this.fileAccessRequests.add(request);
    }

    public FileAccessRequest getAccessRequestForAssignee(RoleAssignee roleAssignee) {
        if (this.fileAccessRequests == null) {
            return null;
        }

        return this.fileAccessRequests.stream()
                .filter(fileAccessRequest -> fileAccessRequest.getRequester().equals(roleAssignee) && fileAccessRequest.isStateCreated()).findFirst()
                .orElse(null);
    }

    public boolean removeFileAccessRequest(FileAccessRequest request) {
        if (this.fileAccessRequests == null) {
            return false;
        }

        if (request != null) {
            this.fileAccessRequests.remove(request);
            return true;
        }

        return false;
    }

    public boolean containsActiveFileAccessRequestFromUser(RoleAssignee roleAssignee) {
        if (this.fileAccessRequests == null) {
            return false;
        }

        Set<AuthenticatedUser> existingUsers = getFileAccessRequests(FileAccessRequest.RequestState.CREATED).stream()
            .map(FileAccessRequest::getRequester)
            .collect(Collectors.toSet());

        return existingUsers.contains(roleAssignee);
    }

    public boolean isHarvested() {
        
        Dataset ownerDataset = this.getOwner();
        if (ownerDataset != null) {
            return ownerDataset.isHarvested(); 
        }
        return false; 
    }
    
    public String getRemoteArchiveURL() {
        if (isHarvested()) {
            Dataset ownerDataset = this.getOwner();
            return ownerDataset.getRemoteArchiveURL();
        }
        
        return null; 
    }
    
    public String getHarvestingDescription() {
        if (isHarvested()) {
            Dataset ownerDataset = this.getOwner();
            return ownerDataset.getHarvestingDescription();
        }
        
        return null;
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataFile)) {
            return false;
        }
        DataFile other = (DataFile) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    protected String toStringExtras() {
        FileMetadata fmd = getLatestFileMetadata();
        return "label:" + (fmd!=null? fmd.getLabel() : "[no metadata]");
    }
	
	@Override
	public <T> T accept( Visitor<T> v ) {
		return v.visit(this);
	}
        
    @Override
    public String getDisplayName() {
       return getLatestFileMetadata().getLabel(); 
    }
    
    public String getDirectoryLabel() {
       return getLatestFileMetadata().getDirectoryLabel();
    }
    
    @Override 
    public String getCurrentName(){
        return getLatestFileMetadata().getLabel();
    }
    
    @Override
    public int compareTo(Object o) {
        /*
         * The primary intent here is to provide ordering by displayName. However, the
         * secondary comparison by id is needed to insure that two DataFiles with the
         * same displayName aren't considered equal, e.g. in structures that require
         * unique keys. See Issues #4287 and #6401.
         */
        DataFile other = (DataFile) o;
        int comparison = this.getDisplayName().toUpperCase().compareTo(other.getDisplayName().toUpperCase());
        if (comparison == 0) {
            comparison = this.getId().compareTo(other.getId());
        }
        return comparison;
    }
    
    /**
     * Check if the Geospatial Tag has been assigned to this file
     * @return 
     */
    public boolean hasGeospatialTag(){
        if (this.dataFileTags == null){
            return false;
        }
        for (DataFileTag tag : this.dataFileTags){
            if (tag.isGeospatialTag()){
                return true;
            }
        }
        return false;
    }

    
    /**
     *  Set rootDataFileId
     *  @param rootDataFileId
     */
    public void setRootDataFileId(Long rootDataFileId){
        this.rootDataFileId = rootDataFileId;
    }

    /**
     *  Get for rootDataFileId
     *  @return Long
     */
    public Long getRootDataFileId(){
        return this.rootDataFileId;
    }

//    public int getProvCplId() {
//        return provCplId;
//    }
//    
//    public void setProvCplId(int cplId) {
//        this.provCplId = cplId;
//    }
    
    public String getProvEntityName() {
        return provEntityName;
    }
    
    public void setProvEntityName(String name) {
        this.provEntityName = name;
    }
    
    /**
     *  Set previousDataFileId
     *  @param previousDataFileId
     */
    public void setPreviousDataFileId(Long previousDataFileId){
        this.previousDataFileId = previousDataFileId;
    }

    /**
     *  Get for previousDataFileId
     *  @return Long
     */
    public Long getPreviousDataFileId(){
        return this.previousDataFileId;
    }

    public String toPrettyJSON(){
        
        return serializeAsJSON(true);
    }

    public String toJSON(){
        
        return serializeAsJSON(false);
    }
    
    
    
    public JsonObject asGsonObject(boolean prettyPrint){
        
        GsonBuilder builder;
        if (prettyPrint){  // Add pretty printing
            builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting();
        }else{
            builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();                        
        }
        
        builder.serializeNulls();   // correctly capture nulls
        Gson gson = builder.create();

        // ----------------------------------
        // serialize this object + add the id
        // ----------------------------------
        JsonElement jsonObj = gson.toJsonTree(this);
        jsonObj.getAsJsonObject().addProperty("id", this.getId());

        // ----------------------------------
        //  get the FileMetadata object
        // ----------------------------------
        FileMetadata thisFileMetadata = this.getFileMetadata();

        // ----------------------------------
        //  Add dataset info
        // ----------------------------------

        Map<String, Object> datasetMap = new HashMap<>();
        // expensive call.......bleh!!! 
        // https://github.com/IQSS/dataverse/issues/761, https://github.com/IQSS/dataverse/issues/2110, https://github.com/IQSS/dataverse/issues/3191
        //
        datasetMap.put("title", thisFileMetadata.getDatasetVersion().getTitle());
        datasetMap.put("persistentId", getOwner().getGlobalId().asString());
        datasetMap.put("url", getOwner().getPersistentURL());
        datasetMap.put("version", thisFileMetadata.getDatasetVersion().getSemanticVersion());
        datasetMap.put("id", getOwner().getId());
        datasetMap.put("isPublished", thisFileMetadata.getDatasetVersion().isReleased());
        
        jsonObj.getAsJsonObject().add("dataset",  gson.toJsonTree(datasetMap));
       
        // ----------------------------------
        //  Add dataverse info
        // ----------------------------------
        Map<String, Object> dataverseMap = new HashMap<>();
        Dataverse dv = this.getOwner().getOwner();
        
        dataverseMap.put("name", dv.getName());
        dataverseMap.put("alias", dv.getAlias());
        dataverseMap.put("id", dv.getId()); 

        jsonObj.getAsJsonObject().add("dataverse",  gson.toJsonTree(dataverseMap));
        
        // ----------------------------------
        //  Add label (filename), description, and categories from the FileMetadata object
        // ----------------------------------

        jsonObj.getAsJsonObject().addProperty("filename", thisFileMetadata.getLabel());
        jsonObj.getAsJsonObject().addProperty("description", thisFileMetadata.getDescription());
        jsonObj.getAsJsonObject().add("categories", 
                            gson.toJsonTree(thisFileMetadata.getCategoriesByName())
                    );

        // ----------------------------------        
        // Tags
        // ----------------------------------               
        jsonObj.getAsJsonObject().add("tags", gson.toJsonTree(getTagLabels()));

        // ----------------------------------        
        // Checksum
        // ----------------------------------
        Map<String, String> checkSumMap = new HashMap<>();
        checkSumMap.put("type", getChecksumType().toString());
        checkSumMap.put("value", getChecksumValue());
        
        JsonElement checkSumJSONMap = gson.toJsonTree(checkSumMap);
        
        jsonObj.getAsJsonObject().add("checksum", checkSumJSONMap);
        
        return jsonObj.getAsJsonObject();
        
    }
    
    /**
     * 
     * @param prettyPrint
     * @return 
     */
    private String serializeAsJSON(boolean prettyPrint){
        
        JsonObject fullFileJSON = asGsonObject(prettyPrint);
              
        //return fullFileJSON.
        return fullFileJSON.toString();
        
    }
    
    public String getPublicationDateFormattedYYYYMMDD() {
        if (getPublicationDate() != null){
                   return new SimpleDateFormat("yyyy-MM-dd").format(getPublicationDate()); 
        }
        return null;
    }
    
    public String getCreateDateFormattedYYYYMMDD() {
        if (getCreateDate() != null){
                   return new SimpleDateFormat("yyyy-MM-dd").format(getCreateDate()); 
        }
        return null;
    }
    
    @Override
    public String getTargetUrl() {
        return DataFile.TARGET_URL;
    }

    private boolean tagExists(String tagLabel) {
        for (DataFileTag dataFileTag : dataFileTags) {
            if (dataFileTag.getTypeLabel().equals(tagLabel)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isDeaccessioned() {
        // return true, if all published versions were deaccessioned
        boolean inDeaccessionedVersions = false;
        for (FileMetadata fmd : getFileMetadatas()) {
            DatasetVersion testDsv = fmd.getDatasetVersion();
            if (testDsv.isReleased()) {
                return false;
            }
            // Also check for draft version
            if (testDsv.isDraft()) {
                return false;
            }
            if (testDsv.isDeaccessioned()) {
                inDeaccessionedVersions = true;
            }
        }
        return inDeaccessionedVersions; // since any published version would have already returned
    }
    public boolean isInDatasetVersion(DatasetVersion version) {
        for (FileMetadata fmd : getFileMetadatas()) {
            if (fmd.getDatasetVersion().equals(version)) {
                return true;
            }
        }
        return false;
    }
} // end of class
