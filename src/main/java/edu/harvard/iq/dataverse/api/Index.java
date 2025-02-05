package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.search.DvObjectSolrDoc;
import edu.harvard.iq.dataverse.search.FacetCategory;
import edu.harvard.iq.dataverse.search.FileView;
import edu.harvard.iq.dataverse.search.IndexBatchServiceBean;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.IndexUtil;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchFilesServiceBean;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.solr.client.solrj.SolrServerException;

@Path("admin/index")
public class Index extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Index.class.getCanonicalName());

    @EJB
    IndexServiceBean indexService;
    @EJB
    IndexBatchServiceBean indexBatchService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SolrIndexServiceBean SolrIndexService;
    @EJB
    SearchServiceBean searchService;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    SearchFilesServiceBean searchFilesService;

    public static String contentChanged = "contentChanged";
    public static String contentIndexed = "contentIndexed";
    public static String permsChanged = "permsChanged";
    public static String permsIndexed = "permsIndexed";

    @GET
    public Response indexAllOrSubset(@QueryParam("numPartitions") Long numPartitionsSelected, @QueryParam("partitionIdToProcess") Long partitionIdToProcess, @QueryParam("previewOnly") boolean previewOnly) {
        return indexAllOrSubset(numPartitionsSelected, partitionIdToProcess, false, previewOnly);
    }

    @GET
    @Path("continue")
    public Response indexAllOrSubsetContinue(@QueryParam("numPartitions") Long numPartitionsSelected, @QueryParam("partitionIdToProcess") Long partitionIdToProcess, @QueryParam("previewOnly") boolean previewOnly) {
        return indexAllOrSubset(numPartitionsSelected, partitionIdToProcess, true, previewOnly);
    }

    private Response indexAllOrSubset(Long numPartitionsSelected, Long partitionIdToProcess, boolean skipIndexed, boolean previewOnly) {
        try {
            long numPartitions = 1;
            if (numPartitionsSelected != null) {
                if (numPartitionsSelected < 1) {
                    return error(Status.BAD_REQUEST, "numPartitions must be 1 or higher but was " + numPartitionsSelected);
                } else {
                    numPartitions = numPartitionsSelected;
                }
            }
            List<Long> availablePartitionIds = new ArrayList<>();
            for (long i = 0; i < numPartitions; i++) {
                availablePartitionIds.add(i);
            }

            Response invalidParitionIdSelection = error(Status.BAD_REQUEST, "You specified " + numPartitions + " partition(s) and your selected partitionId was " + partitionIdToProcess + " but you must select from these availableParitionIds: " + availablePartitionIds);
            if (partitionIdToProcess != null) {
                long selected = partitionIdToProcess;
                if (!availablePartitionIds.contains(selected)) {
                    return invalidParitionIdSelection;
                }
            } else if (numPartitionsSelected == null) {
                /**
                 * The user has not specified a partitionId and hasn't specified
                 * the number of partitions. Run "index all", the whole thing.
                 */
                partitionIdToProcess = 0l;
            } else {
                return invalidParitionIdSelection;

            }

            JsonObjectBuilder args = Json.createObjectBuilder();
            args.add("numPartitions", numPartitions);
            args.add("partitionIdToProcess", partitionIdToProcess);
            JsonArrayBuilder availablePartitionIdsBuilder = Json.createArrayBuilder();
            for (long i : availablePartitionIds) {
                availablePartitionIdsBuilder.add(i);
            }

            JsonObjectBuilder preview = indexBatchService.indexAllOrSubsetPreview(numPartitions, partitionIdToProcess, skipIndexed);
            if (previewOnly) {
                preview.add("args", args);
                preview.add("availablePartitionIds", availablePartitionIdsBuilder);
                return ok(preview);
            }

            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("availablePartitionIds", availablePartitionIdsBuilder);
            response.add("args", args);
            /**
             * @todo How can we expose the String returned from "index all" via
             * the API?
             */
            Future<JsonObjectBuilder> indexAllFuture = indexBatchService.indexAllOrSubset(numPartitions, partitionIdToProcess, skipIndexed, previewOnly);
            JsonObject workloadPreview = preview.build().getJsonObject("previewOfPartitionWorkload");
            int dataverseCount = workloadPreview.getInt("dataverseCount");
            int datasetCount = workloadPreview.getInt("datasetCount");
            String status = "indexAllOrSubset has begun of " + dataverseCount + " dataverses and " + datasetCount + " datasets.";
            response.add("message", status);
            return ok(response);
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage()).append(" ");
                if (cause instanceof ConstraintViolationException) {
                    sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                } else if (cause instanceof NullPointerException) {
                    for (int i = 0; i < 2; i++) {
                        StackTraceElement stacktrace = cause.getStackTrace()[i];
                        if (stacktrace != null) {
                            String classCanonicalName = stacktrace.getClass().getCanonicalName();
                            String methodName = stacktrace.getMethodName();
                            int lineNumber = stacktrace.getLineNumber();
                            String error = "at " + stacktrace.getClassName() + "." + stacktrace.getMethodName() + "(" + stacktrace.getFileName() + ":" + lineNumber + ") ";
                            sb.append(error);
                        }
                    }
                }
            }
            if (sb.toString().contains("java.lang.IllegalStateException ")) {
                return ok("indexing went as well as can be expected... got java.lang.IllegalStateException but some indexing may have happened anyway");
            } else {
                return error(Status.INTERNAL_SERVER_ERROR, sb.toString());
            }
        }
    }

    @GET
    @Path("clear")
    public Response clearSolrIndex() {
        try {
            JsonObjectBuilder response = SolrIndexService.deleteAllFromSolrAndResetIndexTimes();
            return ok(response);
        } catch (SolrServerException | IOException ex) {
            return error(Status.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("{type}/{id}")
    public Response indexTypeById(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            if (type.equals("dataverses")) {
                Dataverse dataverse = dataverseService.find(id);
                if (dataverse != null) {
                    /**
                     * @todo Can we display the result of indexing to the user?
                     */
                    
                    try {
                        Future<String> indexDataverseFuture = indexService.indexDataverse(dataverse);
                    } catch (IOException | SolrServerException e) {                                                
                        return error(Status.BAD_REQUEST, writeFailureToLog(e.getLocalizedMessage(), dataverse));
                    }
                    return ok("starting reindex of dataverse " + id);
                } else {
                    String response = indexService.removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataverse + id);
                    return notFound("Could not find dataverse with id of " + id + ". Result from deletion attempt: " + response);
                }
            } else if (type.equals("datasets")) {
                Dataset dataset = datasetService.find(id);
                if (dataset != null) {
                    boolean doNormalSolrDocCleanUp = true;
                    indexService.asyncIndexDataset(dataset, doNormalSolrDocCleanUp);

                    return ok("starting reindex of dataset " + id);
                } else {
                    /**
                     * @todo what about published, deaccessioned, etc.? Need
                     * method to target those, not just drafts!
                     */
                    String response = indexService.removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataset + id + IndexServiceBean.draftSuffix);
                    return notFound("Could not find dataset with id of " + id + ". Result from deletion attempt: " + response);
                }
            } else if (type.equals("files")) {
                DataFile dataFile = dataFileService.find(id);
                Dataset datasetThatOwnsTheFile = datasetService.find(dataFile.getOwner().getId());
                /**
                 * @todo How can we display the result to the user?
                 */
                boolean doNormalSolrDocCleanUp = true;
                indexService.asyncIndexDataset(datasetThatOwnsTheFile, doNormalSolrDocCleanUp);
                
                return ok("started reindexing " + type + "/" + id);
            } else {
                return error(Status.BAD_REQUEST, "illegal type: " + type);
            }
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append("Problem indexing ").append(type).append("/").append(id).append(": ");
            sb.append(ex).append(" ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName()).append(" ");
                sb.append(cause.getMessage()).append(" ");
                if (cause instanceof ConstraintViolationException) {
                    sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                } else if (cause instanceof NullPointerException) {
                    for (int i = 0; i < 2; i++) {
                        StackTraceElement stacktrace = cause.getStackTrace()[i];
                        if (stacktrace != null) {
                            String classCanonicalName = stacktrace.getClass().getCanonicalName();
                            String methodName = stacktrace.getMethodName();
                            int lineNumber = stacktrace.getLineNumber();
                            String error = "at " + stacktrace.getClassName() + "." + stacktrace.getMethodName() + "(" + stacktrace.getFileName() + ":" + lineNumber + ") ";
                            sb.append(error);
                        }
                    }
                }
            }
            return error(Status.INTERNAL_SERVER_ERROR, sb.toString());
        }
    }

    @GET
    @Path("dataset")
    public Response indexDatasetByPersistentId(@QueryParam("persistentId") String persistentId) {
        if (persistentId == null) {
            return error(Status.BAD_REQUEST, "No persistent id given.");
        }
        Dataset dataset = null;
        try {
            dataset = datasetService.findByGlobalId(persistentId);
        } catch (Exception ex) {
            return error(Status.BAD_REQUEST, "Problem looking up dataset with persistent id \"" + persistentId + "\". Error: " + ex.getMessage());
        }
        if (dataset != null) {
            boolean doNormalSolrDocCleanUp = true;
            indexService.asyncIndexDataset(dataset, doNormalSolrDocCleanUp);
            JsonObjectBuilder data = Json.createObjectBuilder();
            data.add("message", "Reindexed dataset " + persistentId);
            data.add("id", dataset.getId());
            data.add("persistentId", dataset.getGlobalId().asString());
            JsonArrayBuilder versions = Json.createArrayBuilder();
            for (DatasetVersion version : dataset.getVersions()) {
                JsonObjectBuilder versionObject = Json.createObjectBuilder();
                versionObject.add("semanticVersion", version.getSemanticVersion());
                versionObject.add("id", version.getId());
                versions.add(versionObject);
            }
            data.add("versions", versions);
            return ok(data);
        } else {
            return error(Status.BAD_REQUEST, "Could not find dataset with persistent id " + persistentId);
        }
    }

    /**
     * Clears the entry for a dataset from Solr
     * 
     * @param id numer id of the dataset
     * @return response; 
     * will return 404 if no such dataset in the database; but will attempt to 
     * clear the entry from Solr regardless.
     */
    @DELETE
    @Path("datasets/{id}")
    public Response clearDatasetFromIndex(@PathParam("id") Long id) {
        Dataset dataset = datasetService.find(id);
        // We'll attempt to delete the Solr document regardless of whether the 
        // dataset exists in the database: 
        String response = indexService.removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataset + id);
        if (dataset != null) {
            return ok("Sent request to clear Solr document for dataset " + id + ": " + response);
        } else {
            return notFound("Could not find dataset " + id + " in the database. Requested to clear from Solr anyway: " + response);
        }
    }


    /**
     * This is just a demo of the modular math logic we use for indexAll.
     */
    @GET
    @Path("mod")
    public Response indexMod(@QueryParam("partitions") long partitions, @QueryParam("which") long which) {
        long numObjectToConsider = 100;
        List<Long> dvObjectsIds = new ArrayList<>();
        for (long i = 1; i <= numObjectToConsider; i++) {
            dvObjectsIds.add(i);
        }
        List<Long> mine = IndexUtil.findDvObjectIdsToProcessMod(dvObjectsIds, partitions, which);
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("partitions", partitions);
        response.add("which", which);
        response.add("mine", mine.toString());
        return ok(response);
    }

    @GET
    @Path("perms")
    public Response indexAllPermissions() {
        IndexResponse indexResponse = solrIndexService.indexAllPermissions();
        return ok(indexResponse.getMessage());
    }

    @GET
    @Path("perms/{id}")
    public Response indexPermissions(@PathParam("id") Long id) {
        DvObject dvObject = dvObjectService.findDvObject(id);
        if (dvObject == null) {
            return error(Status.BAD_REQUEST, "Could not find DvObject based on id " + id);
        } else {
            IndexResponse indexResponse = solrIndexService.indexPermissionsForOneDvObject(dvObject);
            return ok(indexResponse.getMessage());
        }
    }
    /**
     * Checks whether there are inconsistencies between the Solr index and 
     * the database, and reports back the status by content type
     * @param sync - optional parameter, if set, then run the command 
     * synchronously. Else, return immediately, and report the status in server.log
     * @return status report
     */
    @GET
    @Path("status")
    public Response indexStatus(@QueryParam("sync") String sync) {
        Future<JsonObjectBuilder> result = indexBatchService.indexStatus();
        if (sync != null) {
            try {
                JsonObjectBuilder status = result.get();
                return ok(status);
            } catch (InterruptedException | ExecutionException e) {
                return AbstractApiBean.error(Status.INTERNAL_SERVER_ERROR, "indexStatus method interrupted: " + e.getLocalizedMessage());
            }
        } else {
            return ok("Index Status Batch Job initiated, check log for job status.");
        }
    }
     /**
     * Deletes "orphan" Solr documents (that don't match anything in the database).
     * @param sync - optional parameter, if set, then run the command 
     * synchronously. Else, return immediately, and report the results in server.log
     * @return what documents, if anything, was deleted
     */
    @GET
    @Path("clear-orphans")
    /**
     * Checks whether there are inconsistencies between the Solr index and the
     * database, and reports back the status by content type
     *
     * @param sync - optional parameter, if !=null, then run the command
     * synchronously. Else, return immediately, and report the status in
     * server.log
     * @return
     */
    public Response clearOrphans(@QueryParam("sync") String sync) {
        Future<JsonObjectBuilder> result = indexBatchService.clearOrphans();
        if (sync != null) {
            try {
                JsonObjectBuilder status = result.get();
                return ok(status);
            } catch (InterruptedException | ExecutionException e) {
                return AbstractApiBean.error(Status.INTERNAL_SERVER_ERROR, "indexStatus method interrupted: " + e.getLocalizedMessage());
            }
        } else {
            return ok("Clear Orphans Batch Job initiated, check log for job status.");
        }
    }
  
    /**
     * We use the output of this method to generate our Solr schema.xml
     *
     * @todo Someday we do want to have this return a Response rather than a
     * String per https://github.com/IQSS/dataverse/issues/298 but not yet while
     * we are trying to ship Dataverse 4.0.
     */
    @GET
    @Path("solr/schema")
    public String getSolrSchema() {

        StringBuilder sb = new StringBuilder();
        Map<Long, JsonObject> cvocTermUriMap = datasetFieldSvc.getCVocConf(true);
        for (DatasetFieldType datasetFieldType : datasetFieldService.findAllOrderedByName()) {
            //ToDo - getSolrField() creates/returns a new object - just get it once and re-use
            String nameSearchable = datasetFieldType.getSolrField().getNameSearchable();
            SolrField.SolrType solrType = datasetFieldType.getSolrField().getSolrType();
            String type = solrType.getType();
            if (solrType.equals(SolrField.SolrType.EMAIL)) {
                /**
                 * @todo should we also remove all "email" field types (e.g.
                 * datasetContact) from schema.xml? We are explicitly not
                 * indexing them for
                 * https://github.com/IQSS/dataverse/issues/759
                 *
                 * "The list of potential collaborators should be searchable"
                 * according to https://github.com/IQSS/dataverse/issues/747 but
                 * it's not clear yet if this means a Solr or database search.
                 * For now we'll keep schema.xml as it is to avoid people having
                 * to update it. If anything, we can remove the email field type
                 * when we do a big schema.xml update for
                 * https://github.com/IQSS/dataverse/issues/754
                 */
                logger.info("email type detected (" + nameSearchable + ") See also https://github.com/IQSS/dataverse/issues/759");
            }
            String multivalued = Boolean.toString(datasetFieldType.getSolrField().isAllowedToBeMultivalued() || cvocTermUriMap.containsKey(datasetFieldType.getId()));
            // <field name="datasetId" type="text_general" multiValued="false" stored="true" indexed="true"/>
            sb.append("    <field name=\"" + nameSearchable + "\" type=\"" + type + "\" multiValued=\"" + multivalued + "\" stored=\"true\" indexed=\"true\"/>\n");
        }

        List<String> listOfStaticFields = new ArrayList<>();
        Object searchFieldsObject = new SearchFields();
        Field[] staticSearchFields = searchFieldsObject.getClass().getDeclaredFields();
        for (Field fieldObject : staticSearchFields) {
            String name = fieldObject.getName();
            String staticSearchField = null;
            try {
                staticSearchField = (String) fieldObject.get(searchFieldsObject);
            } catch (IllegalArgumentException ex) {
            } catch (IllegalAccessException ex) {
            }

            /**
             * @todo: if you search for "pdf" should you get all pdfs? do we
             * need a copyField source="filetypemime_s" to the catchall?
             */
            if (listOfStaticFields.contains(staticSearchField)) {
                return error("static search field defined twice: " + staticSearchField);
            }
            listOfStaticFields.add(staticSearchField);
        }

        sb.append("---\n");
        //ToDo - this is the same for loop as above - could combine into one by using a second string buffer and appending the latter one after the loop. 
        for (DatasetFieldType datasetField : datasetFieldService.findAllOrderedByName()) {
            String nameSearchable = datasetField.getSolrField().getNameSearchable();
            String nameFacetable = datasetField.getSolrField().getNameFacetable();

            if (listOfStaticFields.contains(nameSearchable)) {
                if (nameSearchable.equals(SearchFields.DATASET_DESCRIPTION)) {
                    // Skip, expected conflct.
                } else {
                    return error("searchable dataset metadata field conflict detected with static field: " + nameSearchable);
                }
            }

            if (listOfStaticFields.contains(nameFacetable)) {
                if (nameFacetable.equals(SearchFields.SUBJECT)) {
                    // Skip, expected conflct.
                } else {
                    return error("facetable dataset metadata field conflict detected with static field: " + nameFacetable);
                }
            }

            // <copyField source="*_i" dest="_text_" maxChars="3000"/>
            sb.append("    <copyField source=\"").append(nameSearchable).append("\" dest=\""+ SearchFields.FULL_TEXT + "\" maxChars=\"3000\"/>\n");
        }

        return sb.toString();
    }

    static String error(String message) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("status", "ERROR");
        response.add("message", message);

        return "{\n\t\"status\":\"ERROR\"\n\t\"message\":\"" + message.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n") + "\"\n}";
    }

    /**
     * This method is for integration tests of search.
     */
    @GET
    @Path("test")
    public Response searchDebug(
            @QueryParam("key") String apiToken,
            @QueryParam("q") String query,
            @QueryParam("fq") final List<String> filterQueries) {

        User user = findUserByApiToken(apiToken);
        if (user == null) {
            return error(Response.Status.UNAUTHORIZED, "Invalid apikey ");
        }

        Dataverse subtreeScope = dataverseService.findRootDataverse();

        String sortField = SearchFields.ID;
        String sortOrder = SortBy.ASCENDING;
        int paginationStart = 0;
        boolean dataRelatedToMe = false;
        int numResultsPerPage = Integer.MAX_VALUE;
        SolrQueryResponse solrQueryResponse;
        List<Dataverse> dataverses = new ArrayList<>();
        dataverses.add(subtreeScope);
        try {
            solrQueryResponse = searchService.search(createDataverseRequest(user), dataverses, query, filterQueries, sortField, sortOrder, paginationStart, dataRelatedToMe, numResultsPerPage);
        } catch (SearchException ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage() + ": " + ex.getCause().getLocalizedMessage());
        }

        JsonArrayBuilder itemsArrayBuilder = Json.createArrayBuilder();
        List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
        for (SolrSearchResult solrSearchResult : solrSearchResults) {
            itemsArrayBuilder.add(solrSearchResult.getType() + ":" + solrSearchResult.getNameSort());
        }

        return ok(itemsArrayBuilder);
    }

    /**
     * This method is for integration tests of search.
     */
    @GET
    @Path("permsDebug")
    public Response searchPermsDebug(
            @QueryParam("key") String apiToken,
            @QueryParam("id") Long dvObjectId) {

        User user = findUserByApiToken(apiToken);
        if (user == null) {
            return error(Response.Status.UNAUTHORIZED, "Invalid apikey");
        }

        DvObject dvObjectToLookUp = dvObjectService.findDvObject(dvObjectId);
        if (dvObjectToLookUp == null) {
            return error(Status.BAD_REQUEST, "Could not find DvObject based on id " + dvObjectId);
        }
        List<DvObjectSolrDoc> solrDocs = SolrIndexService.determineSolrDocs(dvObjectToLookUp);

        JsonObjectBuilder data = Json.createObjectBuilder();

        JsonArrayBuilder permissionsData = Json.createArrayBuilder();

        for (DvObjectSolrDoc solrDoc : solrDocs) {
            JsonObjectBuilder dataDoc = Json.createObjectBuilder();
            dataDoc.add(SearchFields.ID, solrDoc.getSolrId());
            dataDoc.add(SearchFields.NAME_SORT, solrDoc.getNameOrTitle());
            JsonArrayBuilder perms = Json.createArrayBuilder();
            for (String perm : solrDoc.getPermissions()) {
                perms.add(perm);
            }
            dataDoc.add(SearchFields.DISCOVERABLE_BY, perms);
            permissionsData.add(dataDoc);
        }
        data.add("perms", permissionsData);

        DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
        NullSafeJsonBuilder timestamps = jsonObjectBuilder();
        timestamps.add(contentChanged, SearchUtil.getTimestampOrNull(dvObject.getModificationTime()));
        timestamps.add(contentIndexed, SearchUtil.getTimestampOrNull(dvObject.getIndexTime()));
        timestamps.add(permsChanged, SearchUtil.getTimestampOrNull(dvObject.getPermissionModificationTime()));
        timestamps.add(permsIndexed, SearchUtil.getTimestampOrNull(dvObject.getPermissionIndexTime()));
        Set<RoleAssignment> roleAssignments = rolesSvc.rolesAssignments(dvObject);
        JsonArrayBuilder roleAssignmentsData = Json.createArrayBuilder();
        for (RoleAssignment roleAssignment : roleAssignments) {
            roleAssignmentsData.add(roleAssignment.getRole() + " has been granted to " + roleAssignment.getAssigneeIdentifier() + " on " + roleAssignment.getDefinitionPoint());
        }
        data.add("timestamps", timestamps);
        data.add("roleAssignments", roleAssignmentsData);

        return ok(data);
    }

    @DELETE
    @Path("timestamps")
    public Response deleteAllTimestamps() {
        int numItemsCleared = dvObjectService.clearAllIndexTimes();
        return ok("cleared: " + numItemsCleared);
    }

    @DELETE
    @Path("timestamps/{dvObjectId}")
    public Response deleteTimestamp(@PathParam("dvObjectId") long dvObjectId) {
        int numItemsCleared = dvObjectService.clearIndexTimes(dvObjectId);
        return ok("cleared: " + numItemsCleared);
    }

    @GET
    @AuthRequired
    @Path("filesearch")
    public Response filesearch(@Context ContainerRequestContext crc, @QueryParam("persistentId") String persistentId, @QueryParam("semanticVersion") String semanticVersion, @QueryParam("q") String userSuppliedQuery) {
        Dataset dataset = datasetService.findByGlobalId(persistentId);
        if (dataset == null) {
            return error(Status.BAD_REQUEST, "Could not find dataset with persistent id " + persistentId);
        }
        User user = GuestUser.get();
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (authenticatedUser != null) {
                user = authenticatedUser;
            }
        } catch (WrappedResponse ex) {
        }
        RetrieveDatasetVersionResponse datasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, semanticVersion);
        if (datasetVersionResponse == null) {
            return error(Status.BAD_REQUEST, "Problem searching for files. Could not find dataset version based on " + persistentId + " and " + semanticVersion);
        }
        DatasetVersion datasetVersion = datasetVersionResponse.getDatasetVersion();
        FileView fileView = searchFilesService.getFileView(datasetVersion, user, userSuppliedQuery);
        if (fileView == null) {
            return error(Status.BAD_REQUEST, "Problem searching for files. Null returned from getFileView.");
        }
        JsonArrayBuilder filesFound = Json.createArrayBuilder();
        JsonArrayBuilder cards = Json.createArrayBuilder();
        JsonArrayBuilder fileIds = Json.createArrayBuilder();
        for (SolrSearchResult result : fileView.getSolrSearchResults()) {
            cards.add(result.getNameSort());
            fileIds.add(result.getEntityId());
            JsonObjectBuilder fileFound = Json.createObjectBuilder();
            fileFound.add("name", result.getNameSort());
            fileFound.add("entityId", result.getEntityId().toString());
            fileFound.add("datasetVersionId", result.getDatasetVersionId());
            fileFound.add("datasetId", result.getParent().get(SearchFields.ID));
            filesFound.add(fileFound);
        }
        JsonArrayBuilder facets = Json.createArrayBuilder();
        for (FacetCategory facetCategory : fileView.getFacetCategoryList()) {
            facets.add(facetCategory.getFriendlyName());
        }
        JsonArrayBuilder filterQueries = Json.createArrayBuilder();
        for (String filterQuery : fileView.getFilterQueries()) {
            filterQueries.add(filterQuery);
        }
        JsonArrayBuilder allDatasetVersionIds = Json.createArrayBuilder();
        for (DatasetVersion dsVersion : dataset.getVersions()) {
            allDatasetVersionIds.add(dsVersion.getId());
        }
        JsonObjectBuilder data = Json.createObjectBuilder();
        data.add("filesFound", filesFound);
        data.add("cards", cards);
        data.add("fileIds", fileIds);
        data.add("facets", facets);
        data.add("user", user.getIdentifier());
        data.add("persistentID", persistentId);
        data.add("query", fileView.getQuery());
        data.add("filterQueries", filterQueries);
        data.add("allDataverVersionIds", allDatasetVersionIds);
        data.add("semanticVersion", datasetVersion.getSemanticVersion());
        return ok(data);
    }

    @GET
    @Path("filemetadata/{dataset_id}")
    public Response getFileMetadataByDatasetId(
            @PathParam("dataset_id") long datasetIdToLookUp,
            @QueryParam("maxResults") int maxResults,
            @QueryParam("sort") String sortField,
            @QueryParam("order") String sortOrder
    ) {
        JsonArrayBuilder data = Json.createArrayBuilder();
        List<FileMetadata> fileMetadatasFound = new ArrayList<>();
        try {
            fileMetadatasFound = dataFileService.findFileMetadataByDatasetVersionId(datasetIdToLookUp, maxResults, sortField, sortOrder);
        } catch (Exception ex) {
            return error(Status.BAD_REQUEST, "error: " + ex.getCause().getMessage() + ex);
        }
        for (FileMetadata fileMetadata : fileMetadatasFound) {
            data.add(fileMetadata.getLabel());
        }
        return ok(data);
    }
    
    private String writeFailureToLog(String localizedMessage, DvObject dvo) {
        String retVal = "";
        String logString = "";
        if(dvo.isInstanceofDataverse()){
            retVal = "Dataverse Indexing failed. " ;
           logString +=  retVal + " You can kickoff a re-index of this dataverse with: \r\n curl http://localhost:8080/api/admin/index/dataverses/" + dvo.getId().toString(); 
        }
        
        if(dvo.isInstanceofDataset()){
            retVal += " Dataset Indexing failed. ";
            logString +=  retVal + " You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dvo.getId().toString(); 
        }
        retVal += " \r\n " + localizedMessage;
        LoggingUtil.writeOnSuccessFailureLog(null, logString, dvo);
        return retVal;
    }

}
