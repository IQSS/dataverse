
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author skraffmi
 */
@Entity
public class AlternativePersistentIdentifier implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
     /**
     * Holds value of the DvObject
     * a dvObject may have many Alternate Persistent Identifiers 
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DvObject dvObject;
    
    private String protocol;
    private String authority;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date globalIdCreateTime;

    private String identifier;
    
    private boolean identifierRegistered;
    
    private boolean storageLocationDesignator;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DvObject getDvObject() {
        return dvObject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public Date getGlobalIdCreateTime() {
        return globalIdCreateTime;
    }

    public void setGlobalIdCreateTime(Date globalIdCreateTime) {
        this.globalIdCreateTime = globalIdCreateTime;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isIdentifierRegistered() {
        return identifierRegistered;
    }

    public void setIdentifierRegistered(boolean identifierRegistered) {
        this.identifierRegistered = identifierRegistered;
    }
    
    public boolean isStorageLocationDesignator() {
        return storageLocationDesignator;
    }

    public void setStorageLocationDesignator(boolean localFileDirectory) {
        this.storageLocationDesignator = localFileDirectory;
    }
    
}
