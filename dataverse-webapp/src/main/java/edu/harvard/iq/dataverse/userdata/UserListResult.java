package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.util.List;

/**
 * @author rmp553
 */
public class UserListResult {

    private final Pager pager;
    private final List<AuthenticatedUser> userList;

    // -------------------- CONSTRUCTORS --------------------

    public UserListResult(Pager pager, List<AuthenticatedUser> userList) {
        this.pager = pager;
        this.userList = userList;
    }

    // -------------------- GETTERS --------------------

    public Pager getPager() {
        return pager;
    }

    public List<AuthenticatedUser> getUserList() {
        return this.userList;
    }
}
