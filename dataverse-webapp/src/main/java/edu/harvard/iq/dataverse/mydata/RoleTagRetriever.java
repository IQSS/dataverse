package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Input:  dvObject id, parent Id, and dvObject type (from Solr)
 * Output: For each dvObject id, a list of Role names
 *
 * @author rmp553
 */
public class RoleTagRetriever {

    private static final Logger logger = Logger.getLogger(RoleTagRetriever.class.getCanonicalName());

    private final DataverseRolePermissionHelper rolePermissionHelper;
    private final RoleAssigneeServiceBean roleAssigneeService;
    private final DvObjectServiceBean dvObjectServiceBean;

    public boolean errorFound = false;
    public String errorMessage = null;

    private Map<Long, List<Long>> idToRoleListHash;    // { dvobject id : [role id, role id] }
    private Map<Long, Long> childToParentIdHash;     // { dvobject id : parent id }
    private Map<Long, SearchObjectType> idToDvObjectType;     // { dvobject id : dvobject type }
    private List<Long> datasetIdsNeedingParentIds;
    private List<Long> finalCardIds;
    private Map<Long, List<String>> finalIdToRolesHash;  // { dvobject id : [role name, role name] }

    // -------------------- CONSTRUCTORS --------------------

    public RoleTagRetriever(DataverseRolePermissionHelper rolePermissionHelper,
                            RoleAssigneeServiceBean roleAssigneeService, DvObjectServiceBean dvObjectServiceBean) {
        this.rolePermissionHelper = rolePermissionHelper;
        this.roleAssigneeService = roleAssigneeService;
        this.dvObjectServiceBean = dvObjectServiceBean;
    }

    // -------------------- GETTERS --------------------

    public String getErrorMessage() {
        return errorMessage;
    }

    // -------------------- LOGIC --------------------

    public void loadRoles(DataverseRequest dataverseRequest, SolrQueryResponse solrQueryResponse) {
        if (dataverseRequest == null) {
            throw new NullPointerException("RoleTagRetriever.constructor. dataverseRequest cannot be null");
        }

        AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();

        if (au == null) {
            throw new NullPointerException("RoleTagRetriever.constructor. au cannot be null");
        }

        String userIdentifier = au.getUserIdentifier();
        if (userIdentifier == null) {
            throw new NullPointerException("RoleTagRetriever.constructor. userIdentifier cannot be null");
        }

        if (solrQueryResponse == null) {
            throw new NullPointerException("RoleTagRetriever.constructor. solrQueryResponse cannot be null");
        }

        // (1) Reset variables
        initLookups();

        // (2) Load roles from solr docs
        loadInfoFromSolrResponseDocs(solrQueryResponse);

        // (3) Load grandparent ids, if needed
        findDataverseIdsForFiles();

        // (4) Retrieve the role ids
        retrieveRoleIdsForDvObjects(dataverseRequest, au);

        // (5) Prepare final role lists
        prepareFinalRoleLists();
    }

    public Map<Long, List<String>> getFinalIdToRolesHash() {
        return finalIdToRolesHash;
    }

    public boolean hasRolesForCard(Long dvObjectId) {
        return dvObjectId != null && finalIdToRolesHash.containsKey(dvObjectId);
    }

    public List<String> getRolesForCard(Long dvObjectId) {
        return hasRolesForCard(dvObjectId) ? finalIdToRolesHash.get(dvObjectId) : null;
    }

    public Map<Long, List<String>> getRolesForCard(List<Long> dvObjectIds) {
        return dvObjectIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> finalIdToRolesHash.getOrDefault(id, Collections.emptyList()),
                    (prev, next) -> next));
    }

    /**
     * For the cards, make a dict of { dv object id : [role name, role name, etc ]}
     */
    public void prepareFinalRoleLists() {
        if (finalCardIds.isEmpty()) {
            return;
        }

        List<String> formattedRoleNames;
        List<String> finalRoleNames;

        for (Long dvIdForCard : finalCardIds) {
            // (a) Make a new array with the role names for the card
            finalRoleNames = new ArrayList<>();
            if (!idToDvObjectType.containsKey(dvIdForCard)) {
                throw new IllegalStateException("All dvObject ids from solr should have their dvObject types in this hash");
            }

            // (b) Add direct role assignments -- may be empty
            formattedRoleNames = getFormattedRoleListForId(dvIdForCard);
            if (formattedRoleNames != null) {
                finalRoleNames.addAll(formattedRoleNames);
            }

            // (c) get parent id
            Long parentId;
            if (childToParentIdHash.containsKey(dvIdForCard)) {
                parentId = childToParentIdHash.get(dvIdForCard);
            } else {
                // No parent!  Store roles and move to next id
                finalIdToRolesHash.put(dvIdForCard, formatRoleNames(finalRoleNames));
                continue;
            }

            // (d) get dtype
            SearchObjectType dtype = idToDvObjectType.get(dvIdForCard);

            switch (dtype) {
                case DATASETS:
                    // (d1) May have indirect assignments re: dataverse
                    formattedRoleNames = getFormattedRoleListForId(parentId, true);
                    if (formattedRoleNames != null) {
                        finalRoleNames.addAll(formattedRoleNames);
                    }
                    break;
                case FILES:
                    // (d2) May have indirect assignments re: dataset
                    formattedRoleNames = getFormattedRoleListForId(parentId, false);
                    if (formattedRoleNames != null) {
                        finalRoleNames.addAll(formattedRoleNames);
                    }
                    // May have indirect assignments re: dataverse
                    if (childToParentIdHash.containsKey(parentId)) {
                        Long grandparentId = childToParentIdHash.get(parentId);
                        formattedRoleNames = getFormattedRoleListForId(grandparentId, false);
                        if (formattedRoleNames != null) {
                            finalRoleNames.addAll(formattedRoleNames);
                        }
                    }
                    break;
                default:
                    // No indirect assignments for dataverse
                    break;
            }
            finalIdToRolesHash.put(dvIdForCard, formatRoleNames(finalRoleNames));
        }
    }

    // -------------------- PRIVATE --------------------

    private void initLookups() {
        errorFound = false;
        errorMessage = null;
        idToRoleListHash = new HashMap<>();    // { dvobject id : [role id, role id] }
        childToParentIdHash = new HashMap<>();     // { dvobject id : parent id }
        idToDvObjectType = new HashMap<>();     // { dvobject id : dvobject type }
        finalIdToRolesHash = new HashMap<>();
        datasetIdsNeedingParentIds = new ArrayList<>();
        finalCardIds = new ArrayList<>();
    }

    private void addIdNeedingRoleRetrieval(Long dvObjectId) {
        if (dvObjectId == null) {
            return;
        }
        idToRoleListHash.putIfAbsent(dvObjectId, new ArrayList<>());
    }

    private void addRoleIdForHash(Long dvObjectId, Long roleId) {
        if (dvObjectId == null || roleId == null) {
            return;
        }
        if (!idToRoleListHash.containsKey(dvObjectId)) {
            logger.warning("DvObject id not found in hash (shouldn't happen): " + dvObjectId);
            return;
        }
        List<Long> roldIdList = idToRoleListHash.get(dvObjectId);
        roldIdList.add(roleId);
        idToRoleListHash.put(dvObjectId, roldIdList);
    }

    /**
     * Iterate through the Solr Cards and collect
     * - DvObject Id + Parent ID
     * - Dtype for object and parent
     * - Whether a "grandparent id" is needed for a file object
     */
    private void loadInfoFromSolrResponseDocs(SolrQueryResponse solrQueryResponse) {

        if (solrQueryResponse == null) {
            throw new NullPointerException("RoleTagRetriever.constructor. solrQueryResponse cannot be null");
        }

        // Load initial data
        // Iterate through Solr cards
        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()) {

            // (a) retrieve Card Id and DvObject type
            finalCardIds.add(doc.getEntityId());

            SearchObjectType dtype = doc.getType();
            Long entityId = doc.getEntityId();

            if (dtype == null) {
                throw new NullPointerException("The dvobject type cannot be null for SolrSearchResult");
            }
            logger.finest("\nid: " + doc.getEntityId() + " dtype: " + dtype);

            // (b) Populate dict of { dvObject id : dtype }
            idToDvObjectType.put(entityId, dtype);

            // (c) initialize dict of { dvObject id : [ (empty list for role ids) ] }
            addIdNeedingRoleRetrieval(entityId);

            Long parentId = doc.getParentIdAsLong();

            // For datasets and files, check parents
            if (dtype != SearchObjectType.DATAVERSES) {

                // (d) Add to the childToParentIdHash
                if (parentId == null) {
                    throw new NullPointerException("A dataset or file parent cannot be null for SolrSearchResult");
                }

                logger.finest("\nparentId: " + parentId);
                childToParentIdHash.put(doc.getEntityId(), parentId);

                // (e) For the parent, add to dict
                addIdNeedingRoleRetrieval(parentId);

                // (f) Add the parent to the DvObject type lookup
                if (doc.getType() == SearchObjectType.FILES) {
                    // (f1) This is a file, we know the parent is a Dataset
                    idToDvObjectType.put(parentId, SearchObjectType.DATASETS);

                    // (g) For files, we'll need to get roles from the grandparent--e.g., the dataverse
                    datasetIdsNeedingParentIds.add(parentId);
                }
                if (dtype == SearchObjectType.DATASETS) {
                    // (f2) This is a Dataset, we know the parent is a Dataverse
                    idToDvObjectType.put(parentId, SearchObjectType.DATAVERSES);
                }
            }

            // initialize final hash of dvObject id and empty list of role names
            finalIdToRolesHash.put(doc.getEntityId(), new ArrayList<>());
        }
    }

    /**
     * From the Cards, we know the Parent Ids of all the DvObjects
     * However, for files, the roles may trickle down from the Dataverses
     * Dataverse (file downloader) -> Dataset (file downloader) -> File (file downloader)
     * Grandparent -> Parent -> Child
     * Therefore, we need the File's "grandparent id" -- the Dataverse ID
     * File (from card) -> Parent (from card) -> Grandparent (NEED TO FIND)
     */
    private void findDataverseIdsForFiles() {
        // (1) Do we have any dataset Ids where we need to find the parent dataverse?
        if (datasetIdsNeedingParentIds == null) {
            throw new NullPointerException("findDataverseIdsForFiles should not be null");
        }

        if (datasetIdsNeedingParentIds.isEmpty()) {
            logger.fine("No ids found!");
            return;
        }

        // (2) Do we have any dataset Ids where we need to find the parent dataverse?
        List<Object[]> results = dvObjectServiceBean.getDvObjectInfoForMyData(datasetIdsNeedingParentIds);
        logger.finest("findDataverseIdsForFiles results count: " + results.size());

        // (2a) Nope, return
        if (results.isEmpty()) {
            return;
        }

        // (3) Process the results -- the parent ID is the Dataverse that we're interested in
        for (Object[] ra : results) {
            Long dvId = new Long((Integer) ra[0]);
            String dtype = (String) ra[1];
            Long parentId = (Long) ra[2];

            // Should ALWAYS be a Dataset!
            if (dtype.equals(DvObject.DATASET_DTYPE_STRING)) {
                childToParentIdHash.put(dvId, parentId); // Store the parent child relation
                addIdNeedingRoleRetrieval(parentId); // We need the roles for this dataverse
                idToDvObjectType.put(parentId, SearchObjectType.DATAVERSES); // store the dv object type
            }
        }
    }

    private void retrieveRoleIdsForDvObjects(DataverseRequest dataverseRequest, AuthenticatedUser au) {

        String userIdentifier = au.getUserIdentifier();
        if (userIdentifier == null) {
            throw new NullPointerException("RoleTagRetriever.constructor. userIdentifier cannot be null");
        }

        if (idToRoleListHash.isEmpty()) {
            return;
        }

        List<Long> dvObjectIdList = new ArrayList<>(idToRoleListHash.keySet());
        List<Object[]> results = roleAssigneeService.getRoleIdsFor(dataverseRequest, dvObjectIdList);

        if (results == null) {
            addErrorMessage("Sorry, the roleAssigneeService isn't working.");
            return;
        } else if (results.isEmpty()) {
            logger.log(Level.WARNING, "No roles were found for user {0} with ids {1}",
                    new Object[]{userIdentifier, dvObjectIdList.toString()});
            addErrorMessage("Sorry, no roles were found.");
            return;
        }

        // Iterate through assigned objects, a single object may end up in multiple "buckets"
        for (Object[] ra : results) {
            Long dvId = (Long) ra[0];
            Long roleId = (Long) ra[1];
            addRoleIdForHash(dvId, roleId);
        }
    }

    private List<String> getFormattedRoleListForId(Long dvId) {
        if (dvId == null || !idToRoleListHash.containsKey(dvId)) {
            return null;
        }

        return idToRoleListHash.get(dvId).stream()
                .map(rolePermissionHelper::getRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> getFormattedRoleListForId(Long dvId, boolean withDatasetPerms) {
        if (dvId == null || !idToRoleListHash.containsKey(dvId)) {
            return null;
        }

        return idToRoleListHash.get(dvId).stream()
                .filter(roleId -> (withDatasetPerms && rolePermissionHelper.hasDatasetPermissions(roleId))
                                || rolePermissionHelper.hasFilePermissions(roleId))
                .map(rolePermissionHelper::getRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> formatRoleNames(List<String> roleNames) {
        if (roleNames == null) {
            return null;
        }

        Set<String> distinctRoleNames = new HashSet<>(roleNames); // remove duplicates
        roleNames = new ArrayList<>(distinctRoleNames);
        Collections.sort(roleNames);
        return roleNames;
    }

    private void addErrorMessage(String s) {
        errorFound = true;
        errorMessage = s;
    }
}
