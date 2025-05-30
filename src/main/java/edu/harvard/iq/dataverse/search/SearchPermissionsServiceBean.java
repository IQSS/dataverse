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
            permStrings.add(IndexServiceBean.getPublicGroupString());
        }
        permStrings.addAll(findDvObjectPerms(dataverse));
        return permStrings;
    }
    
    public List<String> findDatasetVersionPerms(DatasetVersion version) {
        List<String> perms = new ArrayList<>();
        if (version.isReleased()) {
            perms.add(IndexServiceBean.getPublicGroupString());
        }

        perms.addAll(findDvObjectPerms(version.getDataset()));
        return perms;
    }

    public List<String> findDvObjectPerms(DvObject dvObject) {
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

    public Map<DatasetVersion.VersionState, Boolean> getDesiredCards(Dataset dataset) {
        Map<DatasetVersion.VersionState, Boolean> desiredCards = new LinkedHashMap<>();
        DatasetVersion latestVersion = dataset.getLatestVersion();
        DatasetVersion.VersionState latestVersionState = latestVersion.getVersionState();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        boolean atLeastOnePublishedVersion = false;
        if (releasedVersion != null) {
            atLeastOnePublishedVersion = true;
        } else {
            atLeastOnePublishedVersion = false;
        }

        if (atLeastOnePublishedVersion == false) {
            if (latestVersionState.equals(DatasetVersion.VersionState.DRAFT)) {
                desiredCards.put(DatasetVersion.VersionState.DRAFT, true);
                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {
                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, true);
                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
            } else {
                String msg = "No-op. Unexpected condition reached: There is no published version and the latest published version is neither " + DatasetVersion.VersionState.DRAFT + " nor " + DatasetVersion.VersionState.DEACCESSIONED + ". Its state is " + latestVersionState + ".";
                logger.info(msg);
            }
        } else if (atLeastOnePublishedVersion == true) {
            if (latestVersionState.equals(DatasetVersion.VersionState.RELEASED)
                    || latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {
                desiredCards.put(DatasetVersion.VersionState.RELEASED, true);
                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DRAFT)) {
                desiredCards.put(DatasetVersion.VersionState.DRAFT, true);
                desiredCards.put(DatasetVersion.VersionState.RELEASED, true);
                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
            } else {
                String msg = "No-op. Unexpected condition reached: There is at least one published version but the latest version is neither published nor draft";
                logger.info(msg);
            }
        } else {
            String msg = "No-op. Unexpected condition reached: Has a version been published or not?";
            logger.info(msg);
        }
        return desiredCards;
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

}
