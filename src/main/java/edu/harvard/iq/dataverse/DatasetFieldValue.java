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
import java.util.Collection;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;


@Entity
public class DatasetFieldValue implements Serializable,   Comparable<DatasetFieldValue> {

    public DatasetFieldValue () {
    }
    
    public DatasetFieldValue(DatasetField sf, DatasetVersion dsv, String val) {
        setDatasetField(sf);
        setDatasetVersion(dsv);
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
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetField datasetField;
    public DatasetField getDatasetField() {
        return datasetField;
    }
    public void setDatasetField(DatasetField datasetField) {
        this.datasetField = datasetField;
    }
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }
    

    @OneToMany(mappedBy = "parentDatasetFieldValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldValue> childDatasetFieldValues;

    public List<DatasetFieldValue> getChildDatasetFieldValues() {
        return this.childDatasetFieldValues;
    }
    public void setChildDatasetFieldValues(List<DatasetFieldValue> childDatasetFieldValues) {
        this.childDatasetFieldValues = childDatasetFieldValues;
    }
    
    public boolean isChildEmpty(){
        //check all child values for empty...        
        for (DatasetFieldValue dsfvc: this.childDatasetFieldValues){
            if(!dsfvc.getStrValue().isEmpty()){
                return false;
            }
        }
        return true;
    }
    
    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldValue parentDatasetFieldValue;
    public DatasetFieldValue getParentDatasetFieldValue() {
        return parentDatasetFieldValue;
    }
    public void setParentDatasetFieldValue(DatasetFieldValue parentDatasetFieldValue) {
        this.parentDatasetFieldValue = parentDatasetFieldValue;
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

    @Override
    public int compareTo(DatasetFieldValue o) {
        return Integer.compare(this.getDatasetField().getDisplayOrder(),(o.getDatasetField().getDisplayOrder())); 
    }
    
}