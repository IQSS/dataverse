package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
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
import java.util.Arrays;
import java.util.List;
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

    /**
     * Get a list of "perm strings" suitable for indexing as an array into Solr.
     *
     * @param dvObjectId The database id of the DvObject
     * @return A list of strings on which Solr will JOIN to enforce permissions
     */
    private List<String> findPerms(Long dvObjectId) {
        List<String> permStrings = new ArrayList<>();

        if (isPublic(dvObjectId)) {
            permStrings.add(IndexServiceBean.getPublicGroupString());
        }

        List<RoleAssignee> roleAssignees = findWhoCanSearch(dvObjectId);
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

    private boolean isPublic(Long dvObjectId) {
        /**
         * @todo implement this
         */
        return true;
    }

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

    private DvObjectSolrDoc constructDataverseSolrDoc(Dataverse dataverse) {
        List<String> perms = findPerms(dataverse.getId());
        DvObjectSolrDoc dvDoc = new DvObjectSolrDoc(dataverse.getAlias(), perms);
        return dvDoc;
    }

    private List<DvObjectSolrDoc> constructDatasetSolrDocs(Dataset dataset) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        /**
         * @todo return not just latest version but draft versions,
         * deaccessioned versions, etc.
         */
        DvObjectSolrDoc latestVersion = new DvObjectSolrDoc(dataset.getLatestVersion().getTitle(), new ArrayList<String>());
        solrDocs.add(latestVersion);
        return solrDocs;
    }

}
