package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

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
    AuthenticationServiceBean authSvc;

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
        permStrings.addAll(findDirectAssignments(dataverse));
        return permStrings;
    }

    public List<String> findDatasetVersionPerms(DatasetVersion version) {
        List<String> perms = new ArrayList<>();
        if (version.isReleased()) {
            perms.add(IndexServiceBean.getPublicGroupString());
        } else {
            /**
             * @todo this shouldn't be in an else (data related to me)
             */
            perms.addAll(findDirectAssignments(version.getDataset()));
        }
        return perms;
    }

    private List<String> findDirectAssignments(DvObject dvObject) {
        List<String> permStrings = new ArrayList<>();
        List<RoleAssignee> roleAssignees = findWhoCanSearch(dvObject);
        for (RoleAssignee roleAssignee : roleAssignees) {
            AuthenticatedUser au = findAuthUser(roleAssignee);
            if (au != null) {
                permStrings.add(IndexServiceBean.getGroupPerUserPrefix() + au.getId());
            } else {
                RoleAssignee group = findGroup(roleAssignee);
                if (group != null) {
                    permStrings.add(IndexServiceBean.getGroupPrefix() + "FIXME groupId");
                }
            }
        }
        return permStrings;
    }

    private List<RoleAssignee> findWhoCanSearch(DvObject dvObject) {
        List<RoleAssignee> emptyList = new ArrayList<>();
        List<RoleAssignee> peopleWhoCanSearch = emptyList;

        List<RoleAssignment> assignmentsOn = permissionService.assignmentsOn(dvObject);
        for (RoleAssignment roleAssignment : assignmentsOn) {
            if (roleAssignment.getRole().permissions().contains(Permission.Discover)) {
                RoleAssignee userOrGroup = roleAssigneeService.getRoleAssignee(roleAssignment.getAssigneeIdentifier());
                AuthenticatedUser au = findAuthUser(userOrGroup);
                if (au != null) {
                    peopleWhoCanSearch.add(userOrGroup);
                } else {
                    RoleAssignee group = findGroup(userOrGroup);
                    if (group != null) {
                        peopleWhoCanSearch.add(group);
                    }
                }
            }
        }
        return peopleWhoCanSearch;
    }

    private AuthenticatedUser findAuthUser(RoleAssignee roleAssignee) {
        String assigneeIdentifier = roleAssignee.getIdentifier();
        if (assigneeIdentifier == null) {
            return null;
        }
        String identifierWithoutPrefix = null;
        try {
            String prefix = AuthenticatedUser.IDENTIFIER_PREFIX;
            int indexAfterPrefix = prefix.length();
            identifierWithoutPrefix = assigneeIdentifier.substring(indexAfterPrefix);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
        if (identifierWithoutPrefix == null) {
            return null;
        }
        AuthenticatedUser au = userServiceBean.getAuthenticatedUser(identifierWithoutPrefix);
        return au;
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
            }
        } else {
            String msg = "No-op. Unexpected condition reached: Has a version been published or not?";
        }
        return desiredCards;
    }

    private boolean hasBeenPublished(Dataverse dataverse) {
        return dataverse.isReleased();
    }

    /**
     * @todo Once groups have been implemented, try to look up the group from
     * the roleAssignee and return it.
     */
    private RoleAssignee findGroup(RoleAssignee roleAssignee) {
        return null;
    }

}
