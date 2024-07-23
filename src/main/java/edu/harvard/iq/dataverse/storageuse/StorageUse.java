package edu.harvard.iq.dataverse.storageuse;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectContainer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 *
 * @author landreev
 */
@NamedQueries({
    @NamedQuery(name = "StorageUse.findByteSizeByDvContainerId",query = "SELECT su.sizeInBytes FROM StorageUse su WHERE su.dvObjectContainer.id =:dvObjectId "),
    @NamedQuery(name = "StorageUse.findByDvContainerId",query = "SELECT su FROM StorageUse su WHERE su.dvObjectContainer.id =:dvObjectId "),
    @NamedQuery(name = "StorageUse.incrementByteSizeByDvContainerId", query = "UPDATE StorageUse su SET su.sizeInBytes = su.sizeInBytes +:fileSize WHERE su.dvObjectContainer.id =:dvObjectId")
})
@Entity
@Table(indexes = {@Index(columnList="dvobjectcontainer_id")})
public class StorageUse implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @OneToOne
    @JoinColumn(nullable=false)
    private DvObject dvObjectContainer; 
    
    @Column
    private Long sizeInBytes = null; 
    
    public StorageUse() {}
    
    public StorageUse(DvObjectContainer dvObjectContainer) {
        this(dvObjectContainer, 0L);
    }
    
    public StorageUse(DvObjectContainer dvObjectContainer, Long sizeInBytes) {
        this.dvObjectContainer = dvObjectContainer;
        this.sizeInBytes = sizeInBytes;
    }
    
    public Long getSizeInBytes() {
        return sizeInBytes; 
    }
    
    public void setSizeInBytes(Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }
    
    public void incrementSizeInBytes(Long sizeInBytes) {
        this.sizeInBytes += sizeInBytes; 
    }
    
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StorageUse)) {
            return false;
        }
        StorageUse other = (StorageUse) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.storageuse.StorageUse[ id=" + id + " ]";
    }
    
}
