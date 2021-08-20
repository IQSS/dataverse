package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import edu.harvard.iq.dataverse.util.PrimefacesUtil;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LazyUsersInfoDataModel extends LazyDataModel<DashboardUserInfo> {
    private static final long serialVersionUID = 3477513295109722650L;

    private final UserServiceBean userService;
    private final DashboardUserInfoService dashboardUserInfoService;
    private final UserListMaker userListMaker;
    private List<DashboardUserInfo> users = new ArrayList<>();
    private DashboardUserInfo lastSelectedUserInfo = null;
    private String lastSelectedRowId = null;

    // -------------------- CONSTRUCTORS --------------------

    public LazyUsersInfoDataModel(UserServiceBean userService, DashboardUserInfoService dashboardUserInfoService) {
        this.userService = userService;
        this.dashboardUserInfoService = dashboardUserInfoService;
        this.userListMaker = new UserListMaker(userService);
    }

    // -------------------- LOGIC --------------------

    @Override
    public DashboardUserInfo getRowData(String rowId) {
        // workaround for: https://github.com/primefaces/primefaces/issues/6169
        if (lastSelectedRowId != null && lastSelectedRowId.equals(rowId)) {
            return lastSelectedUserInfo;
        }

        lastSelectedRowId = rowId;
        lastSelectedUserInfo = users.stream()
                .filter(user -> user.getId().equals(rowId))
                .findFirst()
                .orElseGet(null);

        return lastSelectedUserInfo;
    }

    @Override
    public Object getRowKey(DashboardUserInfo dashboardUserInfo) {
        return dashboardUserInfo.getId();
    }

    @Override
    public List<DashboardUserInfo> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, FilterMeta> filterMeta) {
        // filter
        String filterValue = getGlobalFilterValue(filterMeta);

        //sort
        String sortColumn = sortField != null ? sortField : "id";
        boolean isSortAscending = sortOrder == SortOrder.ASCENDING;
        int selectedPageNumber = first == 0 ? 1 : (first / pageSize) + 1;

        UserListResult userListResult = runUserSearch(filterValue, pageSize, selectedPageNumber, sortColumn, isSortAscending);
        users = createDashboardUsers(userListResult);

        this.setRowCount(userListResult.getPager().getNumResults());

        return users;
    }

    // -------------------- PRIVATE --------------------

    private String getGlobalFilterValue(Map<String, FilterMeta> filterMeta) {
        FilterMeta globalFilter = filterMeta.getOrDefault(PrimefacesUtil.GLOBAL_FILTER_KEY, null);
        return Objects.toString(globalFilter != null && globalFilter.getFilterValue() != null ? globalFilter.getFilterValue() : StringUtils.EMPTY);
    }

    private List<DashboardUserInfo> createDashboardUsers(UserListResult userListResult) {
        return dashboardUserInfoService.createDashboardUsers(userListResult);
    }

    private UserListResult runUserSearch(String searchTerm, Integer usersPerPage, Integer selectedPageNumber, String sortColumn, boolean isSortAscending) {
        return userListMaker.runUserSearch(searchTerm, usersPerPage, selectedPageNumber, sortColumn, isSortAscending);

    }
}
