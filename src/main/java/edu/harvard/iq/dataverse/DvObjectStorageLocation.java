
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 *
 * @author skraffmi
 */
@Entity
public class DvObjectStorageLocation implements Serializable  {
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(nullable = false)
    private DvObject dvObject;
    
    @OneToOne
    @JoinColumn(nullable = false)
    private StorageLocation storageLocation;
    
    private String storageLocationAddress;
    
    /*
    Boolean primaryLocation:
        - true if this is the preferred location for download from Dataverse   
    */
    private Boolean primaryLocation;

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

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getStorageLocationAddress() {
        return storageLocationAddress;
    }

    public void setStorageLocationAddress(String storageLocationAddress) {
        this.storageLocationAddress = storageLocationAddress;
    }

    public Boolean getPrimaryLocation() {
        return primaryLocation;
    }

    public void setPrimaryLocation(Boolean primaryLocation) {
        this.primaryLocation = primaryLocation;
    }
    
}
