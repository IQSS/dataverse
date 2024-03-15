package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 *
 * @author skraffmiller
 */

@NamedQueries({
	@NamedQuery( name="DataverseFeaturedDataverse.removeByOwnerId",
				 query="DELETE FROM DataverseFeaturedDataverse f WHERE f.dataverse.id=:ownerId")
})

@Entity
@Table(indexes = {@Index(columnList="dataverse_id")
		, @Index(columnList="featureddataverse_id")
		, @Index(columnList="displayorder")})
public class DataverseFeaturedDataverse implements Serializable {
        private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name="dataverse_id")
    private Dataverse dataverse;

    @ManyToOne
    @JoinColumn(name="featureddataverse_id")
    private Dataverse featuredDataverse;

    private int displayOrder;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Dataverse getFeaturedDataverse() {
        return featuredDataverse;
    }

    public void setFeaturedDataverse(Dataverse featuredDataverse) {
        this.featuredDataverse = featuredDataverse;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    } 
  
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DatasetFieldType)) {
            return false;
        }
        DataverseFeaturedDataverse other = (DataverseFeaturedDataverse) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseFeaturedDataverse[ id=" + id + " ]";
    }
    
}
