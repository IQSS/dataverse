package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;

import javax.enterprise.inject.spi.InterceptionType;
import javax.faces.model.SelectItem;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;


/**
 * Defines the meaning and constraints of a metadata field and its values.
 *
 * @author Stephen Kraffmiller
 */
@NamedQueries({
        @NamedQuery(name = "DatasetFieldType.findByName",
                query = "SELECT dsfType FROM DatasetFieldType dsfType WHERE dsfType.name=:name"),
        @NamedQuery(name = "DatasetFieldType.findAllFacetable",
                query = "select dsfType from DatasetFieldType dsfType WHERE dsfType.facetable = true and dsfType.title != '' order by dsfType.id"),
        @NamedQuery(name = "DatasetFieldType.findFacetableByMetadaBlock",
                query = "select dsfType from DatasetFieldType dsfType WHERE dsfType.facetable = true and dsfType.title != '' and dsfType.metadataBlock.id = :metadataBlockId order by dsfType.id")
})
@Entity
@Table(indexes = {@Index(columnList = "metadatablock_id"), @Index(columnList = "parentdatasetfieldtype_id")})
public class DatasetFieldType implements Serializable, Comparable<DatasetFieldType> {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    /**
     * The internal, DDI-like name, no spaces, etc.
     */
    @Column(name = "name", columnDefinition = "TEXT", nullable = false)
    private String name;

    /**
     * A longer, human-friendlier name. Punctuation allowed.
     */
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    /**
     * A user-friendly Description; will be used for
     * mouse-overs, etc.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    /**
     * Metatype of the field.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;
    /**
     * Whether the value must be taken from a controlled vocabulary.
     */
    private boolean allowControlledVocabulary;
    /**
     * A watermark to be displayed in the UI.
     */
    private String watermark;

    private String validationFormat;

    @OneToMany(mappedBy = "datasetFieldType")
    private Set<DataverseFacet> dataverseFacets;

    @OneToMany(mappedBy = "datasetFieldType")
    private Set<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InputRendererType inputRendererType;
    
    @Column(nullable = false)
    private String inputRendererOptions;
    
    
    @Transient
    private Map<String, ControlledVocabularyValue> controlledVocabularyValuesByStrValue;

    @Transient
    private boolean requiredInDataverse;

    public void setRequiredInDataverse(boolean requiredInDataverse) {
        this.requiredInDataverse = requiredInDataverse;
    }

    public boolean isRequiredInDataverse() {
        return this.requiredInDataverse;
    }


    public DatasetFieldType() {
    }

    public DatasetFieldType(String name, FieldType fieldType, boolean allowMultiples) {
        this.name = name;
        this.fieldType = fieldType;
        this.allowMultiples = allowMultiples;
        childDatasetFieldTypes = new LinkedList<>();
    }

    private int displayOrder;
    private String displayFormat;

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getDisplayFormat() {
        return displayFormat;
    }

    public void setDisplayFormat(String displayFormat) {
        this.displayFormat = displayFormat;
    }

    public Boolean isSanitizeHtml() {
        if (this.fieldType.equals(FieldType.URL)) {
            return true;
        }
        return this.fieldType.equals(FieldType.TEXTBOX);
    }

    public Boolean isEscapeOutputText() {
        if (this.fieldType.equals(FieldType.URL)) {
            return false;
        }
        if (this.fieldType.equals(FieldType.TEXTBOX)) {
            return false;
        }
        return !(this.fieldType.equals(FieldType.TEXT) && this.displayFormat != null && this.displayFormat.contains("<a"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAllowControlledVocabulary() {
        return allowControlledVocabulary;
    }

    public void setAllowControlledVocabulary(boolean allowControlledVocabulary) {
        this.allowControlledVocabulary = allowControlledVocabulary;
    }

    /**
     * Determines whether an instance of this field type may have multiple
     * values.
     */
    private boolean allowMultiples;

    public boolean isAllowMultiples() {
        return this.allowMultiples;
    }

    public void setAllowMultiples(boolean allowMultiples) {
        this.allowMultiples = allowMultiples;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    /**
     * Determines whether this field type may be used as a facet.
     */
    private boolean facetable;

    public boolean isFacetable() {
        return facetable;
    }

    public void setFacetable(boolean facetable) {
        this.facetable = facetable;
    }

    public String getValidationFormat() {
        return validationFormat;
    }

    public void setValidationFormat(String validationFormat) {
        this.validationFormat = validationFormat;
    }

    /**
     * Determines whether this field type is displayed in the form when creating
     * the Dataset (or only later when editing after the initial creation).
     */
    private boolean displayOnCreate;

    public boolean isDisplayOnCreate() {
        return displayOnCreate;
    }

    public void setDisplayOnCreate(boolean displayOnCreate) {
        this.displayOnCreate = displayOnCreate;
    }

    public boolean isControlledVocabulary() {
        return controlledVocabularyValues != null && !controlledVocabularyValues.isEmpty();
    }

    /**
     * The {@code MetadataBlock} this field type belongs to.
     */
    @ManyToOne(cascade = CascadeType.MERGE)
    private MetadataBlock metadataBlock;

    public MetadataBlock getMetadataBlock() {
        return metadataBlock;
    }

    public void setMetadataBlock(MetadataBlock metadataBlock) {
        this.metadataBlock = metadataBlock;
    }

    /**
     * A formal URI for the field used in json-ld exports
     */
    @Column(name = "uri", columnDefinition = "TEXT")
    private String uri;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * The list of controlled vocabulary terms that may be used as values for
     * fields of this field type.
     */
    @OneToMany(mappedBy = "datasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private Collection<ControlledVocabularyValue> controlledVocabularyValues;

    public Collection<ControlledVocabularyValue> getControlledVocabularyValues() {
        return this.controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(Collection<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }

    public ControlledVocabularyValue getControlledVocabularyValue(String strValue) {
        if (!isControlledVocabulary()) {
            throw new IllegalStateException("getControlledVocabularyValue() called on a non-controlled vocabulary type.");
        }
        if (controlledVocabularyValuesByStrValue == null) {
            controlledVocabularyValuesByStrValue = new TreeMap<>();
            for (ControlledVocabularyValue cvv : getControlledVocabularyValues()) {
                controlledVocabularyValuesByStrValue.put(cvv.getStrValue(), cvv);
            }
        }
        return controlledVocabularyValuesByStrValue.get(strValue);
    }

    /**
     * Collection of field types that are children of this field type.
     * A field type may consist of one or more child field types, but only one
     * parent.
     */
    @OneToMany(mappedBy = "parentDatasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldType> childDatasetFieldTypes = new ArrayList<>();

    public List<DatasetFieldType> getChildDatasetFieldTypes() {
        return this.childDatasetFieldTypes;
    }

    public void setChildDatasetFieldTypes(Collection<DatasetFieldType> childDatasetFieldTypes) {
        this.childDatasetFieldTypes = new ArrayList<>(childDatasetFieldTypes);
    }

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldType parentDatasetFieldType;

    public DatasetFieldType getParentDatasetFieldType() {
        return parentDatasetFieldType;
    }

    public void setParentDatasetFieldType(DatasetFieldType parentDatasetFieldType) {
        this.parentDatasetFieldType = parentDatasetFieldType;
    }


    public Set<DataverseFacet> getDataverseFacets() {
        return dataverseFacets;
    }

    public void setDataverseFacets(Set<DataverseFacet> dataverseFacets) {
        this.dataverseFacets = dataverseFacets;
    }

    public Set<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevels() {
        return dataverseFieldTypeInputLevels;
    }

    public void setDataverseFieldTypeInputLevels(Set<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels) {
        this.dataverseFieldTypeInputLevels = dataverseFieldTypeInputLevels;
    }

    /**
     * Determines whether fields of this field type are always required. A
     * dataverse may set some fields required, but only if this is false.
     */
    private boolean required;

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    private boolean advancedSearchFieldType;

    public boolean isAdvancedSearchFieldType() {
        return this.advancedSearchFieldType;
    }

    public void setAdvancedSearchFieldType(boolean advancedSearchFieldType) {
        this.advancedSearchFieldType = advancedSearchFieldType;
    }

    /**
     * Indicates if field is primitive (if {@link #allowMultiples} is true values will be placed in
     * {@link DatasetField#getDatasetFieldsChildren()} otherwise in {@link DatasetField#getFieldValue()}).
     */
    public boolean isPrimitive() {
        return !isCompound();
    }

    public boolean isCompound() {
        return !getChildDatasetFieldTypes().isEmpty();
    }

    public boolean isChild() {
        return this.parentDatasetFieldType != null;
    }

    public boolean isSubField() {
        return this.parentDatasetFieldType != null;
    }

    public boolean isHasChildren() {
        return !this.childDatasetFieldTypes.isEmpty();
    }

    public boolean isHasRequiredChildren() {
        if (this.childDatasetFieldTypes.isEmpty()) {
            return false;
        } else {
            for (DatasetFieldType dsftC : this.childDatasetFieldTypes) {
                if (dsftC.isRequired()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isHasParent() {
        return this.parentDatasetFieldType != null;
    }

    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetFieldType)) {
            return false;
        }
        DatasetFieldType other = (DatasetFieldType) object;
        return this.id == other.id || (this.id != null && this.id.equals(other.id));
    }

    /**
     * List of fields that use this field type. If this field type is removed,
     * these fields will be removed too.
     */
    @OneToMany(mappedBy = "datasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetField> datasetFields;

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    public void setDatasetFields(List<DatasetField> datasetFieldValues) {
        this.datasetFields = datasetFieldValues;
    }

    @OneToMany(mappedBy = "datasetField", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetFieldDefaultValue> datasetFieldDefaultValues;

    public List<DatasetFieldDefaultValue> getDatasetFieldDefaultValues() {
        return datasetFieldDefaultValues;
    }

    public void setDatasetFieldDefaultValues(List<DatasetFieldDefaultValue> datasetFieldDefaultValues) {
        this.datasetFieldDefaultValues = datasetFieldDefaultValues;
    }

    @Override
    public int compareTo(DatasetFieldType o) {
        return Integer.compare(this.getDisplayOrder(), (o.getDisplayOrder()));
    }

    public String getDisplayName() {
        if (isHasParent() && !parentDatasetFieldType.getTitle().equals(title)) {
            return Optional.ofNullable(getLocaleTitleWithParent()).filter(s -> !s.isEmpty()).orElse(
                    parentDatasetFieldType.getLocaleTitle() + " " + getLocaleTitle());
        } else {
            return getLocaleTitle();
        }
    }

    public String getLocaleTitle() {
        if (getMetadataBlock() == null) {
            return title;
        } else {
            try {
                return BundleUtil.getStringFromPropertyFile("datasetfieldtype." + getName() + ".title",
                                                            getMetadataBlock().getName());
            } catch (MissingResourceException e) {
                return title;
            }
        }
    }

    public String getLocaleTitleWithParent() {
        if (getMetadataBlock() == null) {
            return title;
        } else {
            try {
                return BundleUtil.getStringFromPropertyFile("datasetfieldtype." + getName() + ".withParent.title",
                                                            getMetadataBlock().getName());
            } catch (MissingResourceException e) {
                return title;
            }
        }
    }

    public String getLocaleDescription() {
        if (getMetadataBlock() == null) {
            return description;
        } else {
            try {
                return BundleUtil.getStringFromPropertyFile("datasetfieldtype." + getName() + ".description",
                                                            getMetadataBlock().getName());
            } catch (MissingResourceException e) {
                return description;
            }
        }
    }

    public String getLocaleWatermark() {
        if (getMetadataBlock() == null) {
            return watermark;
        } else {
            try {
                return BundleUtil.getStringFromPropertyFile("datasetfieldtype." + getName() + ".watermark",
                                                            getMetadataBlock().getName());
            } catch (MissingResourceException e) {
                return watermark;
            }
        }
    }

    /**
     * Determinate if this DatasetFieldType or it's parent allows multiple values for solr field.
     */
    public boolean isThisOrParentAllowsMultipleValues() {
        return allowMultiples || isParentAllowsMutlipleValues();
    }

    private boolean isParentAllowsMutlipleValues() {
        return getParentDatasetFieldType() != null &&
                getParentDatasetFieldType().isAllowMultiples();
    }

    /**
     * Returns value that defines what component should
     * be used when editing dataset field with this type.
     * 
     * @see InputRendererType
     */
    public InputRendererType getInputRendererType() {
        return inputRendererType;
    }

    public void setInputRendererType(InputRendererType inputRendererType) {
        this.inputRendererType = inputRendererType;
    }

    /**
     * Returns string in form of a json that
     * defines options specific for each {@link InputRendererType}.
     */
    public String getInputRendererOptions() {
        return inputRendererOptions;
    }

    public void setInputRendererOptions(String inputRendererOptions) {
        this.inputRendererOptions = inputRendererOptions;
    }

    @Override
    public String toString() {
        return "[DatasetFieldType name:" + getName() + " id:" + getId() + "]";
    }
}
