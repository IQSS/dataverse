package edu.harvard.iq.dataverse.storageuse;

import edu.harvard.iq.dataverse.DvObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.io.Serializable;
import java.util.logging.Logger;

//import jakarta.persistence.*;

/**
 *
 * @author landreev
 * 
 */
@Entity
public class StorageQuota implements Serializable {
    private static final Logger logger = Logger.getLogger(StorageQuota.class.getCanonicalName()); 
    
    /**
     * Only Collection quotas are supported, for now
     */
    
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
    
    /**
     * For defining quotas for Users and/or Groups 
     * (Not supported as of yet)
     
    @Column(nullable = true)
    private String assigneeIdentifier;
    */
    
    /**
     * Could be changed to ManyToOne - if we wanted to be able to define separate
     * quotas on the same collection for different users. (?)
     * Whether we actually want to support the above is TBD. (possibly not)
     * Only collection-wide quotas are supported for now. 
     */
    @OneToOne
    @JoinColumn(name="definitionPoint_id", nullable=true)
    private DvObject definitionPoint;
    
    @Column(nullable = true)
    private Long allocation; 
    
    public StorageQuota() {}
    
    /**
     * Could be uncommented if/when we want to add per-user quotas (see above)
    public String getAssigneeIdentifier() {
        return assigneeIdentifier;
    }

    public void setAssigneeIdentifier(String assigneeIdentifier) {
        this.assigneeIdentifier = assigneeIdentifier;
    }*/
    
    public DvObject getDefinitionPoint() {
        return definitionPoint;
    }

    public void setDefinitionPoint(DvObject definitionPoint) {
        this.definitionPoint = definitionPoint;
    }
    
    public Long getAllocation() {
        return allocation; 
    }
    
    public void setAllocation(Long allocation) {
        this.allocation = allocation; 
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
        if (!(object instanceof StorageQuota)) {
            return false;
        }
        StorageQuota other = (StorageQuota) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.storageuse.StorageQuota[ id=" + id + " ]";
    }
    
}
