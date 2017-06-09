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
import javax.json.JsonObjectBuilder;

/**
 *
 * @author rmp553
 */
public class UserListResult {
    
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
     * TO DO!
     * Return this object as a JsonObjectBuilder object
     * 
     * @return 
     */
    public JsonObjectBuilder asJSON(){
        
        return null;
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


}
