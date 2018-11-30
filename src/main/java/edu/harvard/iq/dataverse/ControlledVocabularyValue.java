/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.MissingResourceException;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList="datasetfieldtype_id"), @Index(columnList="displayorder")})
public class ControlledVocabularyValue implements Serializable  {
    
    public static final Comparator<ControlledVocabularyValue> DisplayOrder = new Comparator<ControlledVocabularyValue>() {
        @Override
        public int compare(ControlledVocabularyValue o1, ControlledVocabularyValue o2) {
            return Integer.compare( o1.getDisplayOrder(), o2.getDisplayOrder() );
    }};

    public ControlledVocabularyValue() {
    }

    public ControlledVocabularyValue(Long id, String strValue, DatasetFieldType datasetFieldType) {
        this.id = id;
        this.strValue = strValue;
        this.datasetFieldType = datasetFieldType;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @Column(columnDefinition="TEXT", nullable=false) 
    private String strValue;

    public String getStrValue() {
        return strValue;
    }
    public void setStrValue(String strValue) {
        this.strValue = strValue;
        
    }
    
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    
    
    private int displayOrder;
    public int getDisplayOrder() { return this.displayOrder;}
    public void setDisplayOrder(int displayOrder) {this.displayOrder = displayOrder;} 
       
    
    @ManyToOne
    // @JoinColumn( nullable = false ) TODO this breaks for the N/A value. need to create an N/A type for that value.
    private DatasetFieldType datasetFieldType;
    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }
    public void setDatasetFieldType(DatasetFieldType datasetFieldType) {
        this.datasetFieldType = datasetFieldType;
    }
  
    @OneToMany(mappedBy = "controlledVocabularyValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<ControlledVocabAlternate> controlledVocabAlternates = new ArrayList<>();

    public Collection<ControlledVocabAlternate> getControlledVocabAlternates() {
        return controlledVocabAlternates;
    }

    public void setControlledVocabAlternates(Collection<ControlledVocabAlternate> controlledVocabAlternates) {
        this.controlledVocabAlternates = controlledVocabAlternates;
    }

    public String getLocaleStrValue()
    {
        String key = strValue.toLowerCase().replace(" " , "_");
        key = StringUtils.stripAccents(key);
        try {
            return BundleUtil.getStringFromPropertyFile("controlledvocabulary." + this.datasetFieldType.getName() + "." + key, getDatasetFieldType().getMetadataBlock().getName());
        } catch (MissingResourceException e) {
            return strValue;
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ControlledVocabularyValue)) {
            return false;
        }
        ControlledVocabularyValue other = (ControlledVocabularyValue) object;
        return Objects.equals(getId(), other.getId());
    }    
    
    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.ControlledVocabularyValue[ id=" + id + " ]";
    }     
    
}
