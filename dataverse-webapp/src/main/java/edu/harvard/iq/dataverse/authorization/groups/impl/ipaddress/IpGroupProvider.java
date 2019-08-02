package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates {@link IpGroup}s.
 *
 * @author michael
 */
public class IpGroupProvider implements GroupProvider<IpGroup> {

    private final IpGroupsServiceBean ipGroupsService;

    public IpGroupProvider(IpGroupsServiceBean ipGroupsService) {
        this.ipGroupsService = ipGroupsService;
    }

    @Override
    public String getGroupProviderAlias() {
        return IpGroup.GROUP_TYPE;
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
    public Set<IpGroup> groupsFor(RoleAssignee ra) {
        return Collections.emptySet();
    }

    @Override
    public Set<IpGroup> groupsFor(DataverseRequest req, DvObject dvo) {
        return groupsFor(req);
    }

    @Override
    public Set<IpGroup> groupsFor(DataverseRequest req) {
        if (req.getSourceAddress() != null) {
            return ipGroupsService.findAllIncludingIp(req.getSourceAddress());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public IpGroup get(String groupAlias) {
        return ipGroupsService.getByGroupName(groupAlias);
    }

    public IpGroup get(Long id) {
        return ipGroupsService.get(id);
    }

    @Override
    public Set<IpGroup> findGlobalGroups() {
        return new HashSet<>(ipGroupsService.findAll());
    }

    public IpGroup store(IpGroup grp) {
        final IpGroup storedGroup = ipGroupsService.store(grp);
        return storedGroup;
    }

    public void deleteGroup(IpGroup grp) {
        ipGroupsService.deleteGroup(grp);
    }

    /**
     * Finds an available name for an IP group. The name is based on the {@code base}
     * parameter, but may be changed in case there's already a group with that name.
     *
     * <strong>
     * Note: This method might fail under very heavy loads. But we do not expect
     * heavy creation of IP groups at this point.
     * </strong>
     *
     * @param base A base name.
     * @return An available group name.
     */
    public String findAvailableName(String base) {
        if (ipGroupsService.getByGroupName(base) == null) {
            return base;
        }
        int i = 1;
        while (ipGroupsService.getByGroupName(base + "-" + i) != null) {
            i++;
        }
        return base + "-" + i;
    }

    @Override
    public Class<IpGroup> providerFor() {
        return IpGroup.class;
    }

    @Override
    public boolean contains(DataverseRequest aRequest, IpGroup group) {
        IpAddress addr = aRequest.getSourceAddress();
        return (addr != null) && group.containsAddress(addr);
    }
}
