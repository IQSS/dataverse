/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
 * @author gdurand
 */
@NamedQueries({
	@NamedQuery( name="DataverseFacet.removeByOwnerId",
				 query="DELETE FROM DataverseFacet f WHERE f.dataverse.id=:ownerId"),
    @NamedQuery( name="DataverseFacet.findByDataverseId",
                 query="select f from DataverseFacet f where f.dataverse.id = :dataverseId order by f.displayOrder")
})

@Entity
@Table(indexes = {@Index(columnList="dataverse_id")
		, @Index(columnList="datasetfieldtype_id")
		, @Index(columnList="displayorder")})
public class DataverseFacet implements Serializable {
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
  @JoinColumn(name="datasetfieldtype_id")
  private DatasetFieldType datasetFieldType;


  private int displayOrder;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    public void setDatasetFieldType(DatasetFieldType datasetFieldType) {
        this.datasetFieldType = datasetFieldType;
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
        if (!(object instanceof DataverseFacet)) {
            return false;
        }
        DataverseFacet other = (DataverseFacet) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseFacet[ id=" + id + " ]";
    }
    
}

