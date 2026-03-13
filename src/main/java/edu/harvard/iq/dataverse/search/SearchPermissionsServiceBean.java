package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

/**
 * Determine whether items should be searchable.
 */
@Stateless
@Named
public class SearchPermissionsServiceBean {

    private static final Logger logger = Logger.getLogger(SearchPermissionsServiceBean.class.getCanonicalName());

    @EJB
    AuthenticationServiceBean userServiceBean;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    DataverseRoleServiceBean rolesSvc;
    @EJB
    AuthenticationServiceBean authSvc;
    @EJB
    GroupServiceBean groupService;
    @EJB
    SettingsServiceBean settingsService;

    /**
     * @todo Should we make a PermStrings object? Probably.
     *
     * @return A list of strings on which Solr will JOIN to enforce permissions
     */
    public List<String> findDataversePerms(Dataverse dataverse) {
        List<String> permStrings = new ArrayList<>();
        if (hasBeenPublished(dataverse)) {
            Set<String> raIds = dataverse.getLocallyFAIRRoleAssigneeIdentifiers();
            if (raIds.isEmpty()) {
                permStrings.add(IndexServiceBean.getPublicGroupString());
            } else {
                raIds.stream()
                .map(this::convertToIndexableString)
                .filter(s -> s != null)
                .forEach(permStrings::add);
            }
        }
        // And anyone who has permission to view the unpublished version
        permStrings.addAll(findDvObjectPerms(dataverse));
        return permStrings;
    }

    public List<String> findDatasetVersionPerms(DatasetVersion version) {
        List<String> perms = new ArrayList<>();
        if (version.isReleased()) {
            Set<String> raIds = version.getDataset().getOwner().getLocallyFAIRRoleAssigneeIdentifiers();
            if (raIds.isEmpty()) {
                perms.add(IndexServiceBean.getPublicGroupString());
            } else {
                raIds.stream()
                .map(this::convertToIndexableString)
                .filter(s -> s != null)
                .forEach(perms::add);
            }

        }
        // And anyone who has permission to view the unpublished version
        perms.addAll(findDvObjectPerms(version.getDataset()));
        return perms;
    }

    private List<String> findDvObjectPerms(DvObject dvObject) {
        List<String> permStrings = new ArrayList<>();
        Permission p = getRequiredSearchPermission(dvObject);

       List<String> assigneeIdStrings = null;
           assigneeIdStrings = roleAssigneeService.findAssigneesWithPermissionOnDvObject(dvObject.getId(), p);
        for (String id : assigneeIdStrings) {
            RoleAssignee userOrGroup = roleAssigneeService.getRoleAssignee(id);
            String indexableUserOrGroupPermissionString = getIndexableStringForUserOrGroup(userOrGroup);
            if (indexableUserOrGroupPermissionString != null) {
                permStrings.add(indexableUserOrGroupPermissionString);
            }
        }
        return permStrings;
    }

    private boolean hasBeenPublished(Dataverse dataverse) {
        return dataverse.isReleased();
    }

    private Permission getRequiredSearchPermission(DvObject dvObject) {
        if (dvObject.isInstanceofDataverse()) {
            return Permission.ViewUnpublishedDataverse;
        } else {
            return Permission.ViewUnpublishedDataset;
        }

    }

    /**
     * From a Solr perspective we can't just index any string when we go to do
     * the JOIN to enforce security. (Maybe putting quotes around the string at
     * search time would allow this.) For users, we index the primary key from
     * the AuthenticatedUsers table. For groups we index the "alias" which
     * should be globally unique because non-builtin groups have a sort of a
     * name space with "shib/2" and "ip/ipGroup3", for example.
     */
    private String getIndexableStringForUserOrGroup(RoleAssignee userOrGroup) {
        if (userOrGroup instanceof AuthenticatedUser) {
            logger.fine(userOrGroup.getIdentifier() + " must be a user: " + userOrGroup.getClass().getName());
            AuthenticatedUser au = (AuthenticatedUser) userOrGroup;
            // Strong prefence to index based on system generated value (e.g. primary key) whenever possible: https://github.com/IQSS/dataverse/issues/1151
            Long primaryKey = au.getId();
            return IndexServiceBean.getGroupPerUserPrefix() + primaryKey;
        } else if (userOrGroup instanceof Group) {
            logger.fine(userOrGroup.getIdentifier() + " must be a group: " + userOrGroup.getClass().getName());
            Group group = (Group) userOrGroup;
            logger.fine("group: " + group.getAlias());
            String groupAlias = group.getAlias();
            if (groupAlias != null) {
                return IndexServiceBean.getGroupPrefix() + groupAlias;
            } else {
                logger.fine("Could not find group alias for " + group.getIdentifier());
                return null;
            }
        } else {
            return null;
        }
    }
    

/**
 * Converts a single role assignee identifier (e.g., "@john.doe", "&admins") to its
 * indexable form for Solr (e.g., "user_1", "group_admins") w/o any db lookup for groups.
 * 
 * @param identifier Identifier prefixed with @ (user) or & (group)
 * @return Indexable string for Solr, or null if conversion fails
 */
public String convertToIndexableString(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
        return null;
    }
    
    char prefix = identifier.charAt(0);
    String value = identifier.substring(1);
    
    if (prefix == '@') {
        // User identifier - need to extract the numeric ID
        // Format: @userIdentifier -> user_<primaryKey>
        AuthenticatedUser user = authSvc.getAuthenticatedUser(value);
        if (user != null) {
            return IndexServiceBean.getGroupPerUserPrefix() + user.getId();
        } else {
            logger.fine("Could not find user for identifier: " + identifier);
            return null;
        }
    } else if (prefix == '&') {
        // Group alias - can use directly
        // Format: &groupAlias -> group_groupAlias
        return IndexServiceBean.getGroupPrefix() + value;
    } else {
        logger.warning("Unknown role assignee identifier format: " + identifier);
        return null;
    }
}


}
