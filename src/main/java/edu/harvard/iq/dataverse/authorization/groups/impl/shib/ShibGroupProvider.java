package edu.harvard.iq.dataverse.authorization.groups.impl.shib;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ShibGroupProvider implements GroupProvider<ShibGroup> {

    private static final Logger logger = Logger.getLogger(ShibGroupProvider.class.getCanonicalName());

    private final ShibGroupServiceBean shibGroupService;

    public ShibGroupProvider(ShibGroupServiceBean shibGroupService) {
        this.shibGroupService = shibGroupService;
    }

    public static String getShibProviderAlias() {
        return "shib";
    }

    @Override
    public String getGroupProviderAlias() {
        return getShibProviderAlias();
    }

    @Override
    public String getGroupProviderInfo() {
        return "Groups users based on Shibboleth attributes received from their institution's authentication system.";
    }

    @Override
    public Set<ShibGroup> groupsFor( DataverseRequest req, DvObject dvo ) {
        return groupsFor(req.getUser());
    }
    
    @Override
    public Set<ShibGroup> groupsFor(RoleAssignee ra, DvObject dvo) {
        return groupsFor(ra);
    }
    
    @Override
    public Set<ShibGroup> groupsFor( DataverseRequest req) {
        return groupsFor(req.getUser());
    }
    
    @Override
    public Set<ShibGroup> groupsFor(RoleAssignee ra) {
        if (ra instanceof AuthenticatedUser) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) ra;
            Set<ShibGroup> groupsFor = shibGroupService.findFor(authenticatedUser);
            for (ShibGroup shibGroup : groupsFor) {
                shibGroup.setShibGroupProvider(this);
            }
            return groupsFor;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<ShibGroup> findGlobalGroups() {
        Set<ShibGroup> allShibGroups = new HashSet<>();
        for (ShibGroup shibGroup : shibGroupService.findAll()) {
            allShibGroups.add(shibGroup);
        }
        return allShibGroups;
    }

    @Override
    public ShibGroup get(String groupAlias) {
        long shibGroupPrimaryKey = 0;
        try {
            shibGroupPrimaryKey = Long.parseLong(groupAlias);
        } catch (NumberFormatException ex) {
            logger.info("Could not convert \"" + groupAlias + "\" into a long.");
            return null;
        }
        ShibGroup shibGroup = shibGroupService.findById(shibGroupPrimaryKey);
        return shibGroup;
    }

    public ShibGroup persist(ShibGroup shibGroup) {
        ShibGroup persistedShibGroupOrNull = shibGroupService.save(shibGroup.getName(), shibGroup.getAttribute(), shibGroup.getPattern());
        return persistedShibGroupOrNull;
    }

    public boolean delete(ShibGroup doomed) throws Exception {
        boolean response = shibGroupService.delete(doomed);
        return response;
    }

}
