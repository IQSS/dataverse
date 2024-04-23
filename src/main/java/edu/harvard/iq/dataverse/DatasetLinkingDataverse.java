package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "dataset_id"),
    @Index(columnList = "linkingDataverse_id")
})
@NamedQueries({
    @NamedQuery(name = "DatasetLinkingDataverse.findByDatasetId",
               query = "select object(o) from DatasetLinkingDataverse as o where o.dataset.id =:datasetId order by o.id"),
    @NamedQuery(name = "DatasetLinkingDataverse.findByLinkingDataverseId",
               query = "SELECT OBJECT(o) FROM DatasetLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId order by o.id"),    
    @NamedQuery(name = "DatasetLinkingDataverse.findByDatasetIdAndLinkingDataverseId",
               query = "SELECT OBJECT(o) FROM DatasetLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId AND o.dataset.id = :datasetId"),
    @NamedQuery(name = "DatasetLinkingDataverse.findIdsByLinkingDataverseId",
               query = "SELECT o.dataset.id FROM DatasetLinkingDataverse AS o WHERE o.linkingDataverse.id = :linkingDataverseId")
})
public class DatasetLinkingDataverse implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(nullable = false)
    private Dataset dataset;
    
    @OneToOne
    @JoinColumn(nullable = false)
    private Dataverse linkingDataverse;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable=false )
    private Date linkCreateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public Date getLinkCreateTime() {
        return linkCreateTime;
    }

    public void setLinkCreateTime(Date linkCreateTime) {
        this.linkCreateTime = linkCreateTime;
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
        if (!(object instanceof DatasetLinkingDataverse)) {
            return false;
        }
        DatasetLinkingDataverse other = (DatasetLinkingDataverse) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetLinkingDataverse[ id=" + id + " ]";
    }
    
}
