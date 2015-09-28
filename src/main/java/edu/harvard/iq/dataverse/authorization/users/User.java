package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.io.Serializable;

/**
 * A user of the dataverse system. Intuitively a single real person in real life, but
 * some corner cases exist (e.g. {@link GuestUser}, who stands for many people).
 */
public interface User extends RoleAssignee, Serializable {

    public boolean isAuthenticated();

    // TODO remove this, should be handles in a more generic fashion,
    // e.g. getUserProvider and get the provider's URL from there. This
    // would allow Shib-based editing as well.
    public boolean isBuiltInUser();
    
    public boolean isSuperuser();

}
