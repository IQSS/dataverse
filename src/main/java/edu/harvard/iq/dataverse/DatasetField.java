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
 * @author Ellen Kraffmiller
 */
@Entity
public class DatasetField implements Serializable {
    @Column(name="name", columnDefinition="TEXT")
    private String name;    // This is the internal, DDI-like name, no spaces, etc.
    @Column(name="title", columnDefinition="TEXT")
    private String title;   // A longer, human-friendlier name - punctuation allowed
    @Column(name="description", columnDefinition="TEXT")
    private String description; // A user-friendly Description; will be used for 
                                // mouse-overs, etc. 
    private boolean customField;
    private String fieldType;
    private boolean allowControlledVocabulary;
    
   
    /** Creates a new instance of DatasetField */
    public DatasetField() {
    }

    /**
     * Holds value of property displayOrder.
     */
    private int displayOrder;

    /**
     * Getter for property order.
     * @return Value of property order.
     */
    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * Setter for property order.
     * @param order New value of property order.
     */
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
    
    public boolean isCustomField() {
        return customField;
    }

    public void setCustomField(boolean customField) {
        this.customField = customField;
    }
    
    public boolean isAllowControlledVocabulary() {
        return allowControlledVocabulary;
    }

    public void setAllowControlledVocabulary(boolean allowControlledVocabulary) {
        this.allowControlledVocabulary = allowControlledVocabulary;
    }
        
    /**
     * Holds value of property allow multiples.
     */
    private boolean allowMultiples; 

    /**
     * Getter for property allow multiples.
     * @return Value of property allow multiples.
     */
    public boolean isAllowMultiples() {
        return this.allowMultiples;
    }

    /**
     * Setter for property allow multiples.
     * @param version New value of property allow multiples.
     */
    public void setAllowMultiples(boolean allowMultiples) {
        this.allowMultiples = allowMultiples;
    }

     public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    /**
     * Holds value of property id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Getter for property id.
     * @return Value of property id.
     */
    public Long getId() {
        return this.id;
    }
    
    

    /**
     * Setter for property id.
     * @param id New value of property id.
     */
    public void setId(Long id) {
        this.id = id;
    }

     /**
     * Holds value of metadata domain
     */
    @ManyToMany (mappedBy="datasetFields",cascade={CascadeType.PERSIST } )
    private Collection<MetadataDomain> metadataDomains;
    
    public Collection<MetadataDomain> getMetadataDomains() {
        return metadataDomains;
    }

    public void setMetadataDomains(Collection<MetadataDomain> metadataDomains) {
        this.metadataDomains = metadataDomains;
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

    @OneToMany(mappedBy="datasetField")
    private Collection <TemplateField> templateFields;

    /**
     * Holds value of property basicSearchField.
     */
    private boolean basicSearchField;

    /**
     * Getter for property basicSearchField.
     * @return Value of property basicSearchField.
     */
    public boolean isBasicSearchField() {
        return this.basicSearchField;
    }

    /**
     * Setter for property basicSearchField.
     * @param basicSearchField New value of property basicSearchField.
     */
    public void setBasicSearchField(boolean basicSearchField) {
        this.basicSearchField = basicSearchField;
    }

    /**
     * Holds value of property advancedSearchField.
     */
    private boolean advancedSearchField;

    /**
     * Getter for property advancedSearchField.
     * @return Value of property advancedSearchField.
     */
    public boolean isAdvancedSearchField() {
        return this.advancedSearchField;
    }

    /**
     * Setter for property advancedSearchField.
     * @param advancedSearchField New value of property advancedSearchField.
     */
    public void setAdvancedSearchField(boolean advancedSearchField) {
        this.advancedSearchField = advancedSearchField;
    }

    /**
     * Holds value of property searchResultField.
     */
    private boolean searchResultField;

    /**
     * Getter for property searchResultField.
     * @return Value of property searchResultField.
     */
    public boolean isSearchResultField() {
        return this.searchResultField;
    }

    /**
     * Setter for property searchResultField.
     * @param searchResultField New value of property searchResultField.
     */
    public void setSearchResultField(boolean searchResultField) {
        this.searchResultField = searchResultField;
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
    
      
}
