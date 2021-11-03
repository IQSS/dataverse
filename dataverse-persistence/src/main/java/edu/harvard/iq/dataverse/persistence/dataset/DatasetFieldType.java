package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import org.apache.commons.lang3.StringUtils;

import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                query = "select dsfType from DatasetFieldType dsfType WHERE dsfType.facetable = true " +
                        "and dsfType.title != '' order by dsfType.id"),
        @NamedQuery(name = "DatasetFieldType.findFacetableByMetadataBlock",
                query = "select dsfType from DatasetFieldType dsfType WHERE dsfType.facetable = true " +
                        "and dsfType.title != '' and dsfType.metadataBlock.id = :metadataBlockId order by dsfType.id"),
        @NamedQuery(name = "DatasetFieldType.findAdvancedSearchFieldsByMetadataBlocks",
                query = "select dsfType from DatasetFieldType dsfType WHERE dsfType.advancedSearchFieldType = true " +
                        "and dsfType.title != '' and dsfType.metadataBlock.id IN :metadataBlockIds order by dsfType.metadataBlock.id, dsfType.displayOrder")
})
@Entity
@Table(indexes = {@Index(columnList = "metadatablock_id"), @Index(columnList = "parentdatasetfieldtype_id")})
public class DatasetFieldType implements Serializable, Comparable<DatasetFieldType>, JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The internal, DDI-like name, no spaces, etc. */
    @Column(name = "name", columnDefinition = "TEXT", nullable = false)
    private String name;

    /** A longer, human-friendlier name. Punctuation allowed. */
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    /** A user-friendly Description; will be used for mouse-overs, etc. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Metatype of the field. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    /** Whether the value must be taken from a controlled vocabulary. */
    private boolean allowControlledVocabulary;

    /** A watermark to be displayed in the UI. */
    private String watermark;

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

    private int displayOrder;

    private String displayFormat;

    /** Determines whether an instance of this field type may have multiple values. */
    private boolean allowMultiples;

    /** Determines whether this field type may be used as a facet. */
    private boolean facetable;

    /** Determines whether this field type is displayed in the form when creating
     * the Dataset (or only later when editing after the initial creation). */
    private boolean displayOnCreate;

    /** The {@code MetadataBlock} this field type belongs to. */
    @ManyToOne(cascade = CascadeType.MERGE)
    private MetadataBlock metadataBlock;

    /** A formal URI for the field used in json-ld exports */
    @Column(name = "uri", columnDefinition = "TEXT")
    private String uri;

    /** The list of controlled vocabulary terms that may be used as values for
     * fields of this field type. */
    @OneToMany(mappedBy = "datasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private Collection<ControlledVocabularyValue> controlledVocabularyValues;

    /** Collection of field types that are children of this field type.
     * A field type may consist of one or more child field types, but only one parent. */
    @OneToMany(mappedBy = "parentDatasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldType> childDatasetFieldTypes = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldType parentDatasetFieldType;

    @OneToMany(mappedBy = "datasetField", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetFieldDefaultValue> datasetFieldDefaultValues;

    /** Determines whether fields of this field type are always required. A
     * dataverse may set some fields required, but only if this is false. */
    private boolean required;

    private boolean advancedSearchFieldType;

    @Column(name="validation", nullable = false)
    private String validation;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldType() { }

    public DatasetFieldType(String name, FieldType fieldType, boolean allowMultiples) {
        this.name = name;
        this.fieldType = fieldType;
        this.allowMultiples = allowMultiples;
        childDatasetFieldTypes = new LinkedList<>();
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return this.id;
    }

    public boolean isRequiredInDataverse() {
        return this.requiredInDataverse;
    }

    public String getDisplayFormat() {
        return displayFormat;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAllowControlledVocabulary() {
        return allowControlledVocabulary;
    }

    public boolean isAllowMultiples() {
        return this.allowMultiples;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public String getWatermark() {
        return watermark;
    }

    public boolean isFacetable() {
        return facetable;
    }

    public boolean isDisplayOnCreate() {
        return displayOnCreate;
    }

    public MetadataBlock getMetadataBlock() {
        return metadataBlock;
    }

    public String getUri() {
        return uri;
    }

    public Collection<ControlledVocabularyValue> getControlledVocabularyValues() {
        return this.controlledVocabularyValues;
    }

    public List<DatasetFieldType> getChildDatasetFieldTypes() {
        return this.childDatasetFieldTypes;
    }

    public DatasetFieldType getParentDatasetFieldType() {
        return parentDatasetFieldType;
    }

    public Set<DataverseFacet> getDataverseFacets() {
        return dataverseFacets;
    }

    public Set<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevels() {
        return dataverseFieldTypeInputLevels;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean isAdvancedSearchFieldType() {
        return this.advancedSearchFieldType;
    }

    public List<DatasetFieldDefaultValue> getDatasetFieldDefaultValues() {
        return datasetFieldDefaultValues;
    }

    /**
     * Returns value that defines what component should be used when editing dataset field with this type.
     *
     * @see InputRendererType
     */
    public InputRendererType getInputRendererType() {
        return inputRendererType;
    }

    /**
     * Returns string in form of a json that defines options specific for each {@link InputRendererType}.
     */
    public String getInputRendererOptions() {
        return inputRendererOptions;
    }

    public String getValidation() {
        return validation;
    }

    // -------------------- LOGIC --------------------

    public Boolean isSanitizeHtml() {
        return FieldType.URL.equals(this.fieldType) || FieldType.TEXTBOX.equals(this.fieldType);
    }

    public Boolean isEscapeOutputText() {
        if (FieldType.URL.equals(this.fieldType) || FieldType.TEXTBOX.equals(this.fieldType)) {
            return false;
        }
        return !(FieldType.TEXT.equals(this.fieldType)
                && this.displayFormat != null
                && this.displayFormat.contains("<a"));
    }

    public boolean isControlledVocabulary() {
        return controlledVocabularyValues != null && !controlledVocabularyValues.isEmpty();
    }

    public Collection<SelectItem> getControlledVocabSelectItems(boolean withLocaleSorting) {

        ArrayList<SelectItem> groupedList = new ArrayList<>();

        Map<String, List<SelectItem>> groupsMap = new LinkedHashMap<>();
        List<SelectItem> itemsWithoutGroup = new ArrayList<>();

        for (ControlledVocabularyValue value : controlledVocabularyValues) {
            if (StringUtils.isNotEmpty(value.getDisplayGroup())) {
                if (!groupsMap.containsKey(value.getDisplayGroup())) {
                    groupsMap.put(value.getDisplayGroup(), new ArrayList<>());
                }
                groupsMap.get(value.getDisplayGroup()).add(new SelectItem(value, value.getLocaleStrValue()));
            } else {
                itemsWithoutGroup.add(new SelectItem(value, value.getLocaleStrValue()));
            }
        }

        for (String groupName : groupsMap.keySet()) {
            String groupLabel = BundleUtil.getStringFromNonDefaultBundle("controlledvocabulary." + getName() + "." + groupName,
                    getMetadataBlock().getName());

            SelectItemGroup selectItemGroup = new SelectItemGroup(groupLabel);
            List<SelectItem> selectItems = groupsMap.get(groupName);

            if (withLocaleSorting) {
                selectItems.sort((i1, i2) -> i1.getLabel().compareToIgnoreCase(i2.getLabel()));
            }

            selectItemGroup.setSelectItems(groupsMap.get(groupName).toArray(new SelectItem[0]));
            groupedList.add(selectItemGroup);
        }
        if (withLocaleSorting) {
            itemsWithoutGroup.sort((i1, i2) -> i1.getLabel().compareToIgnoreCase(i2.getLabel()));
        }
        groupedList.addAll(itemsWithoutGroup);

        return groupedList;
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
            for (DatasetFieldType childFieldType : this.childDatasetFieldTypes) {
                if (childFieldType.isRequired()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isHasParent() {
        return this.parentDatasetFieldType != null;
    }

    @Override
    public int compareTo(DatasetFieldType o) {
        return Integer.compare(this.getDisplayOrder(), (o.getDisplayOrder()));
    }

    public String getDisplayName() {
        if (isHasParent() && !parentDatasetFieldType.getTitle().equals(title)) {
            return Optional.ofNullable(getLocaleTitleWithParent())
                    .filter(title -> !title.isEmpty())
                    .orElse(parentDatasetFieldType.getLocaleTitle()
                            + " " + getLocaleTitle());
        } else {
            return getLocaleTitle();
        }
    }

    public String getLocaleTitle() {
        if (getMetadataBlock() == null) {
            return title;
        } else {
            String key = "datasetfieldtype." + getName() + ".title";
            String bundleName = getMetadataBlock().getName();
            String localeTitle = BundleUtil.getStringFromNonDefaultBundle(key, bundleName);
            return localeTitle.isEmpty() ? title : localeTitle;
        }
    }

    public String getLocaleDescription() {
        if (getMetadataBlock() == null || StringUtils.isEmpty(description)) {
            return description;
        } else {
            String localeDescription = BundleUtil.getStringFromNonDefaultBundle(
                    "datasetfieldtype." + getName() + ".description", getMetadataBlock().getName());
            return localeDescription.isEmpty() ? description : localeDescription;
        }
    }

    public String getLocaleWatermark() {
        if (getMetadataBlock() == null) {
            return watermark;
        } else {
            String localeWatermark = BundleUtil.getStringFromNonDefaultBundle(
                    "datasetfieldtype." + getName() + ".watermark", getMetadataBlock().getName());
            return localeWatermark.isEmpty() ? watermark : localeWatermark;
        }
    }

    /**
     * Determinate if this DatasetFieldType or it's parent allows multiple values for solr field.
     */
    public boolean isThisOrParentAllowsMultipleValues() {
        return allowMultiples || isParentAllowsMutlipleValues();
    }

    // -------------------- PRIVATE --------------------

    private String getLocaleTitleWithParent() {
        try {
            String key = "datasetfieldtype." + getName() + ".withParent.title";
            String bundleName = getMetadataBlock().getName();
            return BundleUtil.getStringFromNonDefaultBundle(key, bundleName);
        } catch (NullPointerException e) {
            return StringUtils.EMPTY;
        }
    }

    private boolean isParentAllowsMutlipleValues() {
        return getParentDatasetFieldType() != null &&
                getParentDatasetFieldType().isAllowMultiples();
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setRequiredInDataverse(boolean requiredInDataverse) {
        this.requiredInDataverse = requiredInDataverse;
    }

    public void setDisplayFormat(String displayFormat) {
        this.displayFormat = displayFormat;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAllowControlledVocabulary(boolean allowControlledVocabulary) {
        this.allowControlledVocabulary = allowControlledVocabulary;
    }

    public void setAllowMultiples(boolean allowMultiples) {
        this.allowMultiples = allowMultiples;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public void setFacetable(boolean facetable) {
        this.facetable = facetable;
    }

    public void setDisplayOnCreate(boolean displayOnCreate) {
        this.displayOnCreate = displayOnCreate;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setMetadataBlock(MetadataBlock metadataBlock) {
        this.metadataBlock = metadataBlock;
    }

    public void setControlledVocabularyValues(Collection<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }

    public void setChildDatasetFieldTypes(Collection<DatasetFieldType> childDatasetFieldTypes) {
        this.childDatasetFieldTypes = new ArrayList<>(childDatasetFieldTypes);
    }

    public void setParentDatasetFieldType(DatasetFieldType parentDatasetFieldType) {
        this.parentDatasetFieldType = parentDatasetFieldType;
    }

    public void setDataverseFacets(Set<DataverseFacet> dataverseFacets) {
        this.dataverseFacets = dataverseFacets;
    }

    public void setDataverseFieldTypeInputLevels(Set<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels) {
        this.dataverseFieldTypeInputLevels = dataverseFieldTypeInputLevels;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setAdvancedSearchFieldType(boolean advancedSearchFieldType) {
        this.advancedSearchFieldType = advancedSearchFieldType;
    }

    public void setDatasetFieldDefaultValues(List<DatasetFieldDefaultValue> datasetFieldDefaultValues) {
        this.datasetFieldDefaultValues = datasetFieldDefaultValues;
    }

    public void setInputRendererType(InputRendererType inputRendererType) {
        this.inputRendererType = inputRendererType;
    }

    public void setInputRendererOptions(String inputRendererOptions) {
        this.inputRendererOptions = inputRendererOptions;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    // -------------------- hashCode & equals --------------------

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
        return Objects.equals(this.id, other.id);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[DatasetFieldType name:" + getName() + " id:" + getId() + "]";
    }
}
