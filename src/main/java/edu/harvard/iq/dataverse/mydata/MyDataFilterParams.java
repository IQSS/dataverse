/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObject;
import static edu.harvard.iq.dataverse.DvObject.DATASET_DTYPE_STRING;
import static edu.harvard.iq.dataverse.DvObject.DATAVERSE_DTYPE_STRING;
import edu.harvard.iq.dataverse.search.SearchConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotEmpty;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/**
 *
 * @author rmp553
 */
public class MyDataFilterParams {
 
    public static final List<String> defaultDvObjectTypes = Arrays.asList(DATAVERSE_DTYPE_STRING, DATASET_DTYPE_STRING);
    
    public static final HashMap<String, String> sqlToSolrSearchMap ;
    static
    {
        sqlToSolrSearchMap = new HashMap<>();
        sqlToSolrSearchMap.put(DvObject.DATAVERSE_DTYPE_STRING, SearchConstants.DATAVERSE);
        sqlToSolrSearchMap.put(DvObject.DATASET_DTYPE_STRING, SearchConstants.DATASET);
        sqlToSolrSearchMap.put(DvObject.DATAFILE_DTYPE_STRING, SearchConstants.FILE);
    }
    
    @NotNull 
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "userIdentifier has invalid characters (only letters/numbers)")
    private String userIdentifier;

    @NotEmpty(message = "Please select one of Dataverses, Datasets, Files.")
    private List<String> dvObjectTypes;
    
    //private ArrayList<DataverseRole> roles;
    private Boolean publishedOnly = false;
    private String searchTerm = "*";
    
    // ---------------------------
    private boolean errorFound = false;
    private String errorMessage = null;
    
   
    
    public MyDataFilterParams(String userIdentifier, List<String> dvObjectTypes){

        if (userIdentifier==null){
            throw new NullPointerException("MyDataFilterParams constructor: userIdentifier cannot be null");
        }

        if (dvObjectTypes==null){
            throw new NullPointerException("MyDataFilterParams constructor: dvObjectTypes cannot be null");
        }
        
        this.userIdentifier = userIdentifier;
        //this.roles = roles;
        this.dvObjectTypes = dvObjectTypes;
        this.checkParams();
    }
    
    public void checkParams(){
        
        if ((this.userIdentifier == null)||(this.userIdentifier.isEmpty())){
            this.addError("Sorry!  No user was found!");
            return;
        }
        
        if ((this.dvObjectTypes == null)||(this.dvObjectTypes.isEmpty())){
            this.addError("No results. Please select one of Dataverses, Datasets, Files.");
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
    
    
    public String getDvObjectTypesAsJSON() throws JSONException{
        
        Map m1 = new HashMap();     
        m1.put(MyDataFilterParams.sqlToSolrSearchMap.get(DvObject.DATAVERSE_DTYPE_STRING), this.areDataversesIncluded());
        m1.put(MyDataFilterParams.sqlToSolrSearchMap.get(DvObject.DATASET_DTYPE_STRING), this.areDatasetsIncluded());
        m1.put(MyDataFilterParams.sqlToSolrSearchMap.get(DvObject.DATAFILE_DTYPE_STRING), this.areFilesIncluded());
        
        JSONObject jsonData = new JSONObject();

        jsonData.put("dvobjectTypes", m1);
        
        return jsonData.toString();
    }
    // --------------------------------------------
    // end: Convenience methods for dvObjectTypes
    // --------------------------------------------

}
