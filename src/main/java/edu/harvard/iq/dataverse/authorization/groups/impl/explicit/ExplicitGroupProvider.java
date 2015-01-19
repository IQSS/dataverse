package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Set;

/**
 * Creates and manages explicit groups. Also provides services they might need.
 * @author michael
 */
public class ExplicitGroupProvider implements GroupProvider {
    
    
    public Group getGroup( String groupID ) {
        // TODO - when we get groups, delegate to the groups bean.
        // 
        return null;
    }

    @Override
    public String getGroupProviderAlias() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getGroupProviderInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set groupsFor(User u) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group get(String groupAlias) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
