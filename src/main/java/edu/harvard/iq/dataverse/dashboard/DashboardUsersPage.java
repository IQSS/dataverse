package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ViewScoped
@Named("DashboardUsersPage")
public class DashboardUsersPage implements java.io.Serializable {
  
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    UserServiceBean userService;
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;

    private static final Logger logger = Logger.getLogger(DashboardUsersPage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;
    private Integer selectedPage = 1;
    private UserListMaker userListMaker = null;

    private Pager pager;
    private List<AuthenticatedUser> userList;
    
    private String searchTerm;

    public String init() {

        if ((session.getUser() != null) && (session.getUser().isAuthenticated()) && (session.getUser().isSuperuser())) {
           authUser = (AuthenticatedUser) session.getUser();
            userListMaker = new UserListMaker(userService);
            runUserSearch();
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type ‘you must be logged in message'
        }

        return null;
    }
    
    public boolean runUserSearchWithPage(Integer pageNumber){
        System.err.println("runUserSearchWithPage");
        setSelectedPage(pageNumber);
        runUserSearch();
        return true;
    }
    
    public boolean runUserSearch(){

        logger.fine("Run the search!");


        /**
         * (1) Determine the number of users returned by the count        
         */
        UserListResult userListResult = userListMaker.runUserSearch(searchTerm, UserListMaker.ITEMS_PER_PAGE, getSelectedPage(), null);
        if (userListResult==null){
            try {
                throw new Exception("userListResult should not be null!");
            } catch (Exception ex) {
                Logger.getLogger(DashboardUsersPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        setSelectedPage(userListResult.getSelectedPageNumber());        

        this.userList = userListResult.getUserList();
        this.pager = userListResult.getPager();
        
        return true;
        
    }


    
    public String getListUsersAPIPath() {
        //return "ok";
        return Admin.listUsersFullAPIPath;
    }

    /** 
     * Number of total users
     * @return 
     */
    public String getUserCount() {
        //return userService.getTotalUserCount();

        return NumberFormat.getNumberInstance(Locale.US).format(userService.getTotalUserCount());
    }

    /** 
     * Number of total Superusers
     * @return 
     */
    public Long getSuperUserCount() {
        
        return userService.getSuperUserCount();
    }

    public List<AuthenticatedUser> getUserList() {
        return this.userList;
    }

    /**
     * Pager for when user list exceeds the number of display rows
     * (default: UserListMaker.ITEMS_PER_PAGE)
     * 
     * @return 
     */
    public Pager getPager() {
        return this.pager;
    }

    public void setSelectedPage(Integer pgNum){
        if ((pgNum == null)||(pgNum < 1)){
            this.selectedPage = 1;
        }
        selectedPage = pgNum;
    }

    public Integer getSelectedPage(){
        if ((selectedPage == null)||(selectedPage < 1)){
            setSelectedPage(null);            
        }
        return selectedPage;
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    /* 
       Methods for toggling the supeuser status of a selected user. 
       Our normal two step approach is used: first showing the "are you sure?" 
       popup, then finalizing the toggled value. 
    */
       
    AuthenticatedUser selectedUser = null; 
    
    public void setSelectedUser(AuthenticatedUser user) {
        this.selectedUser = user;
    }
    
    public AuthenticatedUser getSelectedUser() {
        return this.selectedUser;
    }
    
    
    public void setUserToToggleSuperuserStatus(AuthenticatedUser user) {
        logger.info("selecting user "+user.getIdentifier());
        selectedUser = user; 
    }
    public void saveSuperuserStatus(){
        logger.info("Toggling user's "+selectedUser.getIdentifier()+" superuser status; (current status: "+selectedUser.isSuperuser()+")");
        logger.info("Attempting to save user "+selectedUser.getIdentifier());
        authenticationService.update(selectedUser);
        
    }
    
    public void cancelSuperuserStatusChange(){
        logger.info("unToggling user's "+selectedUser.getIdentifier()+" superuser status; (current status: "+selectedUser.isSuperuser()+")");
        selectedUser.setSuperuser(!selectedUser.isSuperuser());
        
    }
    
    
}
