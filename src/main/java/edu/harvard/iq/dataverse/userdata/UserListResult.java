/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mydata.Pager;
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


    public UserListResult(String searchTerm, Pager pager, List<AuthenticatedUser> userList){
        
        this.searchTerm = searchTerm;
        this.pager = pager;
        this.userList = userList;
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
}
