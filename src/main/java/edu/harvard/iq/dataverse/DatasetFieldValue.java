/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

/**
 *
 * @author skraffmiller
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
public class DatasetFieldValue implements Serializable {
    private static final long serialVersionUID = 1L;    

    public static DatasetFieldValue createNewEmptyDatasetField(DatasetField dsfType, DatasetVersion dsv) {
        DatasetFieldValue dsfv =  createNewEmptyDatasetField(dsfType);
        dsfv.setDatasetVersion(dsv);
        return dsfv;
    }
    
    public static DatasetFieldValue createNewEmptyDatasetField(DatasetField dsfType, DatasetFieldCompoundValue compoundValue) {
        DatasetFieldValue dsfv =  createNewEmptyDatasetField(dsfType);
        dsfv.setParentDatasetFieldCompoundValue(compoundValue);
        return dsfv;
    }    
    
    public static DatasetFieldValue createNewEmptyDatasetField(DatasetField dsfType) {
        DatasetFieldValue dsfv = new DatasetFieldValue();
        dsfv.setDatasetField(dsfType);
      
        if (dsfType.isPrimitive()) {
            if (!dsfType.isControlledVocabulary()) {
                dsfv.getDatasetFieldValues().add(new DatasetFieldValueValue(dsfv));
            }
        } else { // compound field
            dsfv.getDatasetFieldCompoundValues().add(DatasetFieldCompoundValue.createNewEmptyDatasetFieldCompoundValue(dsfv));
        } 
        
        return dsfv;
        
    }
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(nullable = false)
    private DatasetField datasetField;

    public DatasetField getDatasetField() {
        return datasetField;
    }
    
    public DatasetField getDatasetFieldType() {
        return datasetField;
    }    

    public void setDatasetField(DatasetField datasetField) {
        this.datasetField = datasetField;
    }

    @ManyToOne
    @JoinColumn(nullable = false)
    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldCompoundValue parentDatasetFieldCompoundValue;

    public DatasetFieldCompoundValue getParentDatasetFieldCompoundValue() {
        return parentDatasetFieldCompoundValue;
    }

    public void setParentDatasetFieldCompoundValue(DatasetFieldCompoundValue parentDatasetFieldCompoundValue) {
        this.parentDatasetFieldCompoundValue = parentDatasetFieldCompoundValue;
    }

    @OneToMany(mappedBy = "parentDatasetField", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldCompoundValue> datasetFieldCompoundValues = new ArrayList();

    public List<DatasetFieldCompoundValue> getDatasetFieldCompoundValues() {
        return datasetFieldCompoundValues;
    }

    public void setDatasetFieldCompoundValues(List<DatasetFieldCompoundValue> datasetFieldCompoundValues) {
        this.datasetFieldCompoundValues = datasetFieldCompoundValues;
    }

    @OneToMany(mappedBy = "datasetField", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldValueValue> datasetFieldValues = new ArrayList();

    public List<DatasetFieldValueValue> getDatasetFieldValues() {
        return this.datasetFieldValues;
    }

    public void setDatasetFieldValues(List<DatasetFieldValueValue> datasetFieldValues) {
        this.datasetFieldValues = datasetFieldValues;
    }

    @OneToMany(cascade = {CascadeType.MERGE})
    private List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList();

    public List<ControlledVocabularyValue> getControlledVocabularyValues() {
        return controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(List<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }

    
    // HELPER METHODS
    public DatasetFieldValueValue getSingleValue() {
        if (!datasetFieldValues.isEmpty()) {
            return datasetFieldValues.get(0);
        } else {
            return new DatasetFieldValueValue(this,null);
        }
    }
    
    public ControlledVocabularyValue getSingleControlledVocabularyValue() {
        if (!controlledVocabularyValues.isEmpty()) {
            return controlledVocabularyValues.get(0);
        } else {
            return null;
        }
    }

    public void setSingleControlledVocabularyValue(ControlledVocabularyValue cvv) {
        if (!controlledVocabularyValues.isEmpty()) {
            controlledVocabularyValues.set(0, cvv);
        } else {
            controlledVocabularyValues.add(cvv);
        }
    }
    
   

    public String getValue() {
        if (!datasetFieldValues.isEmpty()) {
            return datasetFieldValues.get(0).getValue();
        } else if (!controlledVocabularyValues.isEmpty()) {
            return controlledVocabularyValues.get(0).getStrValue();
        }
        return null;
    }

    public List<String> getValues() {
        List returnList = new ArrayList();
        if (!datasetFieldValues.isEmpty()) {
            for (DatasetFieldValueValue dsfv : datasetFieldValues) {
                returnList.add(dsfv.getValue());
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                returnList.add(cvv.getStrValue());
            }
        }
        return returnList;
    }
    
    public boolean isEmpty() {
        if (!datasetField.isHasChildren()) { // primitive
            for (String value : getValues()) {
                if (value != null && value.trim() != "") {
                    return false;
                }             
            }
        } else { // compound
            for (DatasetFieldCompoundValue cv : datasetFieldCompoundValues) {
                for (DatasetFieldValue subField : cv.getChildDatasetFields()) {
                    if (!subField.isEmpty()) {
                        return false;
                    }
                }              
            }
        }
        
        return true;
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
        if (!(object instanceof DatasetFieldValue)) {
            return false;
        }
        DatasetFieldValue other = (DatasetFieldValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetFieldValue[ id=" + id + " ]";
    }
    
    public DatasetFieldValue copy() {
        return copy(null);
    }
    
    public DatasetFieldValue copy(DatasetFieldCompoundValue parent) {
        DatasetFieldValue dsf = new DatasetFieldValue();
        dsf.setDatasetField(datasetField);
        dsf.setDatasetVersion(datasetVersion);
        dsf.setParentDatasetFieldCompoundValue(parent);        
        dsf.setControlledVocabularyValues(controlledVocabularyValues);
        
        for (DatasetFieldValueValue dsfv : datasetFieldValues) {
            dsf.getDatasetFieldValues().add(dsfv.copy(this));
        }
        
        for (DatasetFieldCompoundValue compoundValue : datasetFieldCompoundValues) {
            dsf.getDatasetFieldCompoundValues().add(compoundValue.copy(this));
        }        
                
        return dsf;
    }
}
