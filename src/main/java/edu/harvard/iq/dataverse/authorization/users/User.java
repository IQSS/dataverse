package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.io.Serializable;

/**
 * @todo make into an abstract class to override getIdentifier method (prepend
 * "u:").
 */
public interface User extends RoleAssignee, Serializable {

    public boolean isAuthenticated();
            

   public boolean isBuiltInUser();

   public boolean canResetPassword();
  }
