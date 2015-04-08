/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Comparator;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author gdurand
 */
@Entity
@ValidateDatasetFieldType
@Table(indexes = {@Index(columnList="datasetfield_id")})
public class DatasetFieldValue implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final Comparator<DatasetFieldValue> DisplayOrder = new Comparator<DatasetFieldValue>() {
        @Override
        public int compare(DatasetFieldValue o1, DatasetFieldValue o2) {
            return Integer.compare( o1.getDisplayOrder(),
                                    o2.getDisplayOrder() );
    }};
    
    public DatasetFieldValue() {
    }
    
    public DatasetFieldValue(DatasetField aField) {
        setDatasetField(aField); 
    }    
        
    public DatasetFieldValue(DatasetField aField, String aValue) {
        setDatasetField(aField); 
        value = aValue;
    }    
          
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "value", columnDefinition = "TEXT", nullable = false)
    private String value;
    private int displayOrder;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetField datasetField;    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // these methods wrap around value but do not display the N/A value
    // (forcing validation)
    public String getValueForEdit() {
        return DatasetField.NA_VALUE.equals(value) ? "" : value;
    }

    public void setValueForEdit(String value) {
        this.value = value;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public DatasetField getDatasetField() {
        return datasetField;
    }

    public void setDatasetField(DatasetField datasetField) {
        this.datasetField = datasetField;
    }
    
    @Transient private String validationMessage;

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
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
        return "edu.harvard.iq.dataverse.DatasetFieldValueValue[ id=" + id + " ]";
    }

    public DatasetFieldValue copy(DatasetField dsf) {
        DatasetFieldValue dsfv = new DatasetFieldValue();
        dsfv.setDatasetField(dsf);
        dsfv.setDisplayOrder(displayOrder);
        dsfv.setValue(value);
                     
        return dsfv;
    }    
    
}
