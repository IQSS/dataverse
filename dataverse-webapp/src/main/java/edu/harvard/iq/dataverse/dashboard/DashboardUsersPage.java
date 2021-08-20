package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.primefaces.model.LazyDataModel;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("DashboardUsersPage")
public class DashboardUsersPage implements java.io.Serializable {

    private UserServiceBean userService;
    private DataverseSession session;
    private PermissionsWrapper permissionsWrapper;
    private DashboardUsersService dashboardUsersService;
    private SystemConfig systemConfig;
    private DashboardUserInfoService dashboardUserInfoService;

    private static final Logger logger = Logger.getLogger(DashboardUsersPage.class.getCanonicalName());

    private String searchTerm;
    private LazyDataModel<DashboardUserInfo> users;
    private DashboardUserInfo selectedUserInfo;


    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DashboardUsersPage() {
    }

    @Inject
    public DashboardUsersPage(UserServiceBean userService, DataverseSession session,
                              PermissionsWrapper permissionsWrapper, DashboardUsersService dashboardUsersService,
                              SystemConfig systemConfig, DashboardUserInfoService dashboardUserInfoService) {
        this.userService = userService;
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.dashboardUsersService = dashboardUsersService;
        this.systemConfig = systemConfig;
        this.dashboardUserInfoService = dashboardUserInfoService;
    }

    // -------------------- GETTERS --------------------

    public String getSearchTerm() {
        return searchTerm;
    }

    public LazyDataModel<DashboardUserInfo> getUsers() {
        return users;
    }

    public DashboardUserInfo getSelectedUserInfo() {
        return selectedUserInfo;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (!session.getUser().isSuperuser() || systemConfig.isReadonlyMode()) {
            return permissionsWrapper.notAuthorized();
        }

        users = new LazyUsersInfoDataModel(userService, dashboardUserInfoService);

        return null;
    }

    public void saveSuperuserStatus() {
        if (selectedUserInfo != null) {
            Try.of(() -> dashboardUsersService.changeSuperuserStatus(Long.parseLong(selectedUserInfo.getId())))
                    .onSuccess(user -> selectedUserInfo.setSuperuser(user.isSuperuser()))
                    .onFailure(throwable -> logger.warning("Failed to permanently toggle the superuser status for user " + selectedUserInfo.getIdentifier() + ": " + throwable.getMessage()));
        } else {
            logger.warning("selectedUserPersistent is null.  AuthenticatedUser not found for id: ");
        }
    }

    public void cancelSuperuserStatusChange() {
        if (selectedUserInfo != null) {
            selectedUserInfo.setSuperuser(!selectedUserInfo.isSuperuser());
            selectedUserInfo = null;
        }
    }

    public void removeUserRoles() {
        logger.fine("Get persisent AuthenticatedUser for id: " + selectedUserInfo.getId());

        Try.of(() -> dashboardUsersService.revokeAllRolesForUser(Long.parseLong(selectedUserInfo.getId())))
                .onSuccess(user -> selectedUserInfo.setRoles(null))
                .onSuccess(user -> JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dashboard.list_users.removeAll.message.success",
                        selectedUserInfo.getIdentifier())))
                .onFailure(throwable -> JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dashboard.list_users.removeAll.message.failure",
                        selectedUserInfo.getIdentifier())))
                .onFailure(throwable -> logger.log(Level.SEVERE, "Revoking all roles failed for user: " + selectedUserInfo.getIdentifier(), throwable));
    }

    public String getConfirmRemoveRolesMessage() {
        if (selectedUserInfo != null) {
            return BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.roles.removeAll.confirmationText", selectedUserInfo.getIdentifier());
        }
        return BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.roles.removeAll.confirmationText");
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

    // -------------------- SETTERS --------------------

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public void setUsers(LazyDataModel<DashboardUserInfo> users) {
        this.users = users;
    }

    public void setSelectedUserInfo(DashboardUserInfo selectedUserInfo) {
        this.selectedUserInfo = selectedUserInfo;
    }

}
