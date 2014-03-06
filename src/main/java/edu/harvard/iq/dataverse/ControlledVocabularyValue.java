/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 *
 * @author skraffmiller
 */
@Entity
public class ControlledVocabularyValue implements Serializable  {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @Column(columnDefinition="TEXT") 
    private String strValue;

    public String getStrValue() {
        return strValue;
    }
    public void setStrValue(String strValue) {
        this.strValue = strValue;
        
    } 
    
    private int displayOrder;
    public int getDisplayOrder() { return this.displayOrder;}
    public void setDisplayOrder(int displayOrder) {this.displayOrder = displayOrder;} 
       
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DatasetField datasetField;
    public DatasetField getDatasetField() {
        return datasetField;
    }
    public void setDatasetField(DatasetField datasetField) {
        this.datasetField = datasetField;
    }
    
}
