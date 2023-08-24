
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.Serializable;
import java.util.MissingResourceException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

/**
 *
 * @author ekraffmiller 
 * Represents a generic file that is associated with a dataFile.
 * This is a data representation of a physical file in StorageIO
 */
@NamedQueries({
    @NamedQuery(name = "AuxiliaryFile.lookupAuxiliaryFile",
            query = "select object(o) from AuxiliaryFile as o where o.dataFile.id = :dataFileId and o.formatTag = :formatTag and o.formatVersion = :formatVersion"),
    @NamedQuery(name = "AuxiliaryFile.findAuxiliaryFiles",
            query = "select object(o) from AuxiliaryFile as o where o.dataFile.id = :dataFileId"),
    @NamedQuery(name = "AuxiliaryFile.findAuxiliaryFilesByType",
            query = "select object(o) from AuxiliaryFile as o where o.dataFile.id = :dataFileId and o.type = :type"),
    @NamedQuery(name = "AuxiliaryFile.findAuxiliaryFilesWithoutType",
            query = "select object(o) from AuxiliaryFile as o where o.dataFile.id = :dataFileId and o.type is null"),
    @NamedQuery(name = "AuxiliaryFile.findAuxiliaryFilesByOrigin",
            query = "select object(o) from AuxiliaryFile as o where o.dataFile.id = :dataFileId and o.origin = :origin"),
})
@NamedNativeQueries({
    @NamedNativeQuery(name = "AuxiliaryFile.findAuxiliaryFileTypes",
            query = "select distinct type from auxiliaryfile where datafile_id = ?1")
})
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

    /**
     * The application/entity that created the auxiliary file.
     */
    private String origin;
    
    private boolean isPublic;
    
    private String contentType; 
    
    private Long fileSize; 
    
    private String checksum;

    /**
     * A way of grouping similar auxiliary files together. The type could be
     * "DP" for "Differentially Private Statistics", for example.
     */
    private String type;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeFriendly() {
        try {
            return BundleUtil.getStringFromPropertyFile("file.auxfiles.types." + type, "Bundle");
        } catch (MissingResourceException ex) {
            return null;
        }
    }

}
