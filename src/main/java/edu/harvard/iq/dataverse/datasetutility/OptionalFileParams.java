/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
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
    
    private String label;
    public static final String LABEL_ATTR_NAME = "label";
    
    private String directoryLabel;
    public static final String DIRECTORY_LABEL_ATTR_NAME = "directoryLabel";

    private List<String> categories;
    public static final String CATEGORIES_ATTR_NAME = "categories";
    
    private List<String> dataFileTags;
    public static final String FILE_DATA_TAGS_ATTR_NAME = "dataFileTags";
    
    private String provFreeForm;
    public static final String PROVENANCE_FREEFORM_ATTR_NAME = "provFreeForm";
    
    private boolean restrict = false;
    public static final String RESTRICT_ATTR_NAME = "restrict";
    
    private String storageIdentifier;
    public static final String STORAGE_IDENTIFIER_ATTR_NAME = "storageIdentifier";
    private String fileName;
    public static final String FILE_NAME_ATTR_NAME = "fileName";
    private String mimeType;
    public static final String MIME_TYPE_ATTR_NAME = "mimeType";
    private String checkSumValue;
    private ChecksumType checkSumType;
    public static final String LEGACY_CHECKSUM_ATTR_NAME = "md5Hash";
    public static final String CHECKSUM_OBJECT_NAME = "checksum";
    public static final String CHECKSUM_OBJECT_TYPE = "@type";
    public static final String CHECKSUM_OBJECT_VALUE = "@value";

     
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
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getDirectoryLabel() {
        return directoryLabel;
    }

    public void setDirectoryLabel(String directoryLabel) {
        this.directoryLabel = directoryLabel;
    }
    
    public String getProvFreeform() {
        return provFreeForm;
    }

    public void setProvFreeform(String provFreeForm) {
        this.provFreeForm = provFreeForm;
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

    public boolean hasDirectoryLabel(){
        if ((directoryLabel == null)||(this.directoryLabel.isEmpty())){
            return false;
        }
        return true;
    }
    
    public boolean hasLabel(){
        if ((label == null)||(this.label.isEmpty())){
            return false;
        }
        return true;
    }
    
    public boolean hasProvFreeform(){
        if ((provFreeForm == null)||(this.provFreeForm.isEmpty())){
            return false;
        }
        return true;
    }

	public boolean hasStorageIdentifier() {
		return ((storageIdentifier!=null)&&(!storageIdentifier.isEmpty()));
	}

	public String getStorageIdentifier() {
		return storageIdentifier;
	}

	public boolean hasFileName() {
		return ((fileName!=null)&&(!fileName.isEmpty()));
	}

	public String getFileName() {
		return fileName;
	}

	public boolean hasMimetype() {
		return ((mimeType!=null)&&(!mimeType.isEmpty()));
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setCheckSum(String checkSum, ChecksumType type) {
		this.checkSumValue = checkSum;
		this.checkSumType = type;
	}
	
	public boolean hasCheckSum() {
		return ((checkSumValue!=null)&&(!checkSumValue.isEmpty()));
	}

	public String getCheckSum() {
		return checkSumValue;
	}
	
    public ChecksumType getCheckSumType() {
        return checkSumType;
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
        // get directory label as string
        // -------------------------------
        if ((jsonObj.has(DIRECTORY_LABEL_ATTR_NAME)) && (!jsonObj.get(DIRECTORY_LABEL_ATTR_NAME).isJsonNull())){

            this.directoryLabel = jsonObj.get(DIRECTORY_LABEL_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get directory label as string
        // -------------------------------
        if ((jsonObj.has(LABEL_ATTR_NAME)) && (!jsonObj.get(LABEL_ATTR_NAME).isJsonNull())){

            this.label = jsonObj.get(LABEL_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get freeform provenance as string
        // -------------------------------
        if ((jsonObj.has(PROVENANCE_FREEFORM_ATTR_NAME)) && (!jsonObj.get(PROVENANCE_FREEFORM_ATTR_NAME).isJsonNull())){

            this.provFreeForm = jsonObj.get(PROVENANCE_FREEFORM_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get restriction as boolean
        // -------------------------------
        if ((jsonObj.has(RESTRICT_ATTR_NAME)) && (!jsonObj.get(RESTRICT_ATTR_NAME).isJsonNull())){
            
            this.restrict = Boolean.valueOf(jsonObj.get(RESTRICT_ATTR_NAME).getAsString());
        }
        
        // -------------------------------
        // get storage identifier as string
        // -------------------------------
        if ((jsonObj.has(STORAGE_IDENTIFIER_ATTR_NAME)) && (!jsonObj.get(STORAGE_IDENTIFIER_ATTR_NAME).isJsonNull())){

            this.storageIdentifier = jsonObj.get(STORAGE_IDENTIFIER_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get file name as string
        // -------------------------------
        if ((jsonObj.has(FILE_NAME_ATTR_NAME)) && (!jsonObj.get(FILE_NAME_ATTR_NAME).isJsonNull())){

            this.fileName = jsonObj.get(FILE_NAME_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get mimetype as string
        // -------------------------------
        if ((jsonObj.has(MIME_TYPE_ATTR_NAME)) && (!jsonObj.get(MIME_TYPE_ATTR_NAME).isJsonNull())){

            this.mimeType = jsonObj.get(MIME_TYPE_ATTR_NAME).getAsString();
        }
        
        // -------------------------------
        // get md5 checkSum as string
        // -------------------------------
        if ((jsonObj.has(LEGACY_CHECKSUM_ATTR_NAME)) && (!jsonObj.get(LEGACY_CHECKSUM_ATTR_NAME).isJsonNull())){

            this.checkSumValue = jsonObj.get(LEGACY_CHECKSUM_ATTR_NAME).getAsString();
            this.checkSumType= ChecksumType.MD5;
        }
        // -------------------------------
        // get checkSum type and value
        // -------------------------------
        else if ((jsonObj.has(CHECKSUM_OBJECT_NAME)) && (!jsonObj.get(CHECKSUM_OBJECT_NAME).isJsonNull())){

            this.checkSumValue = ((JsonObject) jsonObj.get(CHECKSUM_OBJECT_NAME)).get(CHECKSUM_OBJECT_VALUE).getAsString();
            this.checkSumType = ChecksumType.fromString(((JsonObject) jsonObj.get(CHECKSUM_OBJECT_NAME)).get(CHECKSUM_OBJECT_TYPE).getAsString());

        }
        
        // -------------------------------
        // get tags 
        // -------------------------------
        Gson gson = new Gson();
        
        //Type objType = new TypeToken<List<String[]>>() {}.getType();
        Type listType = new TypeToken<List<String>>() {}.getType();
        Type treeType = new TypeToken<List<LinkedTreeMap>>() {}.getType();
        //----------------------
        // Load tags
        //----------------------
        if ((jsonObj.has(CATEGORIES_ATTR_NAME)) && (!jsonObj.get(CATEGORIES_ATTR_NAME).isJsonNull())){

            List<String> catList = new ArrayList();
            
            try {
                //We try to parse this as a treeMap if the syntax passed was "categories":[{"name","A Category"}]
                List<LinkedTreeMap> testLinked = gson.fromJson(jsonObj.get(CATEGORIES_ATTR_NAME), treeType);
                for(LinkedTreeMap ltm : testLinked) {
                    catList.add((String) ltm.get("name"));
                }
            } catch (JsonSyntaxException je){
                //If parsing a treeMap failed we try again with the syntax "categories":["A Category"]
                catList = gson.fromJson(jsonObj.get(CATEGORIES_ATTR_NAME), listType);
            }

            this.categories = catList;
            setCategories(this.categories);
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
     * Note that this call may have issues seeing fileMetadata generated before it by Dataset.getEditVersion()
     */
    public void addOptionalParams(DataFile df) throws DataFileTagException {
        if (df == null){            
            throw new NullPointerException("The datafile cannot be null!");
        }
        
        FileMetadata fm = df.getFileMetadata();
        
        addOptionalParams(fm);
    }
    
    public void addOptionalParams(FileMetadata fm) throws DataFileTagException{
        
        // ---------------------------
        // Add description
        // ---------------------------
        if (hasDescription()){
            fm.setDescription(this.getDescription());
        }

        // ---------------------------
        // Add directory label (path)
        // ---------------------------
        if (hasDirectoryLabel()){
            fm.setDirectoryLabel(this.getDirectoryLabel());
        }
        
        // ---------------------------
        // Add directory label (path)
        // ---------------------------
        if (hasLabel()){
            fm.setLabel(this.getLabel());
        }
        
        // ---------------------------
        // Add freeform provenance
        // ---------------------------
        if (hasProvFreeform()){
            fm.setProvFreeForm(this.getProvFreeform());
        }
        
        // ---------------------------
        // Add categories
        // ---------------------------
        replaceCategoriesInDataFile(fm);
       

        // ---------------------------
        // Add DataFileTags
        // ---------------------------
        replaceFileDataTagsInFile(fm.getDataFile());
       
    }
    

    /**
     *  Replace Categories in the DataFile.
     *  
     * This previously added the categories to what previously existed on the file metadata, but was switched to replace.
     * This was because the add-to functionality was never utilized and replace was needed for file metadata update
     * 
     */
    private void replaceCategoriesInDataFile(FileMetadata fileMetadata){
        
        if (fileMetadata == null){            
            throw new NullPointerException("The fileMetadata cannot be null!");
        }
        
        // Is there anything to add?
        //
        if (!hasCategories()){
            return;
        }
        
        //List<String> currentCategories = fileMetadata.getCategoriesByName();

        // Add categories to the file metadata object
        //
        fileMetadata.setCategories(new ArrayList()); //clear categories
        
        this.getCategories().stream().forEach((catText) -> {               
            fileMetadata.addCategoryByName(catText);  // fyi: "addCategoryByName" checks for dupes
        });
    }

    
    /**
     * Replace File Data Tags in Tabular File.
     * 
     * This previously added the tags to what previously existed on the file metadata, but was switched to replace.
     * This was because the add-to functionality was never utilized and replace was needed for file metadata update
     * 
     * NOTE: DataFile tags can only be added to tabular files
     * 
     *      - e.g. The file must already be ingested.
     * 
     * Because of this, these tags cannot be used when "Adding" a file via 
     * the API--e.g. b/c the file will note yet be ingested
     * 
     * @param df 
     */
    private void replaceFileDataTagsInFile(DataFile df) throws DataFileTagException{
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
        
        df.setTags(new ArrayList<DataFileTag>()); //MAD: TEST CLEARING TAGS
        
        

        // --------------------------------------------------
        // Iterate through and add any new labels
        // --------------------------------------------------
        DataFileTag newTagObj;
        for (String tagLabel : this.getDataFileTags()){    
            if (DataFileTag.isDataFileTag(tagLabel)){

                newTagObj = new DataFileTag();
                newTagObj.setDataFile(df);
                newTagObj.setTypeByLabel(tagLabel);
                df.addTag(newTagObj);

            }
        }                
        
    }

}
