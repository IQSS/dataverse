/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

/**
 *
 * @author gdurand
 */
@Entity
public class DatasetFieldCompoundValue implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private int displayOrder;

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldValue parentDatasetField;    

    @OneToMany(mappedBy = "parentDatasetFieldCompoundValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetFieldValue> childDatasetFields = new ArrayList();    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public DatasetFieldValue getParentDatasetField() {
        return parentDatasetField;
    }

    public void setParentDatasetField(DatasetFieldValue parentDatasetField) {
        this.parentDatasetField = parentDatasetField;
    }

    public List<DatasetFieldValue> getChildDatasetFields() {
        return childDatasetFields;
    }

    public void setChildDatasetFields(List<DatasetFieldValue> childDatasetFields) {
        this.childDatasetFields = childDatasetFields;
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
        if (!(object instanceof DatasetFieldCompoundValue)) {
            return false;
        }
        DatasetFieldCompoundValue other = (DatasetFieldCompoundValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetFieldCompoundValue[ id=" + id + " ]";
    }
    
    public DatasetFieldCompoundValue copy(DatasetFieldValue parent) {
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(parent);
        compoundValue.setDisplayOrder(displayOrder);

        for (DatasetFieldValue subField : childDatasetFields) {
            compoundValue.getChildDatasetFields().add(subField.copy(this));
        }
                     
        return compoundValue;
    }
}    

