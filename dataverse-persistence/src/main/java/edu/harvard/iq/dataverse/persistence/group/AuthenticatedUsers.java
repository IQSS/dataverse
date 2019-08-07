package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;


public class AuthenticatedUsers implements Group {

    public static final String GROUP_TYPE = "builtin";

    private static final AuthenticatedUsers instance = new AuthenticatedUsers();

    private AuthenticatedUsers() {
    }

    public static AuthenticatedUsers get() {
        return instance;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public String getIdentifier() {
        return ":authenticated-users";
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(BundleUtil.getStringFromBundle("permission.anyoneWithAccount"), null);
    }

    @Override
    public String getAlias() {
        return GROUP_TYPE + Group.PATH_SEPARATOR + "authenticated-users";
    }

    @Override
    public String getDisplayName() {
        return "Authenticated Users";
    }

    @Override
    public String getDescription() {
        return "All users, except for guests";
    }

    @Override
    public String toString() {
        return "[AuthenticatedUsers " + getIdentifier() + "]";
    }


}
