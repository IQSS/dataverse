package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;

/**
 * Creates and managed explicit groups. Also provides services they might need.
 * @author michael
 */
public class ExplicitGroupCreator implements GroupCreator {

    @Override
    public ExplicitGroup createGroup() {
        ExplicitGroup grp = new ExplicitGroup();
        
        return grp;
    }
    
    
    public Group getGroup( String groupID ) {
        // FIXME delegate to the groups bean.
        return null;
    }
}
