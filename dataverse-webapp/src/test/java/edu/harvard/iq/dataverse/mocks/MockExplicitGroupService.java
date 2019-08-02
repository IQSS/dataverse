package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * @author michael
 */
public class MockExplicitGroupService extends ExplicitGroupServiceBean {

    private ExplicitGroupProvider provider;
    private Map<Long, ExplicitGroup> groups = new HashMap<>();

    public MockExplicitGroupService(RoleAssigneeServiceBean roleAssigneeService) {
        this.roleAssigneeSvc = roleAssigneeService;
    }
    
    public ExplicitGroup registerGroup(ExplicitGroup grp) {
        groups.put(grp.getId(), grp);
        return grp;
    }

    public void registerProvider(ExplicitGroupProvider provider) {
        this.provider = provider;
    }

    @Override
    public ExplicitGroupProvider getProvider() {
        return provider;
    }
    
    @Override
    public Set<ExplicitGroup> findDirectlyContainingGroups(RoleAssignee ra) {
        return groups.values().stream()
                .filter(g -> this.getDirectMembers(g).contains(ra))
                .collect(toSet());
    }


}
