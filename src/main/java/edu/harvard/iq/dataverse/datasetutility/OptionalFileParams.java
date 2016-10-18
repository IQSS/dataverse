/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import java.util.List;

/**
 * This is used in conjunction with the AddReplaceFileHelper
 * 
 * It encapsulates these optional parameters:
 * 
 *  - description
 *  - file tags (can be custom)
 *  - tabular tags (controlled vocabulary)
 * 
 * Future params:
 *  - Provenance related information
 * 
 * @author rmp553
 */
public class OptionalFileParams {
    
    private String description;

    private List<String> tags;

    private List<String> tabularTags;


    public OptionalFileParams(String description){
        
        this.description = description;
    }

    public OptionalFileParams(String description,
                    List<String> tags){
        
        this.description = description;
        this.tags = tags;
    }

    public OptionalFileParams(String description,
                    List<String> tags, 
                    List<String> tabularTags){
        
        this.description = description;
        this.tags = tags;
        this.tabularTags = tabularTags;
    }

    /**
     *  Set description
     *  @param description
     */
    public void setDescription(String description){
        this.description = description;
    }

    /**
     *  Get for description
     *  @return String
     */
    public String getDescription(){
        return this.description;
    }
    

    /**
     *  Set tags
     *  @param tags
     */
    public void setTags(List<String> tags){
        this.tags = tags;
    }

    /**
     *  Get for tags
     *  @return List<String>
     */
    public List<String> getTags(){
        return this.tags;
    }
    

    /**
     *  Set tabularTags
     *  @param tabularTags
     */
    public void setTabularTags(List<String> tabularTags){
        this.tabularTags = tabularTags;
    }

    /**
     *  Get for tabularTags
     *  @return List<String>
     */
    public List<String> getTabularTags(){
        return this.tabularTags;
    }
}
