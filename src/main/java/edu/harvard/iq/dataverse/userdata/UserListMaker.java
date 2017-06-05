/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.util.BundleUtil;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author rmp553
 */
public class UserListMaker {
    
    UserServiceBean userService;
    
    public boolean errorFound = false;
    public String errorMessage = null;
    
    public static final int ITEMS_PER_PAGE = 25;
    public static final int DEFAULT_OFFSET = 0;
    
    
    /*
     * Constructor
     */
    public UserListMaker(UserServiceBean userService) {
        this.msgt("MyDataFinder, constructor");
        this.userService = userService;
    }
    
    
    public Long getTotalUserCount(boolean superusers_only){
    
        if (superusers_only){
            return userService.getSuperUserCount();            
        }else{
            return userService.getUserCount(null);  // send null for the optional search term
        }
    
    }
    
    
    /** 
     * 
     * Get user count with search
     * 
     * @param searchTerm
     * @return 
     */
    public Long getUserCountWithSearch(String searchTerm){

        if (searchTerm == null){
            searchTerm = "";
        }else{
            searchTerm = searchTerm.trim();
        }
        
        return userService.getUserCount(searchTerm);
    }
  
    /*
     * Run the search
     */
    public JsonObjectBuilder runSearch(String searchTerm, Integer itemsPerPage, Integer selectedPage, String sorKey){
        
        // Initialize searchTerm
        if ((searchTerm == null) || (searchTerm.trim().isEmpty())){
            searchTerm = null;
        }

        // Initialize itemsPerPage
        if ((itemsPerPage == null) || (itemsPerPage < 10)){
            itemsPerPage = ITEMS_PER_PAGE;
        }

        // Initialize selectedPage
        if ((selectedPage == null) || (selectedPage < 1)){
            selectedPage = 1;
        }

        // Initialize sortKey
        String sortKey = null;

              
        // -------------------------------------------------
        // (1) What is the user count for this search?
        // -------------------------------------------------
        Long userCount = userService.getUserCount(searchTerm);
        
        // Are there any hits?  No; return info
        if ((userCount == null)||(userCount == 0)){
            return getNoResultsJSON();
        }
              
        // -------------------------------------------------
        // (2) Do some calculations here regarding the selected page, offset, etc.
        // -------------------------------------------------
        
        OffsetPageValues offsetPageValues = getOffset(userCount, selectedPage, itemsPerPage);
        selectedPage = offsetPageValues.getPageNumber();
        int offset = offsetPageValues.getOffset();
        
        //int offset = (selectedPage - 1) * itemsPerPage;
        //if (offset > userCount){
        //    offset = DEFAULT_OFFSET;
        //    selectedPage = 1;
        //}

        
        // -------------------------------------------------
        // (3) Retrieve the users
        // -------------------------------------------------
        JsonArrayBuilder jsonUserListArray = userService.getUserListAsJSON(searchTerm, sortKey, itemsPerPage, offset);       
        if (jsonUserListArray==null){
            return getNoResultsJSON();
        }
        
         Pager pager = new Pager(userCount.intValue(), itemsPerPage, selectedPage);

        JsonObjectBuilder jsonOverallData = Json.createObjectBuilder();
        jsonOverallData.add("userCount", userCount)
                       .add("selectedPage", 1)
                       .add("pagination", pager.asJsonObjectBuilder())
                       .add("bundleStrings", getBundleStrings())
                       .add("users", jsonUserListArray)
                       ;
        return jsonOverallData;
        
    }
    
    public OffsetPageValues getOffset(Long userCount, Integer selectedPage, Integer itemsPerPage){
        
        if (userCount == null){
            return new OffsetPageValues(DEFAULT_OFFSET, 0);
        }
        
        if (itemsPerPage == null){
            itemsPerPage = ITEMS_PER_PAGE;
        }
        if ((selectedPage == null)||(selectedPage < 1)){
            selectedPage = 1;
        }
        
        int offset = (selectedPage - 1) * itemsPerPage;
        if (offset > userCount){
            offset = DEFAULT_OFFSET;
            selectedPage = 1;
        }
        
        return new OffsetPageValues(offset, selectedPage);
                
    }
    
    
    private JsonObjectBuilder getNoResultsJSON(){
        
         return Json.createObjectBuilder()
                        .add("userCount", 0)
                        .add("selectedPage", 1)
                        .add("bundleStrings", getBundleStrings())
                        .add("users", Json.createArrayBuilder()); // empty array
    }
    
    public JsonObjectBuilder getBundleStrings(){
     
           return Json.createObjectBuilder()                   
                .add("userId", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.userId"))
                .add("userIdentifier", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.userIdentifier"))
                .add("lastName", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.lastName"))
                .add("firstName", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.firstName"))
                .add("email", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.email"))
                .add("isSuperuser", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.isSuperuser"))
                ;
                       
    }
    
    public boolean hasError(){
        return this.errorFound;
    }
    
    public String getErrorMessage(){
        return this.errorMessage;
    }
    
    private void addErrorMessage(String errMsg){
        this.errorFound = true;
        this.errorMessage = errMsg;
    }
   
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
}
