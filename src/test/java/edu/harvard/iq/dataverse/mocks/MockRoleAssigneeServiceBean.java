package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author michael
 */
public class MockRoleAssigneeServiceBean extends RoleAssigneeServiceBean {
    
    Map<String, RoleAssignee> assignees = new HashMap<>();
    
    public <T extends RoleAssignee> T add (T ra ) {
        assignees.put(ra.getIdentifier(), ra);
        return ra;
    }
    
    @Override
    public RoleAssignee getRoleAssignee(String identifier) {
        if ( predefinedRoleAssignees.isEmpty() ) {
            setup();
        }
        
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty string.");
        }
        switch (identifier.charAt(0)) {
            case ':':
                return predefinedRoleAssignees.get(identifier);
            case '@':
                return assignees.get(identifier);
            case '&':
                return assignees.get(identifier);
            case '#':
                throw new IllegalArgumentException("private url users not supported in test - it might be easy to add, though.");
            default:
                throw new IllegalArgumentException("Unsupported assignee identifier '" + identifier + "'");
        }
    }
    
}
