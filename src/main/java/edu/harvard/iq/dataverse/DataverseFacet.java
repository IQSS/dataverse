/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

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
  
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetFieldType)) {
            return false;
        }
        DataverseFacet other = (DataverseFacet) object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseFacet[ id=" + id + " ]";
    }
    
}

