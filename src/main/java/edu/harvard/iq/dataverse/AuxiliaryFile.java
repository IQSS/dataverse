
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author ekraffmiller 
 * Represents a generic file that is associated with a dataFile.
 * This is a data representation of a physical file in StorageIO
 */
@Entity
public class AuxiliaryFile implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
     /**
     * The data file that this AuxiliaryFile belongs to
     * a data file may have many auxiliaryFiles
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;
  
    private String formatTag;
    
    private String formatVersion;
    
    private String origin;
    
    private boolean isPublic;
    
    private String contentType; 
    
    private Long fileSize; 
    
    private String checksum;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public String getFormatTag() {
        return formatTag;
    }

    public void setFormatTag(String formatTag) {
        this.formatTag = formatTag;
    }

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public Long getFileSize() {
            return fileSize; 
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    
}
