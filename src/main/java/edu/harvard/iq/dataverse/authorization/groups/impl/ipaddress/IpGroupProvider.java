package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Set;

/**
 * Creates {@link IpGroup}s.
 * @author michael
 */
public class IpGroupProvider implements GroupProvider<IpGroup> {
    
    private final IpGroupsServiceBean ipGroupsService;

    public IpGroupProvider(IpGroupsServiceBean ipGroupsService) {
        this.ipGroupsService = ipGroupsService;
    }
    
    @Override
    public String getGroupProviderAlias() {
        return "ip";
    }

    @Override
    public String getGroupProviderInfo() {
        return "Groups users by their current IP address";
    }

    @Override
    public Set<IpGroup> groupsFor(User u) {
        return ipGroupsService.findAllIncludingIp(u.getRequestMetadata().getIpAddress());
    }

    @Override
    public IpGroup get(String groupAlias) {
        return ipGroupsService.getByGroupName(groupAlias );
    }

    
}
