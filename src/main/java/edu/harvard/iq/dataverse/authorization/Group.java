package edu.harvard.iq.dataverse.authorization;

import java.util.ArrayList;
import java.util.Collection;

public interface Group {

    String name = new String();
    String description = new String();
//    AuthenticationProvider authenticationProvider;
    Collection<RoleAssignee> roleAssignees = new ArrayList<>();

}
