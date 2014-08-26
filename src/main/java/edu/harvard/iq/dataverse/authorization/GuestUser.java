package edu.harvard.iq.dataverse.authorization;

/**
 * Guest user in the system. There's only one, so you get it with the static getter {@link #get()} (singleton pattern).
 * 
 * @author michael
 */
public class GuestUser implements User {
    
    private static final GuestUser instance = new GuestUser();
    
    public static GuestUser get() { return instance; }
    
    private GuestUser(){}
    
    @Override
    public String getIdentifier() {
        return User.IDENTIFIER_PREFIX + "guest";
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo("Guest", null);
    }

    @Override
    public boolean isAuthenticated() { return false; }
    
}
