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
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(OptionalFileParams.class.getName());

    private String description;
    public static final String DESCRIPTION_ATTR_NAME = "description";

    private List<String> categories;
    public static final String CATEGORIES_ATTR_NAME = "categories";
    
    private List<String> dataFileTags;
    public static final String FILE_DATA_TAGS_ATTR_NAME = "dataFileTags";
    
    private boolean restrict = false;
    public static final String RESTRICT_ATTR_NAME = "restrict";


     
    public OptionalFileParams(String jsonData) throws DataFileTagException{
        
        if (jsonData != null){
            loadParamsFromJson(jsonData);
        }
    }

    
    public OptionalFileParams(String description,
                    List<String> newCategories, 
                    List<String> potentialFileDataTags)  throws DataFileTagException{
        
        this.description = description;
        setCategories(newCategories);
        this.addFileDataTags(potentialFileDataTags);
    }


    
    public OptionalFileParams(String description,
            List<String> newCategories,
            List<String> potentialFileDataTags, 
            boolean restrict) throws DataFileTagException {

        this.description = description;
        setCategories(newCategories);
        this.addFileDataTags(potentialFileDataTags);
        this.restrict = restrict;
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
    
    public void setRestriction(boolean restrict){
        this.restrict = restrict;
    }
    
    public boolean getRestriction(){
        return this.restrict;
    }
    
    public boolean hasCategories(){
        if ((categories == null)||(this.categories.isEmpty())){
            return false;
        }
        return true;
    }
 
    public boolean hasFileDataTags(){
        if ((dataFileTags == null)||(this.dataFileTags.isEmpty())){
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
    public void setCategories(List<String> newCategories) {

        if (newCategories != null) {
            newCategories = Util.removeDuplicatesNullsEmptyStrings(newCategories);
            if (newCategories.isEmpty()) {
                newCategories = null;
            }
        }

        this.categories = newCategories;
    }

    /**
     *  Get for tags
     *  @return List<String>
     */
    public List<String> getCategories(){
        return this.categories;
    }
    

    /**
     *  Set dataFileTags
     *  @param dataFileTags
     */
    public void setDataFileTags(List<String> dataFileTags){
        this.dataFileTags = dataFileTags;
    }

    /**
     *  Get for dataFileTags
     *  @return List<String>
     */
    public List<String> getDataFileTags(){
        return this.dataFileTags;
    }

    private void loadParamsFromJson(String jsonData) throws DataFileTagException{
        
        msgt("jsonData: " +  jsonData);
        if (jsonData == null || jsonData.isEmpty()){
            return;
//            logger.log(Level.SEVERE, "jsonData is null");
        }
        JsonObject jsonObj;
        try {
            jsonObj = new Gson().fromJson(jsonData, JsonObject.class);
        } catch (ClassCastException ex) {
            logger.info("Exception parsing string '" + jsonData + "': " + ex);
            return;
        }

        // -------------------------------
        // get description as string
        // -------------------------------
        if ((jsonObj.has(DESCRIPTION_ATTR_NAME)) && (!jsonObj.get(DESCRIPTION_ATTR_NAME).isJsonNull())){
            
            this.description = jsonObj.get(DESCRIPTION_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get restriction as boolean
        // -------------------------------
        if ((jsonObj.has(RESTRICT_ATTR_NAME)) && (!jsonObj.get(RESTRICT_ATTR_NAME).isJsonNull())){
            
            this.restrict = Boolean.valueOf(jsonObj.get(RESTRICT_ATTR_NAME).getAsString());
        }
        
        // -------------------------------
        // get tags 
        // -------------------------------
        Gson gson = new Gson();
        
        //Type objType = new TypeToken<List<String[]>>() {}.getType();
        Type listType = new TypeToken<List<String>>() {}.getType();
        
        //----------------------
        // Load tags
        //----------------------
        if ((jsonObj.has(CATEGORIES_ATTR_NAME)) && (!jsonObj.get(CATEGORIES_ATTR_NAME).isJsonNull())){

            /**
             * @todo Use JsonParser.getCategories somehow instead (refactoring
             * required). This code is exercised by FilesIT.
             */
            setCategories(this.categories = gson.fromJson(jsonObj.get(CATEGORIES_ATTR_NAME), listType));
        }

        //----------------------
        // Load tabular tags
        //----------------------
        if ((jsonObj.has(FILE_DATA_TAGS_ATTR_NAME)) && (!jsonObj.get(FILE_DATA_TAGS_ATTR_NAME).isJsonNull())){
            
            
            // Get potential tags from JSON
            List<String> potentialTags = gson.fromJson(jsonObj.get(FILE_DATA_TAGS_ATTR_NAME), listType); 

            // Add valid potential tags to the list
            addFileDataTags(potentialTags);            
           
        }
       
    }
  
    private void addFileDataTags(List<String> potentialTags) throws DataFileTagException{
        
        if (potentialTags == null){
            return;
        }

        potentialTags = Util.removeDuplicatesNullsEmptyStrings(potentialTags);
                
        if (potentialTags.isEmpty()){
            return;
        }
        
         // Make a new list
        this.dataFileTags = new ArrayList<>();
           
        // Add valid potential tags to the list
        for (String tagToCheck : potentialTags){
            if (DataFileTag.isDataFileTag(tagToCheck)){
                this.dataFileTags.add(tagToCheck);
            }else{                    
                String errMsg = BundleUtil.getStringFromBundle("file.addreplace.error.invalid_datafile_tag");
                throw new DataFileTagException(errMsg + " [" + tagToCheck + "]. Please use one of the following: " + DataFileTag.getListofLabelsAsString());
            }
        }
         // Shouldn't happen....
         if (dataFileTags.isEmpty()){
            dataFileTags = null;
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
    public void addOptionalParams(DataFile df) throws DataFileTagException{
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
        // Add categories
        // ---------------------------
        addCategoriesToDataFile(fm);
       

        // ---------------------------
        // Add DataFileTags
        // ---------------------------
        addFileDataTagsToFile(df);
       
    }
    

    /**
     *  Add Tags to the DataFile
     * 
     */
    private void addCategoriesToDataFile(FileMetadata fileMetadata){
        
        if (fileMetadata == null){            
            throw new NullPointerException("The fileMetadata cannot be null!");
        }
        
        // Is there anything to add?
        //
        if (!hasCategories()){
            return;
        }
        
        List<String> currentCategories = fileMetadata.getCategoriesByName();

        // Add categories to the file metadata object
        //
        this.getCategories().stream().forEach((catText) -> {               
            fileMetadata.addCategoryByName(catText);  // fyi: "addCategoryByName" checks for dupes
        });
    }

    
    /**
     * NOTE: DataFile tags can only be added to tabular files
     * 
     *      - e.g. The file must already be ingested.
     * 
     * Because of this, these tags cannot be used when "Adding" a file via 
     * the API--e.g. b/c the file will note yet be ingested
     * 
     * @param df 
     */
    private void addFileDataTagsToFile(DataFile df) throws DataFileTagException{
        if (df == null){
            throw new NullPointerException("The DataFile (df) cannot be null!");
        }
        
        // --------------------------------------------------
        // Is there anything to add?
        // --------------------------------------------------
        if (!hasFileDataTags()){
            return;
        }
        
        // --------------------------------------------------
        // Is this a tabular file?
        // --------------------------------------------------
        if (!df.isTabularData()){
            String errMsg = BundleUtil.getStringFromBundle("file.metadata.datafiletag.not_tabular");

            throw new DataFileTagException(errMsg);
        }
        
        // --------------------------------------------------
        // Get existing tag list and convert it to list of strings (labels)
        // --------------------------------------------------
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

        // --------------------------------------------------
        // Iterate through and add any new labels
        // --------------------------------------------------
        DataFileTag newTagObj;
        for (String tagLabel : this.getDataFileTags()){    

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
