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
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.MissingResourceException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList="datasetfieldtype_id"), @Index(columnList="displayorder")})
public class ControlledVocabularyValue implements Serializable  {
    
    private static final Logger logger = Logger.getLogger(ControlledVocabularyValue.class.getCanonicalName());
    
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
  
    @OneToMany(mappedBy = "controlledVocabularyValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval=true)
    private Collection<ControlledVocabAlternate> controlledVocabAlternates = new ArrayList<>();

    public Collection<ControlledVocabAlternate> getControlledVocabAlternates() {
        return controlledVocabAlternates;
    }

    public void setControlledVocabAlternates(Collection<ControlledVocabAlternate> controlledVocabAlternates) {
        this.controlledVocabAlternates = controlledVocabAlternates;
    }

    public String getLocaleStrValue() {
        return getLocaleStrValue(null);
    }
    
    public String getLocaleStrValue(String language) {
        
        if(language !=null && language.isBlank()) {
            //null picks up current UI lang
            language=null;
        }
        //Sword input uses a special controlled vacab value ("N/A" that does not have a datasetFieldType / is not part of any metadata block, so handle it specially
        if(strValue.equals(DatasetField.NA_VALUE) && this.datasetFieldType == null) {
            return strValue;
        }
        if(this.datasetFieldType == null) {
            logger.warning("Null datasetFieldType for value: " + strValue);
        }
        return getLocaleStrValue(strValue, this.datasetFieldType.getName(),getDatasetFieldType().getMetadataBlock().getName(),language == null ? null : new Locale(language), true);
    }
    
    public static String getLocaleStrValue(String strValue, String fieldTypeName, String metadataBlockName,
            Locale locale, boolean sendDefault) {
        String key = strValue.toLowerCase().replace(" ", "_");
        key = StringUtils.stripAccents(key);
        try {
            String val = BundleUtil.getStringFromPropertyFile("controlledvocabulary." + fieldTypeName + "." + key,
                    metadataBlockName, locale);
            if (!val.isBlank()) {
                logger.fine("Found : " + val);
                return val;
            } else {
                return sendDefault ? strValue : null;
            }
        } catch (MissingResourceException | NullPointerException e) {
            logger.warning("Error finding " + "controlledvocabulary." + fieldTypeName + "." + key + " in " + ((locale==null)? "defaultLang" : locale.getLanguage()) + " : " + e.getLocalizedMessage());
            return sendDefault ? strValue : null;
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
