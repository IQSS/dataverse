package edu.harvard.iq.dataverse.authorization.groups.impl.mail;

import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MailDomainGroupProvider implements GroupProvider<MailDomainGroup> {

    private MailDomainGroupService mailDomainGroupService;

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainGroupProvider(MailDomainGroupService mailDomainGroupService) {
        this.mailDomainGroupService = mailDomainGroupService;
    }

    // -------------------- LOGIC --------------------

    @Override
    public Class<MailDomainGroup> providerFor() {
        return MailDomainGroup.class;
    }

    @Override
    public String getGroupProviderAlias() {
        return MailDomainGroup.GROUP_TYPE;
    }

    @Override
    public String getGroupProviderInfo() {
        return "Groups users by mail domain";
    }

    @Override
    public Set<MailDomainGroup> groupsFor(RoleAssignee roleAssignee, DvObject dvo) {
        return groupsFor(roleAssignee);
    }

    @Override
    public Set<MailDomainGroup> groupsFor(RoleAssignee roleAssignee) {
        return roleAssignee instanceof AuthenticatedUser
                ? mailDomainGroupService.getGroupsForUser((AuthenticatedUser) roleAssignee)
                : Collections.emptySet();
    }

    @Override
    public Set<MailDomainGroup> groupsFor(DataverseRequest request, DvObject dvo) {
        return groupsFor(request);
    }

    @Override
    public Set<MailDomainGroup> groupsFor(DataverseRequest request) {
        AuthenticatedUser authenticatedUser = request.getAuthenticatedUser();
        return authenticatedUser != null
                ? mailDomainGroupService.getGroupsForUser(authenticatedUser)
                : Collections.emptySet();
    }

    @Override
    public MailDomainGroup get(String alias) {
        return mailDomainGroupService.getGroup(alias)
                .orElse(null);
    }

    @Override
    public Set<MailDomainGroup> findGlobalGroups() {
        return new HashSet<>(mailDomainGroupService.getAllGroups());
    }

    @Override
    public boolean contains(DataverseRequest request, MailDomainGroup group) {
        return groupsFor(request).contains(group);
    }
}
