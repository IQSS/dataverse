package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.toSet;

/**
 *
 * @author michael
 */
public class MockExplicitGroupService extends ExplicitGroupServiceBean {
    
    private Map<Long, ExplicitGroup> groups = new HashMap<>();
    
    public ExplicitGroup registerGroup( ExplicitGroup grp ) {
        groups.put(grp.getId(), grp);
        return grp;
    }

    @Override
    public Set<ExplicitGroup> findDirectlyContainingGroups(RoleAssignee ra) {
        return groups.values().stream()
                .filter( g -> g.getDirectMembers().contains(ra) )
                .collect(toSet());
    }
    
    
}
