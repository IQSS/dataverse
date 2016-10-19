/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.FileMetadata;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    
    private List<String> fileDataTags;
    public static final String FILE_DATA_TAGS_ATTR_NAME = "fileDataTags";


    
    
    public OptionalFileParams(String jsonData) throws DataFileTagException{
        
        if (jsonData != null){
            loadParamsFromJson(jsonData);
        }
    }

    
    public OptionalFileParams(String description,
                    List<String> newTags, 
                    List<String> potentialFileDataTags)  throws DataFileTagException{
        
        this.description = description;
        setTags(newTags);
        this.addFileDataTags(potentialFileDataTags);
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
    
    public boolean hasTags(){
        if ((tags == null)||(this.tags.isEmpty())){
            return false;
        }
        return true;
    }
 
    public boolean hasFileDataTags(){
        if ((fileDataTags == null)||(this.fileDataTags.isEmpty())){
            return false;
        }
        return true;
    }
 
    public boolean hasDescription(){
        if ((description == null)||(this.description.isEmpty())){
            return false;
        }
        return true;
    }

    /**
     *  Set tags
     *  @param tags
     */
    public void setTags(List<String> newTags){
        
        if (newTags != null){
            newTags = removeDuplicatesNullsEmptyStrings(newTags);
            if (newTags.isEmpty()){
                newTags = null;
            }
        }
        
        
        this.tags = newTags;
    }

    /**
     *  Get for tags
     *  @return List<String>
     */
    public List<String> getTags(){
        return this.tags;
    }
    

    /**
     *  Set fileDataTags
     *  @param fileDataTags
     */
    public void setFileDataTags(List<String> fileDataTags){
        this.fileDataTags = fileDataTags;
    }

    /**
     *  Get for dataFileTags
     *  @return List<String>
     */
    public List<String> getFileDataTags(){
        return this.fileDataTags;
    }

    private void loadParamsFromJson(String jsonData) throws DataFileTagException{
        
        msgt("jsonData: " +  jsonData);
        if (jsonData == null){
            return;
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

            setTags(this.tags = gson.fromJson(jsonObj.get(TAGS_ATTR_NAME), listType));
        }

        // Load tabular tags
        if ((jsonObj.has(FILE_DATA_TAGS_ATTR_NAME)) && (!jsonObj.get(FILE_DATA_TAGS_ATTR_NAME).isJsonNull())){
            
            
            // Get potential tags from JSON
            List<String> potentialTags = gson.fromJson(jsonObj.get(FILE_DATA_TAGS_ATTR_NAME), listType); 

            // Add valid potential tags to the list
            addFileDataTags(potentialTags);            
           
        }
       
    }
    
    private List<String> removeDuplicatesNullsEmptyStrings(List<String> tagsToCheck){
        
        if (tagsToCheck == null){
            throw new NullPointerException("tagsToCheck cannot be null");
        }
             
        return tagsToCheck.stream()
                        .filter(p -> p != null)         // no nulls
                        .map(String :: trim)            // strip strings
                        .filter(p -> p.length() > 0 )   // no empty strings
                        .distinct()                     // distinct
                        .collect(Collectors.toList());
      
    }
    
    
    private void addFileDataTags(List<String> potentialTags) throws DataFileTagException{
        
        if (potentialTags == null){
            return;
        }
                
        potentialTags = removeDuplicatesNullsEmptyStrings(potentialTags);
                
        if (potentialTags.isEmpty()){
            return;
        }
        
         // Make a new list
        this.fileDataTags = new ArrayList<>();
           
        // Add valid potential tags to the list
        for (String tagToCheck : potentialTags){
            if (DataFileTag.isDataFileTag(tagToCheck)){
                this.fileDataTags.add(tagToCheck);
            }else{                    
                String errMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.invalid_datafile_tag");
                throw new DataFileTagException(errMsg + " [" + tagToCheck + "]. Please use one of the following: " + DataFileTag.getListofLabelsAsString());
            }
        }
         // Shouldn't happen....
         if (fileDataTags.isEmpty()){
            fileDataTags = null;
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

    /** 
     * Add parameters to a DataFile object
     * 
     */
    public void addOptionalParams(DataFile df) {
        if (df == null){            
            throw new NullPointerException("The datafile cannot be null!");
        }
        
        FileMetadata fm = df.getFileMetadata();
        
        // ---------------------------
        // Add description
        // ---------------------------
        if (hasDescription()){
            fm.setDescription(this.getDescription());
        }
        
        // ---------------------------
        // Add tags
        // ---------------------------
        addTagsToDataFile(fm);
       

        // ---------------------------
        // Add DataFileTags
        // ---------------------------
        addFileDataTagsToFile(df);
       
    }
    

    /**
     *  Add Tags to the DataFile
     * 
     */
    private void addTagsToDataFile(FileMetadata fileMetadata){
        
        if (fileMetadata == null){            
            throw new NullPointerException("The fileMetadata cannot be null!");
        }
        
        // Is there anything to add?
        //
        if (!hasTags()){
            return;
        }
        
        List<String> currentCategories = fileMetadata.getCategoriesByName();
        for (String tagText : this.getTags()){               
            if (!currentCategories.contains(tagText)){
                fileMetadata.addCategoryByName(tagText);
            }
        }
    }
    
    
    private void addFileDataTagsToFile(DataFile df){
        if (df == null){
            throw new NullPointerException("The DataFile (df) cannot be null!");
        }
        msgt("addFileDataTagsToFile");
        
        // Is there anything to add?
        if (!hasFileDataTags()){
            return;
        }
        msgt("addFileDataTagsToFile 2");
        
        // Get existing tag list and convert it to list of strings
        List<DataFileTag> existingDataFileTags = df.getTags();
        List<String> currentLabels;

        if (existingDataFileTags == null){
            // nothing, just make an empty list
            currentLabels = new ArrayList<>();
        }else{            
            // Yes, get the labels in a list
            currentLabels = df.getTags().stream()
                                        .map(x -> x.getTypeLabel())
                                        .collect(Collectors.toList())
                                       ;
        }

        // Iterate through and add any new labels
        //
        DataFileTag newTagObj;
        for (String tagLabel : this.getFileDataTags()){    

            if (!currentLabels.contains(tagLabel)){     // not  already there!

                // redundant "if" check here.  Also done in constructor
                //
                if (DataFileTag.isDataFileTag(tagLabel)){

                    newTagObj = new DataFileTag();
                    newTagObj.setDataFile(df);
                    newTagObj.setTypeByLabel(tagLabel);
                    df.addTag(newTagObj);

                }
            }
        }                
        
    }
        
}
