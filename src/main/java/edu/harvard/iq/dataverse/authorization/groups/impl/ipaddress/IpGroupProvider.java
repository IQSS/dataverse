package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
            final Set<IpGroup> groups = updateProvider( ipGroupsService.findAllIncludingIp(req.getSourceAddress()) );
            
            { // FIXME remove this block before merge to DEV
            final IpAddress sourceAddress = req.getSourceAddress();
            logger.log(Level.INFO, "ip: {1} ({2}) groups: {0}", new Object[]{
                groups.stream().map( g->"[" + g.getId() + " " + g.getDisplayName()+"]").collect(Collectors.joining(",")), 
                sourceAddress});
            }
            
            return groups;
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
