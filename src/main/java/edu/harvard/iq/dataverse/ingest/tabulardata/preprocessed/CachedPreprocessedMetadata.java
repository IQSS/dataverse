/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.ingest.tabulardata.preprocessed;

import edu.harvard.iq.dataverse.DataFile;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Database entity representing a cached "preprocessed metadata" fragment 
 * for a tabular data file. 
 * @author Leonid Andreev
 */
@Entity
public class CachedPreprocessedMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }
    
    /* */
    
    public enum PrivacyLevelType { PUBLIC, CONFIDENTIAL };
    
    private static String PRIVACY_LABEL_PUBLIC = "PUBLIC";
    private static String PRIVACY_LABEL_CONFIDENTIAL = "CONFIDENTIAL";
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private DataFile dataFile;

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    private PrivacyLevelType privacyLevel; 

    public PrivacyLevelType getResult() {
        return privacyLevel;
    }
    
    
    public void setResult(PrivacyLevelType privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    public boolean isPublic() {
        return PrivacyLevelType.PUBLIC == privacyLevel;
    }

    public void setPublic() {
        privacyLevel = PrivacyLevelType.PUBLIC;
    }

    public boolean isConfidential() {
        return PrivacyLevelType.CONFIDENTIAL == privacyLevel;
    }

    public void setConfidential() {
        privacyLevel = PrivacyLevelType.CONFIDENTIAL;
    }
    
    

    /**
     * time/date this preprocessed fragment was cached
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date timeGenerated;

    public Date getTimeGenerated() {
        return timeGenerated;
    }

    public void setTimeGenerated(Date timeGenerated) {
        this.timeGenerated = timeGenerated;
    }

    /**
     * Name (identifier) of the tool that generated this preprocessed metadata 
     * snapshot. 
     * (For example, "R"; "PSI" (for "noisy" descriptive metadata produced for 
     * confidential tabular data files by the PSI tool)
     */
    private String producedBy; 
    
    public String getProducedBy() {
        return producedBy; 
    }
    
    public void setProducedBy(String producedBy) {
        this.producedBy = producedBy; 
    }
    
    /**
     * A label identifying the version of the tool that produced this preprocessed
     * fragment. 
     * Can be a standard version (for example, "1.0") of a software package; or 
     * a checksum of the R code fragment. In other words, any character string 
     * that could be used to associate this cache with a specific version of the
     * tool in question. 
     */
    private String versionIdentifier; 
    
    public String getVersionIdentifier() {
        return versionIdentifier; 
    }
    
    public void setVersionIdentifier(String versionIdentifier) {
        this.versionIdentifier = versionIdentifier; 
    }

    
    /**
     * 
     */
    private Integer storagePrefix; 
    
    public Integer storagePrefix() {
        return storagePrefix; 
    }
    
    public void setStoragePrefix(Integer storagePrefix) {
        this.storagePrefix = storagePrefix; 
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CachedPreprocessedMetadata)) {
            return false;
        }
        CachedPreprocessedMetadata other = (CachedPreprocessedMetadata) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.ingest.tabulardata.preprocessed.PreprocessedMetadata[ id=" + id + " ]";
    }
    
}
