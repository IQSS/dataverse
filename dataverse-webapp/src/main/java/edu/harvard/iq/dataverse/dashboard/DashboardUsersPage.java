package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.omnifaces.cdi.ViewScoped;

import javax.inject.Inject;
import javax.inject.Named;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("DashboardUsersPage")
public class DashboardUsersPage implements java.io.Serializable {

    private UserServiceBean userService;
    private DataverseSession session;
    private PermissionsWrapper permissionsWrapper;
    private DashboardUsersService dashboardUsersService;

    private static final Logger logger = Logger.getLogger(DashboardUsersPage.class.getCanonicalName());

    private Integer selectedPage = 1;
    private UserListMaker userListMaker = null;
    private Pager pager;
    private List<AuthenticatedUser> userList;
    private String searchTerm;
    private AuthenticatedUser selectedUser = null;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DashboardUsersPage() {
    }

    @Inject
    public DashboardUsersPage(UserServiceBean userService, DataverseSession session,
                              PermissionsWrapper permissionsWrapper, DashboardUsersService dashboardUsersService) {
        this.userService = userService;
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.dashboardUsersService = dashboardUsersService;
    }

    // -------------------- GETTERS --------------------

    /**
     * Pager for when user list exceeds the number of display rows
     * (default: UserListMaker.ITEMS_PER_PAGE)
     *
     * @return
     */
    public Pager getPager() {
        return this.pager;
    }

    public Integer getSelectedPage() {
        if ((selectedPage == null) || (selectedPage < 1)) {
            setSelectedPage(null);
        }
        return selectedPage;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public AuthenticatedUser getSelectedUser() {
        return selectedUser;
    }

    public List<AuthenticatedUser> getUserList() {
        return this.userList;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (session.getUser().isSuperuser()) {
            userListMaker = new UserListMaker(userService);
            runUserSearch();
        } else {
            return permissionsWrapper.notAuthorized();
        }

        return null;
    }

    public boolean runUserSearchWithPage(Integer pageNumber) {
        System.err.println("runUserSearchWithPage");
        setSelectedPage(pageNumber);
        runUserSearch();
        return true;
    }

    public void saveSuperuserStatus() {
        if (selectedUser != null) {
            Try.of(() -> dashboardUsersService.changeSuperuserStatus(selectedUser))
                    .onSuccess(user -> selectedUser = user)
                    .onFailure(throwable -> logger.warning("Failed to permanently toggle the superuser status for user " + selectedUser.getIdentifier() + ": " + throwable.getMessage()));
        } else {
            logger.warning("selectedUserPersistent is null.  AuthenticatedUser not found for id: ");
        }
    }

    public void cancelSuperuserStatusChange() {
        if(selectedUser != null) {
            selectedUser.setSuperuser(!selectedUser.isSuperuser());
            selectedUser = null;
        }
    }

    public void removeUserRoles() {
        logger.fine("Get persisent AuthenticatedUser for id: " + selectedUser.getId());

        Try.of(() -> dashboardUsersService.revokeAllRolesForUser(selectedUser))
                .onSuccess(user -> selectedUser=user)
                .onSuccess(user -> selectedUser.setRoles(null))
                .onSuccess(user -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dashboard.list_users.removeAll.message.success",
                        selectedUser.getUserIdentifier())))
                .onFailure(throwable ->  JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dashboard.list_users.removeAll.message.failure",
                        selectedUser.getUserIdentifier())))
                .onFailure(throwable -> logger.log(Level.SEVERE, "Revoking all roles failed for user: " + selectedUser.getIdentifier(), throwable));
    }

    public String getConfirmRemoveRolesMessage() {
        if (selectedUser != null) {
            return BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.roles.removeAll.confirmationText", Collections.singletonList(selectedUser.getUserIdentifier()));
        }
        return BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.roles.removeAll.confirmationText");
    }

    public String getAuthProviderFriendlyName(String authProviderId) {
        return AuthenticationProvider.getFriendlyName(authProviderId);
    }

    public boolean runUserSearch() {

        logger.fine("Run the search!");

        /**
         * (1) Determine the number of users returned by the count        
         */
        UserListResult userListResult = userListMaker.runUserSearch(searchTerm, UserListMaker.ITEMS_PER_PAGE, getSelectedPage(), null);
        if (userListResult == null) {
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
        return Admin.listUsersFullAPIPath;
    }

    /**
     * Number of total users
     *
     * @return
     */
    public String getUserCount() {
        return NumberFormat.getInstance().format(userService.getTotalUserCount());
    }

    /**
     * Number of total Superusers
     *
     * @return
     */
    public Long getSuperUserCount() {
        return userService.getSuperUserCount();
    }

    public void setSelectedPage(Integer pgNum) {
        if ((pgNum == null) || (pgNum < 1)) {
            this.selectedPage = 1;
        }
        selectedPage = pgNum;
    }

    // -------------------- SETTERS --------------------

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public void setSelectedUser(AuthenticatedUser user) {
        selectedUser = user;
    }
}
