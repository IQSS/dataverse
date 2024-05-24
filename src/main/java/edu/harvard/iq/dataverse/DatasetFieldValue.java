/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MarkupChecker;
import java.io.Serializable;
import java.util.Comparator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;

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
    
    public String getDisplayValue() {
        String retVal = "";
        if (!StringUtils.isBlank(this.getValue()) && !DatasetField.NA_VALUE.equals(this.getValue())) {
            String format = this.datasetField.getDatasetFieldType().getDisplayFormat();
            if (StringUtils.isBlank(format)) {
                format = "#VALUE";
            }           
            String sanitizedValue = !this.datasetField.getDatasetFieldType().isSanitizeHtml() ? this.getValue() :  MarkupChecker.sanitizeBasicHTML(this.getValue());    
            
                if (!this.datasetField.getDatasetFieldType().isSanitizeHtml() && this.datasetField.getDatasetFieldType().isEscapeOutputText()){
                    sanitizedValue = MarkupChecker.stripAllTags(sanitizedValue);
                }
            
            // replace the special values in the format (note: we replace #VALUE last since we don't
            // want any issues if the value itself has #NAME in it)
            String displayValue = format
                    .replace("#NAME",  this.datasetField.getDatasetFieldType().getTitle() == null ? "" : this.datasetField.getDatasetFieldType().getTitle())
                    .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                    .replace("#VALUE", sanitizedValue);
            retVal = displayValue;
        }

        return retVal;
    }

    public String getUnsanitizedDisplayValue() {
        String retVal = "";
        if (!StringUtils.isBlank(this.getValue()) && !DatasetField.NA_VALUE.equals(this.getValue())) {
            String format = this.datasetField.getDatasetFieldType().getDisplayFormat();
            if (StringUtils.isBlank(format)) {
                format = "#VALUE";
            }           
            String value = this.getValue();    
            String displayValue = format
                    .replace("#NAME",  this.datasetField.getDatasetFieldType().getTitle() == null ? "" : this.datasetField.getDatasetFieldType().getTitle())
                    .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                    .replace("#VALUE", value);
            retVal = displayValue;
        }
        return retVal;
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
