package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.userdata.OffsetPageValues;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
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
    private List<Object[]> userList;


    public String init() {
        System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

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
    
    
    public boolean runUserSearch(){

        String searchTerm = "a";
        
        /**
         * (1) Determine the number of users returned by the count        
         */
        Long userCount = userListMaker.getUserCountWithSearch(null);
        
        int itemsPerPage = UserListMaker.ITEMS_PER_PAGE;
        /**
         * (2) Based on the page number and number of users,
         *      determine the off set to use when querying
         */
        OffsetPageValues offsetPageValues = userListMaker.getOffset(userCount, getSelectedPage(), itemsPerPage);
        
        /**
         * Update the pageNumber on the page
         */
        setSelectedPage(offsetPageValues.getPageNumber());        
        int offset = offsetPageValues.getOffset();
        
        /**
         * (3) Run the search and update the user list 
         */
        String sortKey = null;
        this.userList = userService.getUserList(searchTerm, sortKey, UserListMaker.ITEMS_PER_PAGE, offsetPageValues.getOffset());

        pager = new Pager(userCount.intValue(), itemsPerPage, getSelectedPage());
       
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

        
    public List<Object[]> getUserList() {
        return this.userList;
    }

    public Pager getPager() {
        return this.pager;
    }

    private void setSelectedPage(Integer pgNum){
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
}
