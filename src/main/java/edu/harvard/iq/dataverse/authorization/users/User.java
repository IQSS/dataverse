package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.io.Serializable;

/**
 * @todo make into an abstract class to override getIdentifier method (prepend
 * "u:").
 */
public interface User extends RoleAssignee, Serializable {

    public boolean isAuthenticated();

    // TODO remove this, should be handles in a more generic fashion,
    // e.g. getUserProvider and get the provider's URL from there. This
    // would allow Shib-based editing as well.
    public boolean isBuiltInUser();
    
    public boolean isSuperuser();

    // TODO remove when we reverse the UserRequest and User composition.
    public UserRequestMetadata getRequestMetadata();
    
    public void setRequestMetadata( UserRequestMetadata mtd );
}
