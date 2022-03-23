/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mydata.Pager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

/**
 *
 * @author rmp553
 */
public class UserListResult {
    
    private static final Logger logger = Logger.getLogger(UserListResult.class.getName());

    private String searchTerm;

    private Pager pager;
    private List<AuthenticatedUser> userList;

    private boolean success;

    private String errorMessage;
        
    
    public UserListResult(String searchTerm, Pager pager, List<AuthenticatedUser> userList){
        
        
        if (searchTerm == null){
            searchTerm = "";
        }
        this.searchTerm = searchTerm;
        
        this.pager = pager;
        if (this.pager==null){
            logger.severe("Pager should never be null!");
        }
        
        this.userList = userList;
        if (this.userList == null){
            this.userList = new ArrayList<>();  // new empty list
        }
       
    }

    public Integer getSelectedPageNumber(){
        
        if (pager == null){
            return 1;
        }
        return pager.getSelectedPageNumber();
    }
    
    /**
     *  Set searchTerm
     *  @param searchTerm
     */
    public void setSearchTerm(String searchTerm){
        this.searchTerm = searchTerm;
    }

    /**
     *  Get for searchTerm
     *  @return String
     */
    public String getSearchTerm(){
        return this.searchTerm;
    }
    

    /**
     *  Set pager
     *  @param pager
     */
    public void setPager(Pager pager){
        this.pager = pager;
    }

    /**
     *  Get for pager
     *  @return Pager
     */
    public Pager getPager(){
        return this.pager;
    }
    

    /**
     *  Set userList
     *  @param userList
     */
    public void setUserList(List<AuthenticatedUser> userList){
        this.userList = userList;
    }

    /**
     *  Get for userList
     *  @return List<AuthenticatedUser>
     */
    public List<AuthenticatedUser> getUserList(){
        return this.userList;
    }
    
    
    /**
     *  Set success
     *  @param success
     */
    public void setSuccess(boolean success){
        this.success = success;
    }

    /**
     *  Get for success
     *  @return boolean
     */
    public boolean getSuccess(){
        return this.success;
    }
    
   

    /**
     *  Set errorMessage
     *  @param errorMessage
     */
    public void setErrorMessage(String errorMessage){
        this.errorMessage = errorMessage;
    }

    /**
     *  Get for errorMessage
     *  @return String
     */
    public String getErrorMessage(){
        return this.errorMessage;
    }

    
    /**
     * TO DO!
     * Return this object as a JsonObjectBuilder object
     * 
     * @return 
     */
    public JsonObjectBuilder toJSON(){
        
        if (userList.isEmpty()){
            return getNoResultsJSON();
        }
        if (pager==null){
            logger.severe("Pager should never be null!");
            return getNoResultsJSON();
           
        }
        
        JsonObjectBuilder jsonOverallData = Json.createObjectBuilder();
        jsonOverallData.add("userCount", pager.getNumResults())
                       .add("selectedPage", pager.getSelectedPageNumber())
                       .add("pagination", pager.asJsonObjectBuilder())
                       .add("bundleStrings", AuthenticatedUser.getBundleStrings())
                       .add("users", getUsersAsJSONArray())
                       ;
        return jsonOverallData;
    }
    

    
    private JsonArrayBuilder getUsersAsJSONArray(){
        
         // -------------------------------------------------
        // No results..... Return count of 0 and empty array
        // -------------------------------------------------
        if ((userList==null)||(userList.isEmpty())){
            return Json.createArrayBuilder(); // return an empty array
        }
        
        // -------------------------------------------------
        // We have results, format them into a JSON object
        // -------------------------------------------------
        JsonArrayBuilder jsonUserListArray = Json.createArrayBuilder();

        for (AuthenticatedUser oneUser : userList) {    
            jsonUserListArray.add(oneUser.toJson());
        }            
        return jsonUserListArray;
    }
    
    
    private JsonObjectBuilder getNoResultsJSON(){
        
         return Json.createObjectBuilder()
                        .add("userCount", 0)
                        .add("selectedPage", 1)
                        .add("bundleStrings", AuthenticatedUser.getBundleStrings())
                        .add("users", Json.createArrayBuilder()); // empty array
    }
    
   

}
