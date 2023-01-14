package edu.harvard.iq.dataverse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author adaybujeda
 */
@Entity
@Table(indexes = {@Index(columnList="dataverse_id")
        , @Index(columnList="metadatablock_id")})
public class DataverseMetadataBlockFacet implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;

    @ManyToOne
    @JoinColumn(name = "metadatablock_id")
    private MetadataBlock metadataBlock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public MetadataBlock getMetadataBlock() {
        return metadataBlock;
    }

    public void setMetadataBlock(MetadataBlock metadataBlock) {
        this.metadataBlock = metadataBlock;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataverseMetadataBlockFacet)) {
            return false;
        }
        DataverseMetadataBlockFacet other = (DataverseMetadataBlockFacet) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return String.format("edu.harvard.iq.dataverse.DataverseMetadataBlockFacet[ id=%s ]", id);
    }
    
}

