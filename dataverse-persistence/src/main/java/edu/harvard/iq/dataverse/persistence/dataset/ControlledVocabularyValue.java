package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.apache.commons.lang3.StringUtils;

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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList = "datasetfieldtype_id"), @Index(columnList = "displayorder")})
public class ControlledVocabularyValue implements Serializable {

    public static final Comparator<ControlledVocabularyValue> DisplayOrder
            = Comparator.comparingInt(ControlledVocabularyValue::getDisplayOrder);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(columnDefinition = "TEXT", nullable = false)
    private String strValue;

    private String identifier;

    private int displayOrder;

    @ManyToOne
    // @JoinColumn( nullable = false ) TODO this breaks for the N/A value. need to create an N/A type for that value.
    private DatasetFieldType datasetFieldType;

    @OneToMany(mappedBy = "controlledVocabularyValue", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<ControlledVocabAlternate> controlledVocabAlternates = new ArrayList<>();

    @Column
    private String displayGroup;

    // -------------------- CONSTRUCTORS --------------------

    public ControlledVocabularyValue() { }

    public ControlledVocabularyValue(Long id, String strValue, DatasetFieldType datasetFieldType) {
        this.id = id;
        this.strValue = strValue;
        this.datasetFieldType = datasetFieldType;
    }

    // -------------------- GETTERS --------------------

    public String getStrValue() {
        return strValue;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    public Collection<ControlledVocabAlternate> getControlledVocabAlternates() {
        return controlledVocabAlternates;
    }

    public String getDisplayGroup() {
        return displayGroup;
    }

    // -------------------- LOGIC --------------------

    public String getLocaleStrValue() {
        return getLocaleStrValue(BundleUtil.getCurrentLocale());
    }

    public String getLocaleStrValue(Locale locale) {
        String key = strValue.toLowerCase().replace(" ", "_");
        key = StringUtils.stripAccents(key);
        String value;
        try {
            value = BundleUtil.getStringFromNonDefaultBundleWithLocale(
                    "controlledvocabulary." + this.datasetFieldType.getName() + "." + key,
                    getDatasetFieldType().getMetadataBlock().getName(), locale);
        } catch (NullPointerException npe) {
            value = StringUtils.EMPTY;
        }
        return value.isEmpty() ? getStrValue() : value;
    }

    // -------------------- SETTERS --------------------

    public void setStrValue(String strValue) {
        this.strValue = strValue;

    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setDatasetFieldType(DatasetFieldType datasetFieldType) {
        this.datasetFieldType = datasetFieldType;
    }

    public void setControlledVocabAlternates(Collection<ControlledVocabAlternate> controlledVocabAlternates) {
        this.controlledVocabAlternates = controlledVocabAlternates;
    }

    public void setDisplayGroup(String displayGroup) {
        this.displayGroup = displayGroup;
    }

    // -------------------- hashCode & equals --------------------

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

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "ControlledVocabularyValue[ id=" + id + " ]";
    }
}
