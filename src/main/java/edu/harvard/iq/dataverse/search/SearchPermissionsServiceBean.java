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
     * Method used by Search API for showing the document(s) (and their
     * permissions) we'd like to index into Solr based on the dvObjectId.
     */
    public List<DvObjectSolrDoc> determineSolrDocs(Long dvObjectId) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
        if (dvObject == null) {
            return emptyList;
        }
        if (dvObject.isInstanceofDataverse()) {
            DvObjectSolrDoc dataverseSolrDoc = constructDataverseSolrDoc((Dataverse) dvObject);
            solrDocs.add(dataverseSolrDoc);
        } else if (dvObject.isInstanceofDataset()) {
            List<DvObjectSolrDoc> datasetSolrDocs = constructDatasetSolrDocs((Dataset) dvObject);
            solrDocs.addAll(datasetSolrDocs);
        } else if (dvObject.isInstanceofDataFile()) {
            /**
             * @todo constructFileSolrDocs
             */
        } else {
            logger.info("Unexpected DvObject: " + dvObject.getClass().getName());
        }

        return solrDocs;
    }

    /**
     * Get a list of "perm strings" suitable for indexing as an array into Solr.
     *
     * @param dvObjectId The database id of the DvObject
     * @return A list of strings on which Solr will JOIN to enforce permissions
     */
    private List<String> findDataversePerms(Dataverse dataverse) {
        List<String> permStrings = new ArrayList<>();

        permStrings.addAll(findSuperUserPermStrings());

        if (hasBeenPublished(dataverse)) {
            permStrings.add(IndexServiceBean.getPublicGroupString());
        }

        List<RoleAssignee> roleAssignees = findWhoCanSearch(dataverse.getId());
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

    /**
     * Find who can search or browse something.
     *
     * @param dvObjectId The database id of the DvObject
     * @return A list of users and groups (RoleAssignee objects) who should have
     */
    public List<RoleAssignee> findWhoCanSearch(Long dvObjectId) {
        List<RoleAssignee> emptyList = new ArrayList<>();
        List<RoleAssignee> peopleWhoCanSearch = emptyList;
        DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
        if (dvObject == null) {
            return emptyList;
        }
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

    /**
     * @todo Once groups have been implemented, try to look up the group from
     * the roleAssignee and return it.
     */
    private RoleAssignee findGroup(RoleAssignee roleAssignee) {
        return null;
    }

    private boolean hasBeenPublished(Dataverse dataverse) {
        return dataverse.isReleased();
    }

    private DvObjectSolrDoc constructDataverseSolrDoc(Dataverse dataverse) {
        List<String> perms = findDataversePerms(dataverse);
        DvObjectSolrDoc dvDoc = new DvObjectSolrDoc(IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId(), dataverse.getName(), perms);
        return dvDoc;
    }

    private List<DvObjectSolrDoc> constructDatasetSolrDocs(Dataset dataset) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;

        Map<DatasetVersion.VersionState, Boolean> desiredCards = getDesiredCards(dataset);

        for (DatasetVersion version : datasetVersionsWeBuildCardsFor(dataset)) {
            if (version != null) {
                boolean cardShouldExist = desiredCards.get(version.getVersionState());
                if (cardShouldExist) {
                    DvObjectSolrDoc datasetSolrDoc = makeDatasetSolrDoc(version);
                    solrDocs.add(datasetSolrDoc);
                }
            }
        }

        return solrDocs;
    }

    private List<DatasetVersion> datasetVersionsWeBuildCardsFor(Dataset dataset) {
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        DatasetVersion latest = dataset.getLatestVersion();
        DatasetVersion released = dataset.getReleasedVersion();
        datasetVersions.add(latest);
        datasetVersions.add(released);
        return datasetVersions;
    }

    private Map<DatasetVersion.VersionState, Boolean> getDesiredCards(Dataset dataset) {
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

    private DvObjectSolrDoc makeDatasetSolrDoc(DatasetVersion version) {
        String solrIdStart = IndexServiceBean.solrDocIdentifierDataset + version.getDataset().getId().toString();
        String solrIdEnd = getDatasetSolrIdEnding(version.getVersionState());
        String solrId = solrIdStart + solrIdEnd;
        String name = version.getTitle();
        List<String> perms = findDatasetPerms(version);
        return new DvObjectSolrDoc(solrId, name, perms);
    }

    private String getDatasetSolrIdEnding(DatasetVersion.VersionState versionState) {
        if (versionState.equals(DatasetVersion.VersionState.RELEASED)) {
            return "";
        } else if (versionState.equals(DatasetVersion.VersionState.DRAFT)) {
            return IndexServiceBean.draftSuffix;
        } else if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {
            return IndexServiceBean.deaccessionedSuffix;
        } else {
            return "_unexpectedDatasetVersion";
        }
    }

    private List<String> findDatasetPerms(DatasetVersion version) {
        List<String> perms = new ArrayList<>();
        perms.addAll(findSuperUserPermStrings());
        if (version.isReleased()) {
            perms.add(IndexServiceBean.getPublicGroupString());
        } else {
            List<RoleAssignee> roleAssignees = findWhoCanSearch(version.getDataset().getId());
            /**
             * @DRY! Copied from dataverse method
             */
            for (RoleAssignee roleAssignee : roleAssignees) {
                AuthenticatedUser au = findAuthUser(roleAssignee);
                if (au != null) {
                    perms.add(IndexServiceBean.getGroupPerUserPrefix() + au.getId());
                } else {
                    RoleAssignee group = findGroup(roleAssignee);
                    if (group != null) {
                        perms.add(IndexServiceBean.getGroupPrefix() + "FIXME groupId");
                    }
                }
            }

        }
        /**
         * @todo Figure out permissions for the passed in dataset version
         */
        return perms;
    }

    private List<String> findSuperUserPermStrings() {
        List<String> superUserPermStrings = new ArrayList<>();
        List<AuthenticatedUser> superusers = authSvc.findSuperUsers();
        for (AuthenticatedUser superuser : superusers) {
            String superUserPermString = IndexServiceBean.getGroupPerUserPrefix() + superuser.getId();
            superUserPermStrings.add(superUserPermString);
        }
        return superUserPermStrings;
    }

}
