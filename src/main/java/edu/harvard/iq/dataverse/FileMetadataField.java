/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
/*
 * FileMetadataField.java
 *
 * Taken virtually unchanged from DVN 3.*;
 * Originally created in Feb. 2013
 *
 */
package edu.harvard.iq.dataverse;

/**
 *
 * @author Leonid Andreev
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

/**
 *
 * @author Leonid Andreev
 */

// TODO: update the comment below, to reflect the object name changes in
// 4.0: 

// This is the studyfile-level equivalent of the StudyField table; this will 
// store metadata fields associated with study files. 
// For consistency with the StudyField and StudyFieldValue, I could have called 
// it "StudyFileField"; but decided to go wtih "FileMetadataField" and 
// "FileMetadataFieldValue", to have the names that are more descriptive. 


@Entity
public class FileMetadataField implements Serializable {
    /**
     * Properties: 
     * ==========
     */
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name="name", columnDefinition="TEXT")
    private String name;    // This is the internal, DDI-like name, no spaces, etc.
    @Column(name="title", columnDefinition="TEXT")
    private String title;   // A longer, human-friendlier name - punctuation allowed
    @Column(name="description", columnDefinition="TEXT")
    private String description; // A user-friendly Description; will be used for 
                                // mouse-overs, etc. 
    
    // TODO: 
    // decide if we even need this "custom field" flag; since all the file-level
    // fields are going to be custom. 
    // On the other hand, we may want to add a set of standard file-level fields 
    // - something like "author", "date" and "keyword" maybe? - General enough
    // attributes that can be associated with any document. 
    
    private boolean customField; 
    private boolean basicSearchField;
    private boolean advancedSearchField;
    private boolean searchResultField;
    private boolean prefixSearchable;
   
    private String fileFormatName;
    
    private int displayOrder;
    
    /**
     * Constructors: 
     * ============
     */
   
    /** Creates a new instance of FileMetadataField */
    public FileMetadataField() {
    }

    
    /**
     * Getters and Setters:
     * ===================
     */
    
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public boolean isCustomField() {
        return customField;
    }

    public void setCustomField(boolean customField) {
        this.customField = customField;
    }
   
    public String getFileFormatName() {
        return fileFormatName;
    }

    public void setFileFormatName(String fileFormatName) {
        this.fileFormatName = fileFormatName;
    }

    public boolean isBasicSearchField() {
        return this.basicSearchField;
    }

    public void setBasicSearchField(boolean basicSearchField) {
        this.basicSearchField = basicSearchField;
    }

    public boolean isAdvancedSearchField() {
        return this.advancedSearchField;
    }

    public void setAdvancedSearchField(boolean advancedSearchField) {
        this.advancedSearchField = advancedSearchField;
    }

    public boolean isSearchResultField() {
        return this.searchResultField;
    }

    public void setSearchResultField(boolean searchResultField) {
        this.searchResultField = searchResultField;
    }


    public boolean isPrefixSearchable() {
        return this.prefixSearchable;
    }

    public void setPrefixSearchable(boolean prefixSearchale) {
        this.prefixSearchable = prefixSearchable;
    }
          
    /**
     * Helper methods:
     * ==============
     * 
     */
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FileMetadataField)) {
            return false;
        }
        FileMetadataField other = (FileMetadataField)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }      
    
}
