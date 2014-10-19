/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
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
    
    public static final Comparator<DatasetFieldCompoundValue> DisplayOrder = new Comparator<DatasetFieldCompoundValue>() {
        @Override
        public int compare(DatasetFieldCompoundValue o1, DatasetFieldCompoundValue o2) {
            return Integer.compare( o1.getDisplayOrder(),
                                    o2.getDisplayOrder() );
    }};
    
    public static DatasetFieldCompoundValue createNewEmptyDatasetFieldCompoundValue(DatasetField dsf) {
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(dsf);

        for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
            compoundValue.getChildDatasetFields().add( DatasetField.createNewEmptyChildDatasetField(dsfType, compoundValue));
        }
        
        return compoundValue;
    }    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private int displayOrder;

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetField parentDatasetField;    

    @OneToMany(mappedBy = "parentDatasetFieldCompoundValue", orphanRemoval=true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("datasetFieldType ASC")
    private List<DatasetField> childDatasetFields = new ArrayList();    
    
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
    
    public DatasetField getParentDatasetField() {
        return parentDatasetField;
    }

    public void setParentDatasetField(DatasetField parentDatasetField) {
        this.parentDatasetField = parentDatasetField;
    }

    public List<DatasetField> getChildDatasetFields() {
        return childDatasetFields;
    }

    public void setChildDatasetFields(List<DatasetField> childDatasetFields) {
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
        if (!(object instanceof DatasetFieldCompoundValue)) {
            return false;
        }
        DatasetFieldCompoundValue other = (DatasetFieldCompoundValue) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetFieldCompoundValue[ id=" + id + " ]";
    }
    
    public DatasetFieldCompoundValue copy(DatasetField parent) {
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(parent);
        compoundValue.setDisplayOrder(displayOrder);

        for (DatasetField subField : childDatasetFields) {
            compoundValue.getChildDatasetFields().add(subField.copyChild(compoundValue));
        }
                     
        return compoundValue;
    }
}    

