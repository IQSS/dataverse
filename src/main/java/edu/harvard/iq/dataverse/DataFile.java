package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.FileUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 */
@Entity
public class DataFile extends DvObject {
    private static final long serialVersionUID = 1L;
    
    private static final char INGEST_STATUS_NONE = 65;
    private static final char INGEST_STATUS_SCHEDULED = 66;
    private static final char INGEST_STATUS_INPROGRESS = 67;
    private static final char INGEST_STATUS_ERROR = 68; 
    
    @NotBlank
    private String name;
    
    @NotBlank    
    private String contentType;
    
    private String fileSystemName;

    private String md5;

    /*
        Tabular (formerly "subsettable") data files have DataTable objects
        associated with them:
    */
    
    @OneToMany(mappedBy = "dataFile", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataTable> dataTables;
    
    @OneToMany(mappedBy="dataFile", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<FileMetadata> fileMetadatas;
    
    @OneToMany(mappedBy="dataFile", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<FileMetadataFieldValue> fileMetadataFieldValues;
    
    private char ingestStatus = INGEST_STATUS_NONE; 

    public DataFile() {
        this.fileMetadatas = new ArrayList<>();
        fileMetadataFieldValues = new ArrayList<>();
    }    

    public DataFile(String name, String contentType) {
        this.name = name;
        this.contentType = contentType;
        this.fileMetadatas = new ArrayList<>();
        fileMetadataFieldValues = new ArrayList<>();
    }    

    public List<DataTable> getDataTables() {
        return dataTables;
    }

    public void setDataTables(List<DataTable> dataTables) {
        this.dataTables = dataTables;
    }
    
    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
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
        String originalFormat = getOriginalFileFormat(); 
        
        if (originalFormat != null) {
            if (originalFormat.equals("application/x-stata")) {
                return "Stata";
            } else if (originalFormat.equals("application/x-rlang-transport")) {
                return "RData";
            }
            return originalFormat; 
        }
        
        return null; 
    }
    
    public String getName() {
        return name;
    }

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
    
    public List<FileMetadataFieldValue> getFileMetadataFieldValues() {
        return fileMetadataFieldValues;
    }

    public void setFileMetadataFieldValues(List<FileMetadataFieldValue> fileMetadataFieldValues) {
        this.fileMetadataFieldValues = fileMetadataFieldValues;
    }
    
    public FileMetadata getFileMetadata() {
        return getLatestFileMetadata();
    }
    
    private FileMetadata getLatestFileMetadata() {
        FileMetadata fmd = null;

        for (FileMetadata fileMetadata : fileMetadatas) {
            if (fmd == null || fileMetadata.getDatasetVersion().getVersionNumber().compareTo( fmd.getDatasetVersion().getVersionNumber() ) > 0 ) {
                fmd = fileMetadata;
            }                       
        }
        return fmd;
    }
    
    
    public String getmd5() { 
        return this.md5; 
    }
    
    public void setmd5(String md5) { 
        this.md5 = md5; 
    }

    public Path getFileSystemLocation() {
        
        
        Path studyDirectoryPath = this.getOwner().getFileSystemDirectory();
        if (studyDirectoryPath == null) {
            return null;
        }
        String studyDirectory = studyDirectoryPath.toString();
 
        if (this.fileSystemName == null || this.fileSystemName.equals("")) {
            // *temporary* legacy hack - so that the files saved with their
            // user-supplied file names can still be downloaded; 
            // TODO: -- remove this as soon as it's no longer needed. 
            // -- L.A., 4.0 alpha 1
            
            return Paths.get(studyDirectory, this.name);
        }
        return Paths.get(studyDirectory, this.fileSystemName);
    }
    
    public String getFilename() {
        String studyDirectory = this.getOwner().getFileSystemDirectory().toString();
 
        if (studyDirectory == null || this.fileSystemName == null || this.fileSystemName.equals("")) {
            return null;
        }
        String fileSystemPath = studyDirectory + "/" + this.fileSystemName;
        return fileSystemPath.replaceAll("/", "%2F");
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
        return (contentType != null && contentType.startsWith("image/"));
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
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DataFile)) {
            return false;
        }
        DataFile other = (DataFile) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    protected String toStringExtras() {
        return "name:" + getName();
    }
	
	@Override
	public <T> T accept( Visitor<T> v ) {
		return v.visit(this);
	}
}
