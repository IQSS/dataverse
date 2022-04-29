package edu.harvard.iq.dataverse.authorization.groups.impl.saml;

import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.SamlGroup;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class SamlGroupProvider implements GroupProvider<SamlGroup> {

    private static final Logger logger = Logger.getLogger(SamlGroupProvider.class.getCanonicalName());

    private final SamlGroupService samlGroupService;

    // -------------------- CONSTRUCTORS --------------------

    public SamlGroupProvider(SamlGroupService samlGroupService) {
        this.samlGroupService = samlGroupService;
    }

    // -------------------- GETTERS --------------------

    public static String getSamlProviderAlias() {
        return SamlGroup.GROUP_TYPE;
    }

    @Override
    public String getGroupProviderAlias() {
        return getSamlProviderAlias();
    }

    @Override
    public String getGroupProviderInfo() {
        return "Groups users based on Saml IdP entityId received from their institution's authentication system.";
    }

    // -------------------- LOGIC --------------------

    @Override
    public Set<SamlGroup> groupsFor(DataverseRequest req, DvObject dvo) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<SamlGroup> groupsFor(RoleAssignee ra, DvObject dvo) {
        return groupsFor(ra);
    }

    @Override
    public Set<SamlGroup> groupsFor(DataverseRequest req) {
        return groupsFor(req.getUser());
    }

    @Override
    public Set<SamlGroup> groupsFor(RoleAssignee ra) {
        return ra instanceof AuthenticatedUser
                ? samlGroupService.findFor((AuthenticatedUser) ra)
                : Collections.emptySet();
    }

    @Override
    public Set<SamlGroup> findGlobalGroups() {
        return new HashSet<>(samlGroupService.findAll());
    }

    @Override
    public SamlGroup get(String groupId) {
        long samlGroupPrimaryKey;
        try {
            samlGroupPrimaryKey = Long.parseLong(groupId);
        } catch (NumberFormatException ex) {
            logger.info("Could not convert \"" + groupId + "\" into a long.");
            return null;
        }
        return samlGroupService.findById(samlGroupPrimaryKey);
    }

    public SamlGroup persist(SamlGroup samlGroup) {
        return samlGroupService.save(samlGroup.getName(), samlGroup.getEntityId());
    }

    public void delete(SamlGroup doomed) {
        samlGroupService.delete(doomed);
    }

    @Override
    public Class<SamlGroup> providerFor() {
        return SamlGroup.class;
    }

    @Override
    public boolean contains(DataverseRequest aRequest, SamlGroup group) {
        return groupsFor(aRequest).contains(group);
    }
}
