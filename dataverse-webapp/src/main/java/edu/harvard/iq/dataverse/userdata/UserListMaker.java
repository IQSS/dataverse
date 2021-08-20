package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.util.List;

/**
 * @author rmp553
 */
public class UserListMaker {

    private static final int ITEMS_PER_PAGE = 25;
    private static final int MIN_ITEMS_PER_PAGE = 1;
    private static final int DEFAULT_OFFSET = 0;
    private final UserServiceBean userService;

    // -------------------- CONSTRUCTORS --------------------

    public UserListMaker(UserServiceBean userService) {
        this.userService = userService;
    }

    // -------------------- LOGIC --------------------

    public UserListResult runUserSearch(String searchTerm, Integer itemsPerPage, Integer selectedPage, String sortKey, boolean isSortAscending) {

        // Initialize searchTerm
        if ((searchTerm == null) || (searchTerm.trim().isEmpty())) {
            searchTerm = null;
        }

        // Initialize itemsPerPage
        if ((itemsPerPage == null) || (itemsPerPage < MIN_ITEMS_PER_PAGE)) {
            itemsPerPage = ITEMS_PER_PAGE;
        }

        // Initialize selectedPage
        if ((selectedPage == null) || (selectedPage < 1)) {
            selectedPage = 1;
        }

        Pager pager;

        // -------------------------------------------------
        // (1) What is the user count for this search?
        // -------------------------------------------------
        Long userCount = userService.getUserCount(searchTerm);

        // -------------------------------------------------
        // (2) Do some calculations here regarding the selected page, offset, etc.
        // -------------------------------------------------

        OffsetPageValues offsetPageValues = getOffset(userCount, selectedPage, itemsPerPage);
        selectedPage = offsetPageValues.getPageNumber();
        int offset = offsetPageValues.getOffset();

        // -------------------------------------------------
        // (3) Retrieve the users
        // -------------------------------------------------
        List<AuthenticatedUser> userList = userService.getAuthenticatedUserList(searchTerm, sortKey, isSortAscending, itemsPerPage, offset);

        pager = new Pager(userCount.intValue(), itemsPerPage, selectedPage);

        return new UserListResult(pager, userList);

    }

    // -------------------- PRIVATE --------------------

    private OffsetPageValues getOffset(Long userCount, Integer selectedPage, Integer itemsPerPage) {

        int offset = (selectedPage - 1) * itemsPerPage;
        if (offset > userCount) {
            return new OffsetPageValues(DEFAULT_OFFSET, 1);
        }

        return new OffsetPageValues(offset, selectedPage);

    }

}
