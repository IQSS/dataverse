package edu.harvard.iq.dataverse;

import java.util.Collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.faces.model.SelectItem;
import javax.persistence.*;

/**
 *
 * @author Stephen Kraffmiller
 */
@NamedQueries({
        @NamedQuery(name="DatasetFieldType.findByName",
                            query= "SELECT dsfType FROM DatasetFieldType dsfType WHERE dsfType.name=:name"),
	@NamedQuery(name = "DatasetFieldType.findAllFacetable",
			    query= "select dsfType from DatasetFieldType dsfType WHERE dsfType.facetable = true and dsfType.title != '' order by dsfType.id"),
        @NamedQuery(name = "DatasetFieldType.findFacetableByMetadaBlock",
			    query= "select dsfType from DatasetFieldType dsfType WHERE dsfType.facetable = true and dsfType.title != '' and dsfType.metadataBlock.id = :metadataBlockId order by dsfType.id")
})
@Entity
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
    
    public String getIdString(){
        return id.toString();
    }

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;    // This is the internal, DDI-like name, no spaces, etc.
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;   // A longer, human-friendlier name - punctuation allowed
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // A user-friendly Description; will be used for 
    // mouse-overs, etc. 
    private String fieldType;
    private boolean allowControlledVocabulary;
    private String watermark;

    @OneToMany(mappedBy = "datasetFieldType")
    private Set<DataverseFacet> dataverseFacets;
    
    @OneToMany(mappedBy = "datasetFieldType")
    private Set<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels;
    
    @Transient
    private String searchValue;
    
    @Transient
    private List<String> listValues;

    @Transient
    private Map<String, ControlledVocabularyValue> controlledVocabularyValuesByStrValue;
    
    @Transient 
    private boolean requiredDV;
    
    public void setRequiredDV(boolean requiredDV){
        this.requiredDV = requiredDV;
    }
    
    public boolean isRequiredDV(){
        return this.requiredDV;
    }
    
    @Transient 
    private boolean include;
    
    public void setInclude(boolean include){
        this.include = include;
    }
    
    public boolean isInclude(){
        return this.include;
    }
    
    @Transient 
    private List<SelectItem> optionSelectItems;

    public List<SelectItem> getOptionSelectItems() {
        return optionSelectItems;
    }

    public void setOptionSelectItems(List<SelectItem> optionSelectItems) {
        this.optionSelectItems = optionSelectItems;
    }
    
    
    

    
    public DatasetFieldType() {}

    public DatasetFieldType(String name, String fieldType, boolean allowMultiples) {
        this.name = name;
        this.fieldType = fieldType;
        this.allowMultiples = allowMultiples;
        childDatasetFieldTypes = new LinkedList<>();
    }
    
    private int displayOrder;

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
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

    private boolean allowMultiples;

    public boolean isAllowMultiples() {
        return this.allowMultiples;
    }

    public void setAllowMultiples(boolean allowMultiples) {
        this.allowMultiples = allowMultiples;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }
    
    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }
    private boolean facetable;

    public boolean isFacetable() {
        return facetable;
    }

    public void setFacetable(boolean facetable) {
        this.facetable = facetable;
    }

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

    @ManyToOne(cascade = CascadeType.MERGE)
    private MetadataBlock metadataBlock;

    public MetadataBlock getMetadataBlock() {
        return metadataBlock;
    }

    public void setMetadataBlock(MetadataBlock metadataBlock) {
        this.metadataBlock = metadataBlock;
    }
    
   @OneToMany(mappedBy = "datasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
   @OrderBy("displayOrder ASC")
    private Collection<ControlledVocabularyValue> controlledVocabularyValues;

    public Collection<ControlledVocabularyValue> getControlledVocabularyValues() {
        return this.controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(Collection<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }
    
    public ControlledVocabularyValue getControlledVocabularyValue( String strValue ) {
        if ( ! isControlledVocabulary() ) {
            throw new IllegalStateException("getControlledVocabularyValue() called on a non-controlled vocabulary type.");
        }
        if ( controlledVocabularyValuesByStrValue == null ) {
            controlledVocabularyValuesByStrValue = new TreeMap<>();               
            for ( ControlledVocabularyValue cvv : getControlledVocabularyValues() ) {
                controlledVocabularyValuesByStrValue.put( cvv.getStrValue(), cvv);
            }
        }
        return controlledVocabularyValuesByStrValue.get(strValue);
    }
       

    @OneToMany(mappedBy = "parentDatasetFieldType", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private Collection<DatasetFieldType> childDatasetFieldTypes;

    public Collection<DatasetFieldType> getChildDatasetFieldTypes() {
        return this.childDatasetFieldTypes;
    }

    public void setChildDatasetFieldTypes(Collection<DatasetFieldType> childDatasetFieldTypes) {
        this.childDatasetFieldTypes = childDatasetFieldTypes;
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

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public List<String> getListValues() {
        return listValues;
    }

    public void setListValues(List<String> listValues) {
        this.listValues = listValues;
    }
    
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

    public boolean isPrimitive() {
        return this.childDatasetFieldTypes.isEmpty();
    }
    
    public boolean isCompound() {
         return !this.childDatasetFieldTypes.isEmpty();       
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
        if (this.childDatasetFieldTypes.isEmpty()){
            return false;
        } else {
            for (DatasetFieldType dsftC : this.childDatasetFieldTypes){
                if (dsftC.isRequired()) return true;
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
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

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
        return parentDatasetFieldType.getTitle() + " " + title;
        } else {
            return title;
        }
    }

    public SolrField getSolrField() {
        SolrField.SolrType solrType = SolrField.SolrType.TEXT_EN;
        if (fieldType != null) {

            /**
             * @todo made more decisions based on fieldType: index as dates,
             * integers, and floats so we can do range queries etc.
             */
            if (fieldType.equals("date")) {
                solrType = SolrField.SolrType.DATE;
            } else if (fieldType.equals("email")) {
                solrType = SolrField.SolrType.EMAIL;
            }

            Boolean parentAllowsMultiplesBoolean = false;
            if (isHasParent()) {
                if (getParentDatasetFieldType() != null) {
                    DatasetFieldType parent = getParentDatasetFieldType();
                    parentAllowsMultiplesBoolean = parent.isAllowMultiples();
                }
            }
            
            boolean makeSolrFieldMultivalued;
            // http://stackoverflow.com/questions/5800762/what-is-the-use-of-multivalued-field-type-in-solr
            if (allowMultiples || parentAllowsMultiplesBoolean) {
                makeSolrFieldMultivalued = true;
            } else {
                makeSolrFieldMultivalued = false;
            }

            return new SolrField(name, solrType, makeSolrFieldMultivalued, facetable);

        } else {
            /**
             * @todo: clean this up
             */
            String oddValue = name + getTmpNullFieldTypeIdentifier();
            boolean makeSolrFieldMultivalued = false;
            SolrField solrField = new SolrField(oddValue, solrType, makeSolrFieldMultivalued, facetable);
            return solrField;
        }
    }

    // help us identify fields that have null fieldType values
    public String getTmpNullFieldTypeIdentifier() {
        return "NullFieldType_s";
    }
    
    @Override
    public String toString() {
        return "[DatasetFieldType name:" + getName() + " id:" + getId() + "]";
    }
}
