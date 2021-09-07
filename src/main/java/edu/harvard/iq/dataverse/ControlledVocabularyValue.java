package edu.harvard.iq.dataverse;

import com.univocity.parsers.annotations.Parsed;
import com.univocity.parsers.annotations.Validate;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.metadata.Placeholder;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.MissingResourceException;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
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
    
    private static final Logger logger = Logger.getLogger(ControlledVocabularyValue.class.getCanonicalName());
    
    /**
     * Identifiers are used to match either URLs (Term), URIs (PID) or string containing only A-Z, a-z, 0-9, _, + and -
     * (If no identifier is set, the value will be used, so it may contain spaces in the end. But IF you provide
     *  an identifier, you do it for good reasons. Any real identifiers out there don't contain whitespace for a reason)
     */
    public static final String IDENTIFIER_MATCH_REGEX = "^(\\w+:(\\/\\/)?[\\w\\-+&@#/%?=~|!:,.;]*[\\w\\-+&@#/%=~|]|[\\w\\-\\+]+)$";
    public static final Comparator<ControlledVocabularyValue> DisplayOrder = Comparator.comparingInt(ControlledVocabularyValue::getDisplayOrder);
    
    public enum Headers {
        DATASET_FIELD(Constants.DATASET_FIELD),
        VALUE(Constants.VALUE),
        IDENTIFIER(Constants.IDENTIFIER),
        DISPLAY_ORDER(Constants.DISPLAY_ORDER),
        ALT_VALUES(Constants.ALT_VALUES);
    
        public static final class Constants {
            public final static String DATASET_FIELD = "DatasetField";
            public final static String VALUE = "Value";
            public final static String IDENTIFIER = "identifier";
            public final static String DISPLAY_ORDER = "displayOrder";
            public final static String ALT_VALUES = "altValue";
        }
    
        private final String key;
        Headers(String key) {
            this.key = key;
        }
        public String key() {
            return this.key;
        }
    
        public static String[] keys() {
            return Arrays.stream(values()).map(Headers::key).collect(Collectors.toUnmodifiableList()).toArray(new String[]{});
        }
    }

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
    
    @Parsed(field = Headers.Constants.VALUE)
    @Validate
    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }
    
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    @Parsed(field = Headers.Constants.IDENTIFIER)
    @Validate(nullable = true, matches = IDENTIFIER_MATCH_REGEX)
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    
    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    /**
     * Set display order value from String. Allow only positive integers >= 0.
     * @param displayOrder
     */
    @Parsed(field = Headers.Constants.DISPLAY_ORDER)
    @Validate(matches = "^\\d+$")
    public void setDisplayOrder(String displayOrder) {
        this.displayOrder = Integer.parseInt(displayOrder);
    }
       
    
    @ManyToOne
    // @JoinColumn( nullable = false ) TODO this breaks for the N/A value. need to create an N/A type for that value.
    private DatasetFieldType datasetFieldType;
    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }
    public void setDatasetFieldType(DatasetFieldType datasetFieldType) {
        this.datasetFieldType = datasetFieldType;
    }
    
    @Parsed(field = Headers.Constants.DATASET_FIELD)
    @Validate(matches = DatasetFieldType.FIELD_NAME_REGEX)
    private void setDatasetFieldType(String datasetFieldType) {
        this.datasetFieldType = new Placeholder.DatasetFieldType();
        this.datasetFieldType.setName(datasetFieldType);
    }
  
    @OneToMany(mappedBy = "controlledVocabularyValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval=true)
    private Collection<ControlledVocabAlternate> controlledVocabAlternates = new ArrayList<>();

    public Collection<ControlledVocabAlternate> getControlledVocabAlternates() {
        return controlledVocabAlternates;
    }

    public void setControlledVocabAlternates(Collection<ControlledVocabAlternate> controlledVocabAlternates) {
        this.controlledVocabAlternates = controlledVocabAlternates;
    }
    
    /**
     * A hacky workaround to allow arbitrary numbers of "altValue" columns in the TSV file, providing
     * alternative values for the controlled vocabulary value.
     * @param alternative
     */
    @Parsed(field = Headers.Constants.ALT_VALUES)
    @Validate(nullable = true, allowBlanks = true)
    private void addControlledVocabAlternates(String alternative) {
        if (alternative == null || alternative.isBlank()) {
            return;
        }
        ControlledVocabAlternate alt = new Placeholder.ControlledVocabAlternate();
        alt.setControlledVocabularyValue(this);
        alt.setStrValue(alternative);
        this.controlledVocabAlternates.add(alt);
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
            logger.warning("Error finding" + "controlledvocabulary." + fieldTypeName + "." + key + " in " + ((locale==null)? "defaultLang" : locale.getLanguage()) + " : " + e.getLocalizedMessage());
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
