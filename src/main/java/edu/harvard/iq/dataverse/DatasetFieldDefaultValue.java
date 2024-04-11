/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList="datasetfield_id"), 
    @Index(columnList="defaultvalueset_id"), 
    @Index(columnList="parentdatasetfielddefaultvalue_id"), 
    @Index(columnList="displayorder")})
public class DatasetFieldDefaultValue implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    public DatasetFieldDefaultValue() {
   
    }
    
    public DatasetFieldDefaultValue(DatasetFieldType sf, DefaultValueSet dvs, String val) {
        setDatasetField(sf);
        setDefaultValueSet(dvs);
        setStrValue(val);    
    } 
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetFieldType datasetField;
    public DatasetFieldType getDatasetField() {
        return datasetField;
    }
    public void setDatasetField(DatasetFieldType datasetField) {
        this.datasetField = datasetField;
    }
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DefaultValueSet defaultValueSet;
    public DefaultValueSet getDefaultValueSet() {
        return defaultValueSet;
    }
    public void setDefaultValueSet(DefaultValueSet defaultValueSet) {
        this.defaultValueSet = defaultValueSet;
    }
    
    @OneToMany(mappedBy = "parentDatasetFieldDefaultValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private Collection<DatasetFieldDefaultValue> childDatasetFieldDefaultValues;

    public Collection<DatasetFieldDefaultValue> getChildDatasetFieldDefaultValues() {
        return this.childDatasetFieldDefaultValues;
    }
    public void setChildDatasetFieldDefaultValues(Collection<DatasetFieldDefaultValue> childDatasetFieldDefaultValues) {
        this.childDatasetFieldDefaultValues = childDatasetFieldDefaultValues;
    }
    
    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldDefaultValue parentDatasetFieldDefaultValue;
    public DatasetFieldDefaultValue getParentDatasetFieldDefaultValue() {
        return parentDatasetFieldDefaultValue;
    }
    public void setParentDatasetFieldValue(DatasetFieldDefaultValue parentDatasetFieldDefaultValue) {
        this.parentDatasetFieldDefaultValue = parentDatasetFieldDefaultValue;
    }
        
    @Column(columnDefinition="TEXT", nullable=false )
    private String strValue;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
        
    }    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DatasetFieldDefaultValue)) {
            return false;
        }
        DatasetFieldDefaultValue other = (DatasetFieldDefaultValue) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetFieldDefaultValue[ id=" + id + " ]";
    }
    
     public boolean isEmpty() {
        return ((strValue==null || strValue.trim().equals("")));
    }
    
    private int displayOrder;
    public int getDisplayOrder() { return this.displayOrder;}
    public void setDisplayOrder(int displayOrder) {this.displayOrder = displayOrder;} 
    
}
