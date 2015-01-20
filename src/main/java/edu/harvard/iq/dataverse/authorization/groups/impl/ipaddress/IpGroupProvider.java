package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.HashSet;
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
        return updateProvider(ipGroupsService.findAllIncludingIp(u.getRequestMetadata().getIpAddress()));
    }

    @Override
    public IpGroup get(String groupAlias) {
        return setProvider(ipGroupsService.getByGroupName(groupAlias));
    }
    
    public IpGroup get(Long id) {
        return setProvider(ipGroupsService.get(id));
    }
    
    @Override
    public Set<IpGroup> findAll() {
        return updateProvider( new HashSet<>(ipGroupsService.findAll()));
    }
    
    private IpGroup setProvider( IpGroup g ) {
        g.setProvider(this);
        return g;
    }
    
    private Set<IpGroup> updateProvider( Set<IpGroup> groups ) {
        for ( IpGroup g : groups ) {
            g.setProvider(this);
        }
        return groups;
    }
    
    public IpGroup store(IpGroup grp) {
        grp.setProvider(this);
        return ipGroupsService.store(grp);
    }

    public void deleteGroup(IpGroup grp) {
        ipGroupsService.deleteGroup(grp);
    }
}
