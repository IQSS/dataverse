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
 * TemplateField.java
 *
 * Created on August 2, 2006, 4:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;


import javax.persistence.*;

/**
 *
 * @author Ellen Kraffmiller
 */
@Entity
public class TemplateField implements java.io.Serializable {


    /**
     * Creates a new instance of TemplateField
     */
    public TemplateField() {
    }

    
    private String defaultValue;

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    private String fieldInputLevelString;

    public String getFieldInputLevelString() {
        return fieldInputLevelString;
    }

    public void setFieldInputLevelString(String fieldInputLevelString) {
        this.fieldInputLevelString = fieldInputLevelString;
    }

    /**
     * Holds value of property template.
     */
    @ManyToOne
    @JoinColumn(nullable=false)
    private Template template;

    /**
     * Getter for property template.
     * @return Value of property template.
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Setter for property template.
     * @param template New value of property template.
     */
    public void setTemplate(Template template) {
        this.template=template;
    }

    /*
    @ManyToOne
    @JoinColumn
    private ControlledVocabulary controlledVocabulary;

    public ControlledVocabulary getControlledVocabulary() { return controlledVocabulary; }
    public void setControlledVocabulary(ControlledVocabulary controlledVocabulary) { this.controlledVocabulary=controlledVocabulary; }    
    
      */  
    
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
     * Holds value of property datasetField.
     */
    @ManyToOne
    @JoinColumn(nullable=false, insertable = true)
    private DatasetFieldType datasetField;

    public DatasetFieldType getDatasetField() {
        return this.datasetField;
    }

    public void setDatasetField(DatasetFieldType datasetField) {
        this.datasetField = datasetField;
    }


  /**
     * Holds value of property version.
     */
    @Version
    private Long version; 

    /**
     * Getter for property version.
     * @return Value of property version.
     */
    public Long getVersion() {
        return this.version;
    }

    /**
     * Setter for property version.
     * @param version New value of property version.
     */
    public void setVersion(Long version) {
        this.version = version;
    }
    

    public boolean isAllowMultiples() {
        return this.datasetField.isAllowMultiples();
    }

    private int displayOrder; 

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public boolean isRequired() {
        return fieldInputLevelString.equals("required"); 
    }

    public boolean isRecommended() {
        return fieldInputLevelString.equals("recommended"); 
    }
    
    public boolean isOptional() {
        return fieldInputLevelString.equals("optional"); 
    }
    
    public boolean isHidden() {
        return fieldInputLevelString.equals("hidden"); 
    }
    
    public boolean isDisabled() {
        return fieldInputLevelString.equals("disabled"); 
    }
    
   public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TemplateField)) {
            return false;
        }
        TemplateField other = (TemplateField)object;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) return false;
        return true;
    }            

}

