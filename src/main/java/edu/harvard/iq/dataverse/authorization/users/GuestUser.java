package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;

/**
 * Guest user in the system. There's only one, so you get it with the static getter {@link #get()} (singleton pattern).
 * 
 * @author michael
 */
public class GuestUser implements User {
    
    private static final GuestUser INSTANCE = new GuestUser();
    
    public static GuestUser get() { return INSTANCE; }
    
    private GuestUser(){}
    
    @Override
    public String getIdentifier() {
        return ":guest";
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo("Guest", null);
    }

    @Override
    public boolean isAuthenticated() { return false; }
    
    @Override
    public boolean isSuperuser() {
        return false;
    }

    @Override
    public boolean equals( Object o ) {
        return (o instanceof GuestUser);
    }
    
    @Override
    public String toString() {
        return "[GuestUser :guest]";
    }
    
    @Override
    public int hashCode() {
        return 7;
    }
    
}
