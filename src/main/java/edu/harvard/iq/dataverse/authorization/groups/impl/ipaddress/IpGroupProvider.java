package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Creates {@link IpGroup}s.
 * @author michael
 */
public class IpGroupProvider implements GroupProvider<IpGroup> {
    
    private static final Logger logger = Logger.getLogger(IpGroupProvider.class.getCanonicalName());

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
    public Set<IpGroup> groupsFor(RoleAssignee ra, DvObject o) {
        return Collections.emptySet();
    }
    
    @Override
    public Set<IpGroup> groupsFor( DataverseRequest req, DvObject dvo ) {
        if ( req.getSourceAddress() != null ) {
            return updateProvider( ipGroupsService.findAllIncludingIp(req.getSourceAddress()) );
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public IpGroup get(String groupAlias) {
        return setProvider(ipGroupsService.getByGroupName(groupAlias));
    }
    
    public IpGroup get(Long id) {
        return setProvider(ipGroupsService.get(id));
    }
    
    @Override
    public Set<IpGroup> findGlobalGroups() {
        return updateProvider( new HashSet<>(ipGroupsService.findAll()) );
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
