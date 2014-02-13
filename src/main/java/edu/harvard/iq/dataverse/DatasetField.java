/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

/**
 *
 * @author Stephen Kraffmiller
 */
@Entity
public class DatasetField implements Serializable,   Comparable<DatasetField> {
        
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    
    @Column(name="name", columnDefinition="TEXT")
    private String name;    // This is the internal, DDI-like name, no spaces, etc.
    @Column(name="title", columnDefinition="TEXT")
    private String title;   // A longer, human-friendlier name - punctuation allowed
    @Column(name="description", columnDefinition="TEXT")
    private String description; // A user-friendly Description; will be used for 
                                // mouse-overs, etc. 
    private String fieldType;
    private boolean allowControlledVocabulary;
    
    @Transient
    private String searchValue;
    
    public DatasetField() {
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


    @ManyToOne(cascade = CascadeType.MERGE) 
    private MetadataBlock metadataBlock;   
    public MetadataBlock getMetadataBlock() {
        return metadataBlock;
    }
    public void setMetadataBlock(MetadataBlock metadataBlock) {
        this.metadataBlock = metadataBlock;
    }
    
    @OneToMany(mappedBy = "parentDatasetField", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private Collection<DatasetField> childDatasetFields;
    public Collection<DatasetField> getChildDatasetFields() {
        return this.childDatasetFields;
    }
    public void setChildDatasetFields(Collection<DatasetField> childDatasetFields) {
        this.childDatasetFields = childDatasetFields;
    }
    
    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetField parentDatasetField;
    public DatasetField getParentDatasetField() {
        return parentDatasetField;
    }
    public void setParentDatasetField(DatasetField parentDatasetField) {
        this.parentDatasetField = parentDatasetField;
    }
    
    
    /**
     * Holds value of property studies. 
     */
    /*
    @ManyToMany(mappedBy="summaryFields",cascade={CascadeType.REMOVE })
    private Collection<Study> studies;

    @ManyToMany(mappedBy="advSearchFields",cascade={CascadeType.REMOVE })
    private Collection<VDC> advSearchFieldVDCs;

   @ManyToMany(mappedBy="searchResultFields",cascade={CascadeType.REMOVE })
    private Collection<VDC> searchResultFieldVDCs;
   
   @ManyToMany(mappedBy="anySearchFields",cascade={CascadeType.REMOVE })
    private Collection<VDC> anySearchFieldVDCs;
    
   @ManyToMany(mappedBy="summaryFields",cascade={CascadeType.REMOVE })
    private Collection<VDC> summaryFieldVDCs;
*/
    public String getSearchValue() {
        return searchValue;
    }
   
    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    private boolean required;
    public boolean isRequired() {
        return this.required;
    }
    public void setRequired(boolean required) {
        this.required = required;
    }

    private boolean basicSearchField;
    public boolean isBasicSearchField() {
        return this.basicSearchField;
    }
    public void setBasicSearchField(boolean basicSearchField) {
        this.basicSearchField = basicSearchField;
    }

    private boolean advancedSearchField;
    public boolean isAdvancedSearchField() {
        return this.advancedSearchField;
    }
    public void setAdvancedSearchField(boolean advancedSearchField) {
        this.advancedSearchField = advancedSearchField;
    }

    private boolean searchResultField;
    public boolean isSearchResultField() {
        return this.searchResultField;
    }
    public void setSearchResultField(boolean searchResultField) {
        this.searchResultField = searchResultField;
    }
    
    public boolean isHasChildren(){
        return !this.childDatasetFields.isEmpty();
    }
    
    public boolean isHasParent(){
        return this.parentDatasetField != null;
    }
    
 public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetField)) {
            return false;
        }
        DatasetField other = (DatasetField)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }      
    

    @OneToMany (mappedBy="datasetField",  cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    private List<DatasetFieldValue> datasetFieldValues;

    public List<DatasetFieldValue> getDatasetFieldValues() {
        return datasetFieldValues;
    }  
       
    public void setDatasetFieldValues(List<DatasetFieldValue> datasetFieldValues) {
        this.datasetFieldValues = datasetFieldValues;
    } 
    
    @OneToMany (mappedBy="datasetField",  cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
    private List<DatasetFieldDefaultValue> datasetFieldDefaultValues;

    public List<DatasetFieldDefaultValue> getDatasetFieldDefaultValues() {
        return datasetFieldDefaultValues;
    }  
       
    public void setDatasetFieldDefaultValues(List<DatasetFieldDefaultValue> datasetFieldDefaultValues) {
        this.datasetFieldDefaultValues = datasetFieldDefaultValues;
    } 
    
    // helper methods for getting the internal string values
    public List<String> getDatasetFieldValueStrings() {
        List <String> retString = new ArrayList();
        for (DatasetFieldValue sfv:datasetFieldValues){
            String testString = sfv.getStrValue();
            if (!testString.isEmpty()) {
                retString.add(sfv.getStrValue());
            }
        }
        return retString;
    }
    
    public String getDatasetFieldValueSingleString() {
        return datasetFieldValues.size() > 0 ? datasetFieldValues.get(0).getStrValue() : "";
    }
    
    public void setDatasetFieldValueStrings(List<String> newValList) {}

    public void setDatasetFieldValueSingleString(String newVal) {}
    
    @Override
    public int compareTo(DatasetField o) {
        return Integer.compare(this.getDisplayOrder(),(o.getDisplayOrder()));        
    }       
}
