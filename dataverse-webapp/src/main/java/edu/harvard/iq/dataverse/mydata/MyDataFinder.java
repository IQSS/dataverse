package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.api.DataRetriever;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.search.SearchFields;
import org.apache.commons.lang.StringUtils;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Given a user and a set of filters (dvobject type, roles, publication status):
 * - Use postgres to identify DvObject types
 * - Format a solr query string
 *
 * @author rmp553
 */
public class MyDataFinder {

    MyDataFilterParams filterParams;

    private DataverseRolePermissionHelper rolePermissionHelper;
    private RoleAssigneeServiceBean roleAssigneeService;
    private DvObjectServiceBean dvObjectServiceBean;

    public boolean errorFound = false;
    public String errorMessage = null;

    public Map<Long, Boolean> harvestedDataverseIds = new HashMap<>();

    // Populated in initial query.  DvObject ids -- regardless of Dtype,
    // are sorted into respective buckets in regard to permissions.
    // The same id may appear in multiple lists--and more than once
    // ----------------------------
    // POPULATED IN STEP 1 (1st query)
    // ----------------------------
    public Map<Long, Long> childToParentIds = new HashMap<>();
    public Map<Long, Boolean> idsWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithFilePermissions = new HashMap<>();  // { role id : true }

    private List<Long> directDvObjectIds = new ArrayList<>();

    // Lists later used to format Solr Queries
    // ----------------------------
    // POPULATED IN STEP 2 (2nd query)
    // ----------------------------
    private List<Long> directDataverseIds = new ArrayList<>();
    private List<Long> directDatasetIds = new ArrayList<>();
    private List<Long> directFileIds = new ArrayList<>();

    private List<Long> datasetParentIds = new ArrayList<>(); // dataverse has dataset permissions

    private List<Long> fileParentIds = new ArrayList<>();   // dataset has file permissions
    private List<Long> fileGrandparentFileIds = new ArrayList<>();  // dataverse has file permissions

    // -------------------- CONSTRUCTORS --------------------

    public MyDataFinder(DataverseRolePermissionHelper rolePermissionHelper, RoleAssigneeServiceBean roleAssigneeService, DvObjectServiceBean dvObjectServiceBean) {
        this.rolePermissionHelper = rolePermissionHelper;
        this.roleAssigneeService = roleAssigneeService;
        this.dvObjectServiceBean = dvObjectServiceBean;
        loadHarvestedDataverseIds();
    }

    // -------------------- GETTERS --------------------

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return errorFound;
    }

    public boolean isHarvestedDataExcluded() {
        return true;
    }

    // -------------------- LOGIC --------------------

    public void runFindDataSteps(MyDataFilterParams filterParams) {
        this.filterParams = filterParams;

        if (this.filterParams.hasError()) {
            addErrorMessage(filterParams.getErrorMessage());
            return;
        }

        if (!runStep1RoleAssignments() || !runStep2DirectAssignments()) {
            return;
        }
        if (!fileGrandparentFileIds.isEmpty()) {
            runStep3FilePermsAssignedAtDataverse();
        }
    }

    /**
     * Get the final queries for the Solr Search object
     */
    public List<String> getSolrFilterQueries() {
        if (hasError()) {
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyDataFinder object, first check 'hasError()'");
        }

        List<String> filterQueries = new ArrayList<>();

        // -----------------------------------------------------------------
        // (1) Add entityId/parentId FQ
        //  - by entityId (dvObject id) and parentId (dvObject ownerId)
        // -----------------------------------------------------------------
        String dvObjectFQ = getSolrDvObjectFilterQuery();
        if (dvObjectFQ == null) {
            addErrorMessage(DataRetriever.MSG_NO_RESULTS_FOUND);
            return null;
        }
        filterQueries.add(dvObjectFQ);

        // -----------------------------------------------------------------
        // (2) FQ by Publication Status
        // -----------------------------------------------------------------
        filterQueries.add(filterParams.getSolrFragmentForPublicationStatus());
        return filterQueries;
    }

    public String getSolrDvObjectFilterQuery() {

        if (hasError()) {
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyDataFinder object,first check 'hasError()'");
        }

        // Build lists of Ids
        List<Long> entityIds = new ArrayList<>();
        List<Long> parentIds = new ArrayList<>();

        if (filterParams.areDataversesIncluded()) {
            entityIds.addAll(directDataverseIds); // dv ids
        }
        if (filterParams.areDatasetsIncluded()) {
            entityIds.addAll(directDatasetIds);  // dataset ids
            parentIds.addAll(datasetParentIds);  // dv ids that are dataset parents
        }
        if (filterParams.areFilesIncluded()) {
            entityIds.addAll(directFileIds); // file ids
            parentIds.addAll(fileParentIds); // dataset ids that are file parents
        }

        // Remove duplicates by Creating a Set
        Set<Long> distinctEntityIds = new HashSet<>(entityIds);
        Set<Long> distinctParentIds = new HashSet<>(parentIds);

        if ((distinctEntityIds.isEmpty()) && (distinctParentIds.isEmpty())) {
            addErrorMessage(DataRetriever.MSG_NO_RESULTS_FOUND);
            return null;
        }

        // See if we can trim down the list of distinctEntityIds
        // If we have the parent of a distinctEntityId in distinctParentIds, then we query it via the parent
        List<Long> finalDirectEntityIds = new ArrayList<>();
        for (Long idToCheck : distinctEntityIds) {
            // Do we have the parent in our map? we are not checking the parent of dataverses, so add this explicitly
            // Similar to SEK 7/015 - all direct dataverse ids are used because child dataverses with direct assignments are being lost.
            if (childToParentIds.containsKey(idToCheck)
                && (directDataverseIds.contains(idToCheck) || !distinctParentIds.contains(childToParentIds.get(idToCheck)))) {
                // Add all dataverse ids explicitly
                // OR: Is the parent also in our list of Ids to query? No, then let's check this id directly
                finalDirectEntityIds.add(idToCheck);
            }
        }

        // Set the distinctEntityIds to the finalDirectEntityIds
        distinctEntityIds = new HashSet<>(finalDirectEntityIds);

        // Start up a SolrQueryFormatter for building clauses
        SolrQueryFormatter sqf = new SolrQueryFormatter();

        // Build clauses
        String entityIdClause = distinctEntityIds.isEmpty()
                ? null : sqf.buildIdQuery(distinctEntityIds, SearchFields.ENTITY_ID, null);
        String parentIdClause = distinctParentIds.isEmpty()
                ? null : sqf.buildIdQuery(distinctParentIds, SearchFields.PARENT_ID, "datasets OR files");

        if (entityIdClause != null && parentIdClause != null) {
            return String.format("(%s OR %s)", entityIdClause, parentIdClause);
        } else {
            return entityIdClause != null ? entityIdClause : parentIdClause;
        }
    }

    public JsonObjectBuilder getSelectedFilterParamsAsJSON() {
        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        jsonData.add("publication_statuses", filterParams.getListofSelectedPublicationStatuses())
                .add("role_names", getListofSelectedRoles());
        return jsonData;
    }

    public JsonArrayBuilder getListofSelectedRoles() {
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        filterParams.getRoleIds().stream()
                .map(rolePermissionHelper::getRoleName)
                .forEach(jsonArray::add);
        return jsonArray;
    }

    // -------------------- PRIVATE --------------------

    private void loadHarvestedDataverseIds() {
        dvObjectServiceBean.getAllHarvestedDataverseIds()
                .forEach(id -> harvestedDataverseIds.put(id, true));
    }

    private boolean isHarvesteDataverseId(Long id) {
        return id != null && harvestedDataverseIds.containsKey(id);
    }

    private boolean runStep1RoleAssignments() {
        List<Object[]> results = roleAssigneeService.getAssigneeAndRoleIdListFor(filterParams);

        if (results == null) {
            addErrorMessage("Sorry, the EntityManager isn't working (still).");
            return false;
        } else if (results.isEmpty()) {
            List<String> roleNames = rolePermissionHelper.getRoleNamesByIdList(filterParams.getRoleIds());
            if (roleNames == null || roleNames.isEmpty()) {
                addErrorMessage("Sorry, you have no assigned roles.");
            } else {
                addErrorMessage(roleNames.size() == 1
                        ? "Sorry, nothing was found for this role: " + StringUtils.join(roleNames, ", ")
                        : "Sorry, nothing was found for these roles: " + StringUtils.join(roleNames, ", "));
            }
            return false;
        }

        // Iterate through assigned objects, a single object may end up in
        // multiple "buckets"
        for (Object[] ra : results) {
            Long dvId = (Long) ra[0];
            Long roleId = (Long) ra[1];

            // Is this is a harvested Dataverse? If so, skip it.
            if (isHarvestedDataExcluded() && isHarvesteDataverseId(dvId)) {
                continue;
            }

            // Put dvId in 1 or more buckets, depending pn if role
            // applies to a Dataverse, Dataset, and/or File
            if (rolePermissionHelper.hasDataversePermissions(roleId)) {
                idsWithDataversePermissions.put(dvId, true);
            }
            if (rolePermissionHelper.hasDatasetPermissions(roleId)) {
                idsWithDatasetPermissions.put(dvId, true);
            }
            if (rolePermissionHelper.hasFilePermissions(roleId)) {
                idsWithFilePermissions.put(dvId, true);
            }
            directDvObjectIds.add(dvId);
        }
        return true;
    }

    private boolean runStep2DirectAssignments() {

        if (hasError()) {
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyData object,first check 'hasError()'");
        }

        List<Object[]> results = dvObjectServiceBean.getDvObjectInfoForMyData(directDvObjectIds);
        if (results.isEmpty()) {
            addErrorMessage("Sorry, you have no assigned Dataverses, Datasets, or Files.");
            return false;
        }

        // Iterate through assigned objects
        for (Object[] ra : results) {
            Long dvId = new Long((Integer) ra[0]);
            String dtype = (String) ra[1];
            Long parentId = (Long) ra[2];

            // If this object is harvested, then skip it...
            if (isHarvestedDataExcluded() && (isHarvesteDataverseId(dvId) || isHarvesteDataverseId(parentId))) {
                continue;
            }

            childToParentIds.put(dvId, parentId);

            switch (dtype) {
                case (DvObject.DATAVERSE_DTYPE_STRING):
                    directDataverseIds.add(dvId);  // Direct dataverse (no indirect dataverses)
                    if (idsWithDatasetPermissions.containsKey(dvId)) {
                        datasetParentIds.add(dvId);    // Parent to dataset
                    }
                    if (idsWithFilePermissions.containsKey(dvId)) {
                        fileGrandparentFileIds.add(dvId); // Grandparent to file
                        // Also show the Dataset--even though the permissions don't apply directly
                        //  e.g. The Permissions flows:
                        //      from the DV -> through the DS -> to the file
                        datasetParentIds.add(dvId);    // Parent to dataset
                    }
                    break;
                case (DvObject.DATASET_DTYPE_STRING):
                    directDatasetIds.add(dvId); // Direct dataset
                    if (idsWithFilePermissions.containsKey(dvId)) {
                        fileParentIds.add(dvId);   // Parent to file
                    }
                    break;
                case (DvObject.DATAFILE_DTYPE_STRING):
                    if (idsWithFilePermissions.containsKey(dvId)) {
                        directFileIds.add(dvId); // Direct file
                    }
                    break;
            }
        }

        // Direct ids no longer needed
        directDvObjectIds = null;

        return true;
    }

    private void runStep3FilePermsAssignedAtDataverse() {
        if (fileGrandparentFileIds == null || fileGrandparentFileIds.isEmpty()) {
            return;
        }

        List<Object[]> results = dvObjectServiceBean.getDvObjectInfoByParentIdForMyData(fileGrandparentFileIds);
        //  SEK 07/09 Ticket 2329
        //  Removed failure for empty results - if there are none let it go
        if (results.isEmpty()) {
            return;        // RMP, shouldn't throw an error if no results
        }

        // Iterate through object list
        for (Object[] ra : results) {
            Long dvId = new Long((Integer) ra[0]);
            String dtype = (String) ra[1];
            Long parentId = (Long) ra[2];

            childToParentIds.put(dvId, parentId);

            // Should ALWAYS be a Dataset!
            if (dtype.equals(DvObject.DATASET_DTYPE_STRING)) {
                fileParentIds.add(dvId);
            }
        }
    }

    private void addErrorMessage(String s) {
        errorFound = true;
        errorMessage = s;
    }
}
