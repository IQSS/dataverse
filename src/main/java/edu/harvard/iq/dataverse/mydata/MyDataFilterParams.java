/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObject;
import static edu.harvard.iq.dataverse.DvObject.DATASET_DTYPE_STRING;
import static edu.harvard.iq.dataverse.DvObject.DATAVERSE_DTYPE_STRING;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchFields;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang.StringUtils;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/**
 *
 * @author rmp553
 */
public class MyDataFilterParams {
 
    // -----------------------------------
    // Static Reference objects
    // -----------------------------------
    public static final List<String> defaultDvObjectTypes = Arrays.asList(DvObject.DATAVERSE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING);
    
    public static final List<String> defaultPublishedStates = Arrays.asList(IndexServiceBean.getPUBLISHED_STRING(),
                                                    IndexServiceBean.getUNPUBLISHED_STRING(),
                                                    IndexServiceBean.getDRAFT_STRING());
    
    public static final HashMap<String, String> sqlToSolrSearchMap ;
    static
    {
        sqlToSolrSearchMap = new HashMap<>();
        sqlToSolrSearchMap.put(DvObject.DATAVERSE_DTYPE_STRING, SearchConstants.DATAVERSES);
        sqlToSolrSearchMap.put(DvObject.DATASET_DTYPE_STRING, SearchConstants.DATASETS);
        sqlToSolrSearchMap.put(DvObject.DATAFILE_DTYPE_STRING, SearchConstants.FILES);
    }
    
    public static final HashMap<String, String> userInterfaceToSqlSearchMap ;
    static
    {
        userInterfaceToSqlSearchMap = new HashMap<>();
        userInterfaceToSqlSearchMap.put(DvObject.DATAVERSE_DTYPE_STRING, SearchConstants.UI_DATAVERSES);
        userInterfaceToSqlSearchMap.put(DvObject.DATASET_DTYPE_STRING, SearchConstants.UI_DATAVERSES);
        userInterfaceToSqlSearchMap.put(DvObject.DATAFILE_DTYPE_STRING, SearchConstants.UI_FILES);
    }
    
    
    // -----------------------------------
    // Filter parameters
    // -----------------------------------
    private String userIdentifier;
    private List<String> dvObjectTypes;    
    private List<String> publicationStatuses;
    private List<Long> roleIds;
    
    //private ArrayList<DataverseRole> roles;
    private String searchTerm = "*:*";
    
    // -----------------------------------
    // Error checking
    // -----------------------------------
    private boolean errorFound = false;
    private String errorMessage = null;
    

    /**
     * Minimal filter params with defaults set
     * 
     * @param userIdentifier 
     */
    public MyDataFilterParams(String userIdentifier){
        
        if ((userIdentifier==null)||(userIdentifier.isEmpty())){
            throw new NullPointerException("MyDataFilterParams constructor: userIdentifier cannot be null or an empty string");
        }
        this.userIdentifier = userIdentifier;
        this.dvObjectTypes = MyDataFilterParams.defaultPublishedStates;
        this.publicationStatuses = MyDataFilterParams.defaultPublishedStates;
        this.checkParams();
    }
    
    /**
     * @param userIdentifier
     * @param dvObjectTypes
     * @param publicationStatuses 
     * @param searchTerm 
     */    
    public MyDataFilterParams(String userIdentifier, List<String> dvObjectTypes, List<String> publicationStatuses, List<Long> roleIds, String searchTerm){

        if ((userIdentifier==null)||(userIdentifier.isEmpty())){
            throw new NullPointerException("MyDataFilterParams constructor: userIdentifier cannot be null or an empty string");
        }

        if (dvObjectTypes==null){
            throw new NullPointerException("MyDataFilterParams constructor: dvObjectTypes cannot be null");
        }

        this.userIdentifier = userIdentifier;
        this.dvObjectTypes = dvObjectTypes;

        if (publicationStatuses == null){
            this.publicationStatuses = MyDataFilterParams.defaultPublishedStates;
        }else{
            this.publicationStatuses = publicationStatuses;
        }
        
        // Do something here if none chosen!
        this.roleIds = roleIds;
        
        if (searchTerm != null){
            this.searchTerm = searchTerm;
        }
        
        this.checkParams();
    }
    
    
    public List<Long> getRoleIds(){
        
        return this.roleIds;
    }
    
    
    
    private void checkParams(){
        
        if ((this.userIdentifier == null)||(this.userIdentifier.isEmpty())){
            this.addError("Sorry!  No user was found!");
            return;
        }
        
        if ((this.dvObjectTypes == null)||(this.dvObjectTypes.isEmpty())){
            this.addError("No results. Please select one of Dataverses, Datasets, Files.");
            return;
        }
        
        if ((this.publicationStatuses == null)||(this.publicationStatuses.isEmpty())){
            this.addError("No results. Please select one of " + StringUtils.join(MyDataFilterParams.defaultPublishedStates, ", ") + ".");
            return;
        }

        for (String dtype : this.dvObjectTypes){
            if (!DvObject.DTYPE_LIST.contains(dtype)){
                this.addError("Sorry!  The type '" + dtype + "' is not known.");
                return;
            }               
        }        
    }
    
    public List<String> getDvObjectTypes(){
        return this.dvObjectTypes;
    }
    
    public String getUserIdentifier(){
        return this.userIdentifier;
    }
    
    public String getErrorMessage(){
        return this.errorMessage;
    }
    
    public boolean hasError(){
        return this.errorFound;
    }

    public void addError(String s){
        this.errorFound = true;
        this.errorMessage = s;
    }

    // --------------------------------------------
    // start: Convenience methods for dvObjectTypes
    // --------------------------------------------
    public boolean areDataversesIncluded(){
        if (this.dvObjectTypes.contains(DvObject.DATAVERSE_DTYPE_STRING)){
            return true;
        }
        return false;
    }
    public boolean areDatasetsIncluded(){
        if (this.dvObjectTypes.contains(DvObject.DATASET_DTYPE_STRING)){
            return true;
        }
        return false;
    }
    public boolean areFilesIncluded(){
        if (this.dvObjectTypes.contains(DvObject.DATAFILE_DTYPE_STRING)){
            return true;
        }
        return false;
    }
    
    public String getSolrFragmentForDvObjectType(){
        if ((this.dvObjectTypes == null)||(this.dvObjectTypes.isEmpty())){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method, first check 'hasError()'");
        }
        
        List<String> solrTypes = new ArrayList<>();
        for (String dtype : this.dvObjectTypes){
            solrTypes.add(MyDataFilterParams.sqlToSolrSearchMap.get(dtype));
        }
                
        String valStr = StringUtils.join(solrTypes, " OR ");
        if (this.dvObjectTypes.size() > 1){
            valStr = "(" + valStr + ")";
        }
        
        return  SearchFields.TYPE + ":" + valStr;// + ")";
    }

    public String getSolrFragmentForPublicationStatus(){
        if ((this.publicationStatuses == null)||(this.publicationStatuses.isEmpty())){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method, first check 'hasError()'");
        }

        String valStr = StringUtils.join(this.publicationStatuses, " OR ");
        if (this.publicationStatuses.size() > 1){
            valStr = "(" + valStr + ")";
        }
        
        return  "(" + SearchFields.PUBLICATION_STATUS + ":" + valStr + ")";
    }

    public String getDvObjectTypesAsJSONString(){
        
        return this.getDvObjectTypesAsJSON().build().toString();
    }
    
    public JsonObjectBuilder getDvObjectTypesAsJSON(){
        
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();

        jsonArray.add(Json.createObjectBuilder().add("value", DvObject.DATAVERSE_DTYPE_STRING)
                            .add("label", SearchConstants.UI_DATAVERSES)
                            .add("selected", this.areDataversesIncluded()))
                .add(Json.createObjectBuilder().add("value", DvObject.DATASET_DTYPE_STRING)
                            .add("label", SearchConstants.UI_DATASETS)
                            .add("selected", this.areDatasetsIncluded()))
                .add(Json.createObjectBuilder().add("value", DvObject.DATAFILE_DTYPE_STRING)
                            .add("label", SearchConstants.UI_FILES)
                            .add("selected", this.areFilesIncluded())
                );
        
        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        jsonData.add(SearchFields.TYPE, jsonArray);
        
        return jsonData;
    }
    
    // --------------------------------------------
    // end: Convenience methods for dvObjectTypes
    // --------------------------------------------

    public String getSearchTerm(){
       return this.searchTerm;
   }
}
