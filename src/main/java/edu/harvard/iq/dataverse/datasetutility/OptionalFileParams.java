/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import edu.harvard.iq.dataverse.DataFileTag;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
    public static final String DESCRIPTION_ATTR_NAME = "description";

    private List<String> tags;
    public static final String TAGS_ATTR_NAME = "tags";
    
    private List<String> tabularTags;
    public static final String TABULAR_TAGS_ATTR_NAME = "tabularTags";


    
    
    public OptionalFileParams(String jsonData) throws DataFileTagException{
        
        if (jsonData != null){
            loadParamsFromJson(jsonData);
        }
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

    private void loadParamsFromJson(String jsonData) throws DataFileTagException{
        
        if (jsonData == null){
//            logger.log(Level.SEVERE, "jsonData is null");
        }
        JsonObject jsonObj = new Gson().fromJson(jsonData, JsonObject.class);
        

        // -------------------------------
        // get description as string
        // -------------------------------
        if ((jsonObj.has(DESCRIPTION_ATTR_NAME)) && (!jsonObj.get(DESCRIPTION_ATTR_NAME).isJsonNull())){
            
            this.description = jsonObj.get(DESCRIPTION_ATTR_NAME).getAsString();
        }
        
        
        // -------------------------------
        // get tags 
        // -------------------------------
        Gson gson = new Gson();
        
        //Type objType = new TypeToken<List<String[]>>() {}.getType();
        Type listType = new TypeToken<List<String>>() {}.getType();
        
        // Load tags
        if ((jsonObj.has(TAGS_ATTR_NAME)) && (!jsonObj.get(TAGS_ATTR_NAME).isJsonNull())){

            this.tags = gson.fromJson(jsonObj.get(TAGS_ATTR_NAME), listType);    
        }

        // Load tabular tags
        if ((jsonObj.has(TABULAR_TAGS_ATTR_NAME)) && (!jsonObj.get(TABULAR_TAGS_ATTR_NAME).isJsonNull())){
            
            // Make a new list
            this.tabularTags = new ArrayList<>();
            
            // Get potential tags from JSON
            List<String> potentialTags = gson.fromJson(jsonObj.get(TABULAR_TAGS_ATTR_NAME), listType); 

            // Add valid potential tags to the list
            for (String tagToCheck : potentialTags){
                if (DataFileTag.isDataFileTag(tagToCheck)){
                    this.tabularTags.add(tagToCheck);
                }else{                    
                    throw new DataFileTagException("Not a valid Tabular Tag: [" + tagToCheck + "]. Please use one of the following: " + DataFileTag.getListofLabelsAsString());
                }
            }
            
            // Shouldn't happen....
            if (tabularTags.isEmpty()){
                tabularTags = null;
            }
        }
       
    }
    
    private void msg(String s){
            System.out.println(s);
    }

    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
        
}
