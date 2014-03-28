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

/**
 *
 * @author gdurand
 */
@Entity
public class DatasetFieldValueValue implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public DatasetFieldValueValue() {
    }

    public DatasetFieldValueValue(DatasetFieldValue dsf, String value) {
        setDatasetField(dsf);
        dsf.getDatasetFieldValues().add(this);
        
        setValue(value);
    }
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String value;
    private int displayOrder;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetFieldValue datasetField;    

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

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public DatasetFieldValue getDatasetField() {
        return datasetField;
    }

    public void setDatasetField(DatasetFieldValue datasetField) {
        this.datasetField = datasetField;
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
        if (!(object instanceof DatasetFieldValueValue)) {
            return false;
        }
        DatasetFieldValueValue other = (DatasetFieldValueValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetFieldValueValue[ id=" + id + " ]";
    }

    public DatasetFieldValueValue copy(DatasetFieldValue dsf) {
        DatasetFieldValueValue dsfv = new DatasetFieldValueValue();
        dsfv.setDatasetField(dsf);
        dsfv.setDisplayOrder(displayOrder);
        dsfv.setValue(value);
                     
        return dsfv;
    }    
    
}
