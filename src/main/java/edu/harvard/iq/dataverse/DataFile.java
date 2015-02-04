package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.WorldMapRelatedData;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataAccessObject;
import edu.harvard.iq.dataverse.ingest.IngestReport;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 */
@NamedQueries({
	@NamedQuery( name="DataFile.removeFromDatasetVersion",
		query="DELETE FROM FileMetadata f WHERE f.datasetVersion.id=:versionId and f.dataFile.id=:fileId")
})
@Entity
public class DataFile extends DvObject {
    private static final long serialVersionUID = 1L;
    
    public static final char INGEST_STATUS_NONE = 65;
    public static final char INGEST_STATUS_SCHEDULED = 66;
    public static final char INGEST_STATUS_INPROGRESS = 67;
    public static final char INGEST_STATUS_ERROR = 68; 
    
    private String name;
    
    @NotBlank    
    @Pattern(regexp = "^.*/.*$", message = "Content-Type must contain a slash")
    private String contentType;
    
    private String fileSystemName;

    private String md5;

    @Column(nullable=true)
    private Long filesize;      // Number of bytes in file.  Allows 0 and null, negative numbers not permitted

    private boolean restricted;
    
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
    
    private char ingestStatus = INGEST_STATUS_NONE; 
    
    @OneToOne(mappedBy = "thumbnailFile")
    private Dataset thumbnailForDataset;
    

    public DataFile() {
        this.fileMetadatas = new ArrayList<>();
    }    

    public DataFile(String contentType) {
        this.contentType = contentType;
        this.fileMetadatas = new ArrayList<>();
    }
    
    // The dvObject field "name" should not be used in
    // datafile objects.
    // The file name must be stored in the file metadata.
    @Deprecated
    public DataFile(String name, String contentType) {
        this.name = name;
        this.contentType = contentType;
        this.fileMetadatas = new ArrayList<>();
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
            this.setDataTables( new ArrayList() );
        } else {
            this.getDataTables().clear();
        }

        this.getDataTables().add(dt);
    }
    
    public List<DataFileTag> getTags() {
        return dataFileTags;
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
            ingestReports = new ArrayList();
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
        if ( getDataTables() != null && getDataTables().size() > 0 ) {
            return true; 
        }
        return false; 
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

    /*
     * A user-friendly version of the "original format":
     */
    public String getOriginalFormatLabel() {
        return FileUtil.getUserFriendlyOriginalType(this);
    }
   
    // The dvObject field "name" should not be used in
    // datafile objects.
    // The file name must be stored in the file metadata.
    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
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
    
    public String getFileSystemName() {
        return this.fileSystemName;
    }

    public void setFileSystemName(String fileSystemName) {
        this.fileSystemName = fileSystemName;
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

        for (FileMetadata fileMetadata : fileMetadatas) {
            if (fmd == null || fileMetadata.getDatasetVersion().getId().compareTo( fmd.getDatasetVersion().getId() ) > 0 ) {
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
     */
    public String getFriendlySize() {
        return FileUtil.getFriendlySize(filesize);
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }


    public String getmd5() { 
        return this.md5; 
    }
    
    public void setmd5(String md5) { 
        this.md5 = md5; 
    }

    public Path getFileSystemLocation() {
        // TEMPORARY HACK!
        // (only used in batch ingest testing -- L.A. 4.0 beta)
        if (this.fileSystemName != null && this.fileSystemName.startsWith("/")) {
            return Paths.get(this.fileSystemName);
        }
        
        Path studyDirectoryPath = this.getOwner().getFileSystemDirectory();
        if (studyDirectoryPath == null) {
            return null;
        }
        String studyDirectory = studyDirectoryPath.toString();
 
        return Paths.get(studyDirectory, this.fileSystemName);
    }
    
    public DataAccessObject getAccessObject() throws IOException {
        DataAccessObject dataAccess =  DataAccess.createDataAccessObject(this);
        
        if (dataAccess == null) {
            throw new IOException("Failed to create access object for datafile.");
        }
        
        return dataAccess; 
    }
    
    public Path getSavedOriginalFile() {
       
        if (!this.isTabularData() || this.fileSystemName == null) {
            return null; 
        }
        
        Path studyDirectoryPath = this.getOwner().getFileSystemDirectory();
        if (studyDirectoryPath == null) {
            return null;
        }
        String studyDirectory = studyDirectoryPath.toString();
 
        Path savedOriginal = Paths.get(studyDirectory, "_" + this.fileSystemName);
        if (Files.exists(savedOriginal)) {
            return savedOriginal;
        }
        return null; 
    }
    
    public String getFilename() {
        String studyDirectory = this.getOwner().getFileSystemDirectory().toString();
 
        if (studyDirectory == null || this.fileSystemName == null || this.fileSystemName.equals("")) {
            return null;
        }
        String fileSystemPath = studyDirectory + "/" + this.fileSystemName;
        return fileSystemPath.replaceAll("/", "%2F");
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
        return "name:" + getName();
    }
	
	@Override
	public <T> T accept( Visitor<T> v ) {
		return v.visit(this);
	}
        
    public String getDisplayName() {
        // @todo should we show the published version label instead?
        // currently this method is not being used
        return getLatestFileMetadata().getLabel();
    }
}
