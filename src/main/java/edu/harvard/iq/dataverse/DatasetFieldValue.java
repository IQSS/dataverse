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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


@Entity
public class DatasetFieldValue implements Serializable {

    public DatasetFieldValue () {
    }
    
    public DatasetFieldValue(DatasetField sf, Metadata m, String val) {
        setDatasetField(sf);
        setMetadata(m);
        setStrValue(val);    
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
    
     /**
     * Holds value of property studyField.
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetField datasetField;

    /**
     * Getter for property template.
     * @return Value of property template.
     */
    public DatasetField getDatasetField() {
        return datasetField;
    }

    /**
     * Setter for property template.
     * @param template New value of property template.
     */
    public void setDatasetField(DatasetField datasetField) {
        this.datasetField=datasetField;
    }

    @ManyToOne
    @JoinColumn(nullable=false)
    private Metadata metadata;
    public Metadata getMetadata() {
        return metadata;
    }
    public void setMetadata(Metadata metadata) {
        this.metadata=metadata;
    }
    
    
    @Column(columnDefinition="TEXT") 
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
    
     public boolean isEmpty() {
        return ((strValue==null || strValue.trim().equals("")));
    }
    
    

    private int displayOrder;
    public int getDisplayOrder() { return this.displayOrder;}
    public void setDisplayOrder(int displayOrder) {this.displayOrder = displayOrder;} 
    
}