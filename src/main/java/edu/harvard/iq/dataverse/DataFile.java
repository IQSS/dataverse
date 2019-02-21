package edu.harvard.iq.dataverse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.api.WorldMapRelatedData;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.datasetutility.FileSizeChecker;
import edu.harvard.iq.dataverse.ingest.IngestReport;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 */
@NamedQueries({
	@NamedQuery( name="DataFile.removeFromDatasetVersion",
		query="DELETE FROM FileMetadata f WHERE f.datasetVersion.id=:versionId and f.dataFile.id=:fileId"),
        @NamedQuery(name="DataFile.findDataFileByIdProtocolAuth", 
                query="SELECT s FROM DataFile s WHERE s.identifier=:identifier AND s.protocol=:protocol AND s.authority=:authority")
})
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
    private List<IngestReport> ingestReports;
    
    @OneToOne(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private IngestRequest ingestRequest;
    
    @OneToMany(mappedBy = "dataFile", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataFileTag> dataFileTags;
    
    @OneToMany(mappedBy="dataFile", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<FileMetadata> fileMetadatas;
    
    @OneToMany(mappedBy="dataFile", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<GuestbookResponse> guestbookResponses;

    public List<GuestbookResponse> getGuestbookResponses() {
        return guestbookResponses;
    }

    public void setGuestbookResponses(List<GuestbookResponse> guestbookResponses) {
        this.guestbookResponses = guestbookResponses;
    }
    
    private char ingestStatus = INGEST_STATUS_NONE; 
    
    @OneToOne(mappedBy = "thumbnailFile")
    private Dataset thumbnailForDataset;

    public DataFile() {
        this.fileMetadatas = new ArrayList<>();
        initFileReplaceAttributes();
    }    

    public DataFile(String contentType) {
        this.contentType = contentType;
        this.fileMetadatas = new ArrayList<>();
        initFileReplaceAttributes();
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
        return "Ingest failed. No further information is available.";
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
    
    public FileMetadata getFileMetadata() {
        return getLatestFileMetadata();
    }
    
    private FileMetadata getLatestFileMetadata() {
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
            if (fmd == null || fileMetadata.getDatasetVersion().getVersionNumber().compareTo( fmd.getDatasetVersion().getVersionNumber() ) > 0 ) {
                fmd = fileMetadata;
            } else if ((fileMetadata.getDatasetVersion().getVersionNumber().compareTo( fmd.getDatasetVersion().getVersionNumber())==0 )&& 
                   ( fileMetadata.getDatasetVersion().getMinorVersionNumber().compareTo( fmd.getDatasetVersion().getMinorVersionNumber()) > 0 )   ) {
                fmd = fileMetadata;
        }
        }
        return fmd;
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
        return FileSizeChecker.bytesToHumanReadable(filesize);
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
    
    /**
     * URL to use with the WorldMapRelatedData API
     * Used within dataset.xhtml
     * 
     * @param dataverseUserID
     * @return URL for "Map It" functionality
     */
    public String getMapItURL(Long dataverseUserID){
        if (dataverseUserID==null){
            return null;
        }
        return WorldMapRelatedData.getMapItURL(this.getId(), dataverseUserID);
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
    

    @ManyToMany
    @JoinTable(name = "fileaccessrequests",
    joinColumns = @JoinColumn(name = "datafile_id"),
    inverseJoinColumns = @JoinColumn(name = "authenticated_user_id"))
    private List<AuthenticatedUser> fileAccessRequesters;

    public List<AuthenticatedUser> getFileAccessRequesters() {
        return fileAccessRequesters;
    }

    public void setFileAccessRequesters(List<AuthenticatedUser> fileAccessRequesters) {
        this.fileAccessRequesters = fileAccessRequesters;
    }
    
    public boolean isHarvested() {
        
        // (storageIdentifier is not nullable - so no need to check for null
        // pointers below):
        if (this.getStorageIdentifier().startsWith("http://") || this.getStorageIdentifier().startsWith("https://")) {
            return true;
        }
        
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
    
    @Override
    public int compareTo(Object o) {
        DataFile other = (DataFile) o;
        return this.getDisplayName().toUpperCase().compareTo(other.getDisplayName().toUpperCase());
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
        
        String overarchingKey = "data";
        
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
        datasetMap.put("persistentId", getOwner().getGlobalIdString());
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
    

} // end of class
    

    

