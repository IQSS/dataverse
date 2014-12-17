package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Creates {@link IpGroup}s.
 * @author michael
 */
@Stateless
public class IpGroupProvider implements GroupProvider<IpGroup> {
    
    @PersistenceContext()
    private EntityManager em;
    
    @EJB
    IpGroupsServiceBean ipGroupsService;
    
    
    @Override
    public String getGroupProviderAlias() {
        return "IpGroupProvider";
    }

    @Override
    public String getGroupProviderInfo() {
        return "Groups users by their current IP address";
    }

    @Override
    public Set<IpGroup> groupsFor(User u) {
        return ipGroupsService.findAllIncludingIp(u.getRequestMetadata().getIpAddress());
    }

    
}
