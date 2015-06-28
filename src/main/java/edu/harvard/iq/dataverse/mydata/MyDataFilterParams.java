/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObject;
import java.util.ArrayList;
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
 
    @NotNull 
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "userIdentifier has invalid characters (only letters/numbers)")
    private String userIdentifier;

    @NotEmpty(message = "Please check at least one of Dataverses, Datasets, or Files.")
    private List<String> dvObjectTypes;
    
    //private ArrayList<DataverseRole> roles;
    private Boolean publishedOnly = false;
    private String searchTerm = "*";
    
    // ---------------------------
    private boolean errorFound = false;
    private String errorMessage = null;
    
    public MyDataFilterParams(String userIdentifier, List<String> dvObjectTypes){
        this.userIdentifier = userIdentifier;
        //this.roles = roles;
        this.dvObjectTypes = dvObjectTypes;
        this.checkParams();
    }
    
    public void checkParams(){
        
        for (String dtype : this.dvObjectTypes){
            if (!DvObject.DTYPE_LIST.contains(dtype)){
                this.addError("Sorry!  The type '" + dtype + "' was not found.");
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
        m1.put(DvObject.DATAVERSE_DTYPE_STRING, this.areDataversesIncluded());
        m1.put(DvObject.DATASET_DTYPE_STRING, this.areDatasetsIncluded());
        m1.put(DvObject.DATAFILE_DTYPE_STRING, this.areFilesIncluded());
        
        JSONObject jsonData = new JSONObject();

        jsonData.put("dvobjectTypes", m1);
        
        return jsonData.toString();
    }
    // --------------------------------------------
    // end: Convenience methods for dvObjectTypes
    // --------------------------------------------

}
