package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;

/**
 * Guest user in the system. There's only one, so you get it with the static getter {@link #get()} (singleton pattern).
 * 
 * @author michael
 */
public class GuestUser implements User {
    
    private static final GuestUser instance = new GuestUser();
    
    // TODO - re-enable after reversing the user-userRequestMetadata relation (issue #1257) 
//    public static GuestUser get() { return instance; }
    
    public GuestUser(){}
    
    private UserRequestMetadata requestMetadata;
    
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
    public boolean isBuiltInUser(){
        return false;
    }
    
    @Override
    public boolean isSuperuser() {
        return false;
    }
   
    @Override
    public UserRequestMetadata getRequestMetadata() {
        return requestMetadata;
    }

    public void setRequestMetadata(UserRequestMetadata requestMetadata) {
        this.requestMetadata = requestMetadata;
    }
    
    @Override
    public boolean equals( Object o ) {
        return (o instanceof GuestUser);
    }

    @Override
    public int hashCode() {
        return 7;
    }
    
}
