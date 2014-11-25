package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * The place to obtain {@link RoleAssignee}s, based on their identifiers.
 * 
 * @author michael
 */
@Stateless
@Named
public class RoleAssigneeServiceBean {
    
    // ADD GROUP SERVICE BEAN HERE
    
    @EJB
    AuthenticationServiceBean authSvc;
    
    Map<String, RoleAssignee> predefinedRoleAssignees = new TreeMap<>();
    
    @PostConstruct
    void setup() {
        predefinedRoleAssignees.put(GuestUser.get().getIdentifier(), GuestUser.get());
        predefinedRoleAssignees.put(AuthenticatedUsers.get().getIdentifier(), AuthenticatedUsers.get());
    }
    
    public RoleAssignee getRoleAssignee( String identifier ) {
        switch ( identifier.charAt(0) ) {
            case ':':
                return predefinedRoleAssignees.get(identifier);
            case '@':
                return authSvc.getAuthenticatedUser(identifier.substring(1));
            case '&':
                throw new IllegalArgumentException("Groups not supported yet");
            default: 
                throw new IllegalArgumentException("Unsupported assignee identifier '" + identifier + "'");
        }
    }
}
