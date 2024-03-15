/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 *
 * @author ellenk
 */
@Entity
@Table(indexes = {@Index(columnList="controlledvocabularyvalue_id"), @Index(columnList="datasetfieldtype_id")})
public class ControlledVocabAlternate implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @Column(columnDefinition="TEXT", nullable = false) 
    private String strValue;

    public String getStrValue() {
        return strValue;
    }
    public void setStrValue(String strValue) {
        this.strValue = strValue;
        
    }
    
    @ManyToOne
    @JoinColumn( nullable = false )
    private DatasetFieldType datasetFieldType;
    
    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }
    public void setDatasetFieldType(DatasetFieldType datasetFieldType) {
        this.datasetFieldType = datasetFieldType;
    }
    
    @ManyToOne
    @JoinColumn( nullable = false )
    private ControlledVocabularyValue controlledVocabularyValue;

    public ControlledVocabularyValue getControlledVocabularyValue() {
        return controlledVocabularyValue;
    }

    public void setControlledVocabularyValue(ControlledVocabularyValue controlledVocabularyValue) {
        this.controlledVocabularyValue = controlledVocabularyValue;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ControlledVocabAlternate other = (ControlledVocabAlternate) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
    
}
