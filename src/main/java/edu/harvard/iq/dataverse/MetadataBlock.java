/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 *
 * @author skraffmiller
 */
@NamedQueries({
    @NamedQuery( name="MetadataBlock.listAll", query = "SELECT mdb FROM MetadataBlock mdb"),
    @NamedQuery( name="MetadataBlock.findByName", query = "SELECT mdb FROM MetadataBlock mdb WHERE mdb.name=:name")
})
@Entity
public class MetadataBlock implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String displayName;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    @OneToMany(mappedBy = "metadataBlock", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetFieldType> datasetFieldTypes;
    public List<DatasetFieldType> getDatasetFieldTypes() {
        return datasetFieldTypes;
    }
    
    public void setDatasetFieldTypes(List<DatasetFieldType> datasetFieldTypes) {
        this.datasetFieldTypes = datasetFieldTypes;
    }
    
    public boolean isDisplayOnCreate() {
        for (DatasetFieldType dsfType : datasetFieldTypes) {
            if (dsfType.isDisplayOnCreate()) {
                return true;
            }
        }
        return false;
    }

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isRequired() {
        // eventually this will be dynamic, for now only citation is required
        return "citation".equals(name);
    }
    
    @Transient
    private boolean empty;

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
   
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MetadataBlock)) {
            return false;
        }
        MetadataBlock other = (MetadataBlock) object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }    
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.MetadataBlock[ id=" + id + " ]";
    }    
    
}
