package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

/**
 * A group containing all the users in the system - including the guest user.
 * So, basically, everyone.
 *
 * <b>NOTE</b> this group is a singleton, as there's no point in having more than one. Get the instance
 * using {@link #get()}.
 *
 * @author michael
 */
public final class AllUsers implements Group {

    public static final String GROUP_TYPE = "builtin";

    public static final AllUsers instance = new AllUsers();

    private final String identifier = ":AllUsers";

    private final String displayInfo = "Everyone (including guests)";

    public static final AllUsers get() {
        return instance;
    }

    /**
     * Prevent instance creation
     */
    private AllUsers() {
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(displayInfo, null);
    }

    @Override
    public String getAlias() {
        return GROUP_TYPE + Group.PATH_SEPARATOR + "all-users";
    }

    @Override
    public String getDisplayName() {
        return "All Users";
    }

    @Override
    public String getDescription() {
        return "All users, including guests";
    }

    @Override
    public String toString() {
        return "[AllUsers " + getIdentifier() + "]";
    }

}
