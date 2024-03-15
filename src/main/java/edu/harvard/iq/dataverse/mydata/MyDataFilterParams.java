/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author rmp553
 */
public class MyDataFilterParams {

    private static final Logger logger = Logger.getLogger(MyDataFilterParams.class.getCanonicalName());

    // -----------------------------------
    // Static Reference objects
    // -----------------------------------
    public static final List<DvObject.DType> defaultDvObjectTypes = Arrays.asList(DvObject.DType.Dataverse, DvObject.DType.Dataset);
    public static final List<DvObject.DType> allDvObjectTypes = Arrays.asList(DvObject.DType.Dataverse, DvObject.DType.Dataset, DvObject.DType.Dataverse, DvObject.DType.DataFile);
    
    public static final List<String> defaultPublishedStates = Arrays.asList(IndexServiceBean.getPUBLISHED_STRING(),
                                                    IndexServiceBean.getUNPUBLISHED_STRING(),
                                                    IndexServiceBean.getDRAFT_STRING(),
                                                    IndexServiceBean.getIN_REVIEW_STRING(),
                                                    IndexServiceBean.getDEACCESSIONED_STRING());
    public static final List<String> allPublishedStates = defaultPublishedStates;
            /*Arrays.asList(IndexServiceBean.getPUBLISHED_STRING(),
                                                    IndexServiceBean.getUNPUBLISHED_STRING(),
                                                    IndexServiceBean.getDRAFT_STRING(),
                                                    IndexServiceBean.getIN_REVIEW_STRING(),
                                                    IndexServiceBean.getDEACCESSIONED_STRING());*/
            
    public static final HashMap<DvObject.DType, String> sqlToSolrSearchMap ;
    static
    {
        sqlToSolrSearchMap = new HashMap<>();
        sqlToSolrSearchMap.put(DvObject.DType.Dataverse, SearchConstants.DATAVERSES);
        sqlToSolrSearchMap.put(DvObject.DType.Dataset, SearchConstants.DATASETS);
        sqlToSolrSearchMap.put(DvObject.DType.DataFile, SearchConstants.FILES);
    }
    
    public static final HashMap<DvObject.DType, String> userInterfaceToSqlSearchMap ;
    static
    {
        userInterfaceToSqlSearchMap = new HashMap<>();
        
        userInterfaceToSqlSearchMap.put(DvObject.DType.Dataverse, SearchConstants.UI_DATAVERSES);
        userInterfaceToSqlSearchMap.put(DvObject.DType.Dataset, SearchConstants.UI_DATAVERSES);
        userInterfaceToSqlSearchMap.put(DvObject.DType.DataFile, SearchConstants.UI_FILES);
    }
    
    
    // -----------------------------------
    // Filter parameters
    // -----------------------------------
    private DataverseRequest dataverseRequest;
    private AuthenticatedUser authenticatedUser;
    private String userIdentifier;
    private List<DvObject.DType> dvObjectTypes;
    private List<String> publicationStatuses;
    private List<Long> roleIds;
    private List<Boolean> datasetValidities;
    
    //private ArrayList<DataverseRole> roles;
    public static final String defaultSearchTerm = "*:*";
    private String searchTerm = "*:*";
    
    // -----------------------------------
    // Error checking
    // -----------------------------------
    private boolean errorFound = false;
    private String errorMessage = null;
    

    
    
    /**
     * Constructor used to get total counts
     * 
     * @param authenticatedUser
     * @param userIdentifier 
     */
    public MyDataFilterParams(DataverseRequest dataverseRequest, DataverseRolePermissionHelper roleHelper){
        if (dataverseRequest==null){
            throw new NullPointerException("MyDataFilterParams constructor: dataverseRequest cannot be null ");
        }
        this.dataverseRequest = dataverseRequest;
        this.setAuthenticatedUserFromDataverseRequest(dataverseRequest);
        this.userIdentifier = authenticatedUser.getIdentifier();

        if (roleHelper==null){
            throw new NullPointerException("MyDataFilterParams constructor: roleHelper cannot be null");
        }
        this.dvObjectTypes = MyDataFilterParams.allDvObjectTypes;
        this.publicationStatuses = MyDataFilterParams.allPublishedStates;
        this.datasetValidities = null;
        this.searchTerm = MyDataFilterParams.defaultSearchTerm;
        this.roleIds = roleHelper.getRoleIdList();
    }
    
    /**
     * @param userIdentifier
     * @param dvObjectTypes
     * @param publicationStatuses 
     * @param searchTerm 
     * @param datasetValidities
     */
    public MyDataFilterParams(DataverseRequest dataverseRequest, List<DvObject.DType> dvObjectTypes, List<String> publicationStatuses, List<Long> roleIds, String searchTerm, List<Boolean> datasetValidities) {
        if (dataverseRequest==null){
            throw new NullPointerException("MyDataFilterParams constructor: dataverseRequest cannot be null ");
        }
        this.dataverseRequest = dataverseRequest;
        this.setAuthenticatedUserFromDataverseRequest(dataverseRequest);
        this.userIdentifier = authenticatedUser.getIdentifier();

        if (dvObjectTypes==null){
            throw new NullPointerException("MyDataFilterParams constructor: dvObjectTypes cannot be null");
        }

        this.dvObjectTypes = dvObjectTypes;

        if (publicationStatuses == null){
            this.publicationStatuses = MyDataFilterParams.defaultPublishedStates;
        }else{
            this.publicationStatuses = publicationStatuses;
        }

        this.datasetValidities = datasetValidities;
        
        // Do something here if none chosen!
        this.roleIds = roleIds;
        
        if ((searchTerm == null)||(searchTerm.trim().isEmpty())){
            this.searchTerm = MyDataFilterParams.defaultSearchTerm;
        }else{
            this.searchTerm = searchTerm;
        }
        
        this.checkParams();
    }
    
    
    private void setAuthenticatedUserFromDataverseRequest(DataverseRequest dvRequest){
        
        if (dvRequest == null){
            throw new NullPointerException("MyDataFilterParams getAuthenticatedUserFromDataverseRequest: dvRequest cannot be null");
        }
        
        this.authenticatedUser = dvRequest.getAuthenticatedUser();
        
        if (this.authenticatedUser == null){
            throw new NullPointerException("MyDataFilterParams getAuthenticatedUserFromDataverseRequest: Hold on! dvRequest must be associated with an AuthenticatedUser be null");
        }
    }  
    
    
    public List<Long> getRoleIds(){
        
        return this.roleIds;
    }
    
    
    
    private void checkParams(){
        
        if ((this.userIdentifier == null)||(this.userIdentifier.isEmpty())){
            this.addError("Sorry!  No user was found!");
            return;
        }

        if ((this.roleIds == null)||(this.roleIds.isEmpty())){
            this.addError("No results. Please select at least one Role.");
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
    }
    
    public List<DvObject.DType> getDvObjectTypes(){
        return this.dvObjectTypes;
    }
    
    public String getUserIdentifier(){
        return this.userIdentifier;
    }
    
    
    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
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
        if (this.dvObjectTypes.contains(DvObject.DType.Dataverse)){
            return true;
        }
        return false;
    }
    public boolean areDatasetsIncluded(){
        if (this.dvObjectTypes.contains(DvObject.DType.Dataset)){
            return true;
        }
        return false;
    }
    public boolean areFilesIncluded(){
        if (this.dvObjectTypes.contains(DvObject.DType.DataFile)){
            return true;
        }
        return false;
    }
    
    public String getSolrFragmentForDvObjectType(){
        if ((this.dvObjectTypes == null)||(this.dvObjectTypes.isEmpty())){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method, first check 'hasError()'");
        }
        
        List<String> solrTypes = new ArrayList<>();
        for (DvObject.DType dtype : this.dvObjectTypes){
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

        // Add quotes around each publication status
        //
        List<String> solrPublicationStatuses = new ArrayList<>();
        for (String pubStatus : this.publicationStatuses){
            solrPublicationStatuses.add("\"" + pubStatus + "\"");
        }
        
        
        String valStr = StringUtils.join(solrPublicationStatuses, " OR ");
        if (this.publicationStatuses.size() > 1){
            valStr = "(" + valStr + ")";
        }

        return  "(" + SearchFields.PUBLICATION_STATUS + ":" + valStr + ")";
    }

    public String getSolrFragmentForDatasetValidity(){
        if ((this.datasetValidities == null) || (this.datasetValidities.isEmpty())){
            return "";
        }
    
        
        String valStr = StringUtils.join(datasetValidities, " OR ");
        if (this.datasetValidities.size() > 1){
            valStr = "(" + valStr + ")";
        }

        return  "(" + SearchFields.DATASET_VALID + ":" + valStr + ")";
    }

    public String getDvObjectTypesAsJSONString(){
        
        return this.getDvObjectTypesAsJSON().build().toString();
    }
    
     /**
     * "publication_statuses" : [ name 1, name 2, etc.]
     * 
     * @return 
     */
    public JsonArrayBuilder getListofSelectedPublicationStatuses(){
        
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        
        for (String pubStatus : this.publicationStatuses){
            jsonArray.add(pubStatus);            
        }
        return jsonArray;
                
    }

        
    /**
     * "dataset_valid" : [ true, false ]
     *
     * @return
     */
    public JsonArrayBuilder getListofSelectedValidities(){
        if (this.datasetValidities == null || this.datasetValidities.isEmpty()) {
            return null;
        }

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();

        for (Boolean valid : this.datasetValidities){
            jsonArray.add(valid);
        }
        return jsonArray;
    }
    
    
    public JsonObjectBuilder getDvObjectTypesAsJSON(){
        
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();

        jsonArray.add(Json.createObjectBuilder().add("value", DvObject.DType.Dataverse.getDType())
                            .add("label", SearchConstants.UI_DATAVERSES)
                            .add("selected", this.areDataversesIncluded()))
                .add(Json.createObjectBuilder().add("value", DvObject.DType.Dataset.getDType())
                            .add("label", SearchConstants.UI_DATASETS)
                            .add("selected", this.areDatasetsIncluded()))
                .add(Json.createObjectBuilder().add("value", DvObject.DType.DataFile.getDType())
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
    
    public static List<String[]> getPublishedStatesForMyDataPage(){
        if (defaultPublishedStates==null){
            throw new NullPointerException("defaultPublishedStates cannot be null");
        }
        List<String[]> publicationStateInfoList = new ArrayList<String[]>();
        String stateNameAsVariable;
        for (String displayState : defaultPublishedStates){
            stateNameAsVariable = displayState.toLowerCase().replace(" ", "_");
            String[] singleInfoRow = { displayState, stateNameAsVariable };
            publicationStateInfoList.add(singleInfoRow);
        }
        return publicationStateInfoList;
    }
  
    public DataverseRequest getDataverseRequest(){
        return this.dataverseRequest;
    }
  
}
