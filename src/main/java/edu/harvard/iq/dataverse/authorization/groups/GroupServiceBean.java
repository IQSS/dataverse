package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.BuiltInGroupsProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupsServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class GroupServiceBean {
    private static final Logger logger = Logger.getLogger(GroupServiceBean.class.getName());
    
    @EJB
    IpGroupsServiceBean ipGroupsService;
    @EJB
    ShibGroupServiceBean shibGroupService;
    
    private final Map<String, GroupProvider> groupProviders = new HashMap<>();
    
    private IpGroupProvider ipGroupProvider;
    private ShibGroupProvider shibGroupProvider;
    
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
    @PostConstruct
    public void setup() {
        addGroupProvider( BuiltInGroupsProvider.get() );
            addGroupProvider( ipGroupProvider = new IpGroupProvider(ipGroupsService) );
        addGroupProvider(shibGroupProvider = new ShibGroupProvider(shibGroupService));
    }

    public Group getGroup( String groupAlias ) {
        String[] comps = groupAlias.split( Group.PATH_SEPARATOR, 2 );
        GroupProvider gp = groupProviders.get( comps[0] );
        if ( gp == null ) {
            logger.log(Level.WARNING, "Cannot find group provider with alias {0}", comps[0]);
            return null;
        }
        return gp.get( comps[1] );
    }

    public IpGroupProvider getIpGroupProvider() {
        return ipGroupProvider;
    }

    public ShibGroupProvider getShibGroupProvider() {
        return shibGroupProvider;
    }

    public Set<Group> groupsFor( User u ) {
        Set<Group> groups = new HashSet<>();
        for ( GroupProvider gv : groupProviders.values() ) {
            groups.addAll( gv.groupsFor(u) );
        }
        return groups;
    }
    
    public Set<Group> findAllGroups() {
        Set<Group> groups = new HashSet<>();
        for ( GroupProvider gp : groupProviders.values() ) {
            groups.addAll( gp.findAll() );
        }
        return groups;
    }
    
    private void addGroupProvider( GroupProvider gp ) {
        groupProviders.put( gp.getGroupProviderAlias(), gp );
    }
}
