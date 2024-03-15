/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
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
import jakarta.persistence.UniqueConstraint;

/**
 *
 * @author skraffmiller
 */
@NamedQueries({
    @NamedQuery(name = "DataverseFieldTypeInputLevel.removeByOwnerId",
            query = "DELETE FROM DataverseFieldTypeInputLevel f WHERE f.dataverse.id=:ownerId"),
    @NamedQuery(name = "DataverseFieldTypeInputLevel.findByDataverseId",
            query = "select f from DataverseFieldTypeInputLevel f where f.dataverse.id = :dataverseId"),
    @NamedQuery(name = "DataverseFieldTypeInputLevel.findByDataverseIdDatasetFieldTypeId",
            query = "select f from DataverseFieldTypeInputLevel f where f.dataverse.id = :dataverseId and f.datasetFieldType.id = :datasetFieldTypeId"),
    @NamedQuery(name = "DataverseFieldTypeInputLevel.findByDataverseIdAndDatasetFieldTypeIdList",
            query = "select f from DataverseFieldTypeInputLevel f where f.dataverse.id = :dataverseId and f.datasetFieldType.id in :datasetFieldIdList")
 
})
@Table(name="DataverseFieldTypeInputLevel"
        ,  uniqueConstraints={
            @UniqueConstraint(columnNames={"dataverse_id", "datasetfieldtype_id"})}
        , indexes = {@Index(columnList="dataverse_id")
		, @Index(columnList="datasetfieldtype_id")
		, @Index(columnList="required")}
)
@Entity
public class DataverseFieldTypeInputLevel implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;

    @ManyToOne
    @JoinColumn(name = "datasetfieldtype_id")
    private DatasetFieldType datasetFieldType;
    private boolean include;
    private boolean required;
    
    public DataverseFieldTypeInputLevel () {}
  
    public DataverseFieldTypeInputLevel (DatasetFieldType fieldType, Dataverse dataverse, boolean required, boolean include) {
        this.datasetFieldType = fieldType;
        this.dataverse = dataverse;
        this.required = required;
        this.include = include;
    }    

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

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DataverseFieldTypeInputLevel)) {
            return false;
        }
        DataverseFieldTypeInputLevel other = (DataverseFieldTypeInputLevel) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel[ id=" + id + " ]";
    }

}
