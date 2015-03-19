package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.search.IndexAllServiceBean;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import java.util.List;
import java.util.concurrent.Future;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("index")
public class Index extends AbstractApiBean {

    @EJB
    IndexServiceBean indexService;
    @EJB
    IndexAllServiceBean indexAllService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean dataFileService;

    @GET
    public Response indexAll(@QueryParam("async") Boolean async) {
        try {
            /**
             * @todo How can we expose the String returned from "index all" via
             * the API?
             */
            if (async != null) {
                if (async) {
                    Future<String> indexAllFuture = indexAllService.indexAll(async);
                    return okResponse("index all has been started (async)");
                } else {
                    // async=false (or async=foo which is a little weird)
                    Future<String> indexAllFuture = indexAllService.indexAll(async);
                    return okResponse("index all has been started (non-async)");
                }
            } else {
                boolean defaultAsyncParam = false;
                Future<String> indexAllFuture = indexAllService.indexAll(defaultAsyncParam);
                return okResponse("index all has been started (async=" + defaultAsyncParam + ", the default)");
            }
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage()).append(" ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage()).append(")");
                    }
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
            if (sb.toString().equals("javax.ejb.EJBException: Transaction aborted javax.transaction.RollbackException java.lang.IllegalStateException ")) {
                return okResponse("indexing went as well as can be expected... got java.lang.IllegalStateException but some indexing may have happened anyway");
            } else {
                return errorResponse( Status.INTERNAL_SERVER_ERROR, sb.toString() );
            }
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
                    Future<String> indexDataverseFuture = indexService.indexDataverse(dataverse);
                    return okResponse("starting reindex of dataverse " + id);
                } else {
                    String response = indexService.removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataverse + id);
                    return notFound("Could not find dataverse with id of " + id + ". Result from deletion attempt: " + response);
                }
            } else if (type.equals("datasets")) {
                Dataset dataset = datasetService.find(id);
                if (dataset != null) {
                    Future<String> indexDatasetFuture = indexService.indexDataset(dataset);
                    return okResponse("starting reindex of dataset " + id);
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
                Future<String> indexDatasetFuture = indexService.indexDataset(datasetThatOwnsTheFile);
                return okResponse("started reindexing " + type + "/" + id);
            } else {
                return errorResponse( Status.BAD_REQUEST, "illegal type: " + type);
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
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage()).append(")");
                    }
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
            return errorResponse( Status.INTERNAL_SERVER_ERROR, sb.toString() );
        }
    }

    @GET
    @Path("perms")
    public Response indexAllPermissions() {
        IndexResponse indexResponse = solrIndexService.indexAllPermissions();
        return okResponse(indexResponse.getMessage());
    }

    @GET
    @Path("perms/{id}")
    public Response indexPermissions(@PathParam("id") Long id) {
        IndexResponse indexResponse = solrIndexService.indexPermissionsForOneDvObject(id);
        return okResponse(indexResponse.getMessage());
    }

    @GET
    @Path("status")
    public Response indexStatus() {
        JsonObjectBuilder contentInDatabaseButStaleInOrMissingFromSolr = getContentInDatabaseButStaleInOrMissingFromSolr();

        JsonObjectBuilder contentInSolrButNotDatabase;
        try {
            contentInSolrButNotDatabase = getContentInSolrButNotDatabase();
        } catch (SearchException ex) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Can not determine index status. " + ex.getLocalizedMessage() + ". Is Solr down? Exception: " + ex.getCause().getLocalizedMessage());
        }

        JsonObjectBuilder permissionsInDatabaseButMissingFromSolr;
        try {
            permissionsInDatabaseButMissingFromSolr = getPermissionsInDatabaseButStaleInOrMissingFromSolr();
        } catch (Exception ex) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
        }
        JsonObjectBuilder permissionsInSolrButNotDatabase = getPermissionsInSolrButNotDatabase();

        JsonObjectBuilder data = Json.createObjectBuilder()
                .add("contentInDatabaseButStaleInOrMissingFromIndex", contentInDatabaseButStaleInOrMissingFromSolr)
                .add("contentInIndexButNotDatabase", contentInSolrButNotDatabase)
                .add("permissionsInDatabaseButMissingFromSolr", permissionsInDatabaseButMissingFromSolr)
                .add("permissionsInIndexButNotDatabase", permissionsInSolrButNotDatabase);

        return okResponse(data);
    }

    private JsonObjectBuilder getContentInDatabaseButStaleInOrMissingFromSolr() {
        List<Dataverse> stateOrMissingDataverses = indexService.findStaleOrMissingDataverses();
        List<Dataset> staleOrMissingDatasets = indexService.findStaleOrMissingDatasets();
        JsonArrayBuilder jsonStateOrMissingDataverses = Json.createArrayBuilder();
        for (Dataverse dataverse : stateOrMissingDataverses) {
            jsonStateOrMissingDataverses.add(dataverse.getId());
        }
        JsonArrayBuilder datasetsInDatabaseButNotSolr = Json.createArrayBuilder();
        for (Dataset dataset : staleOrMissingDatasets) {
            datasetsInDatabaseButNotSolr.add(dataset.getId());
        }
        JsonObjectBuilder contentInDatabaseButStaleInOrMissingFromSolr = Json.createObjectBuilder()
                /**
                 * @todo What about files? Currently files are always indexed
                 * along with their parent dataset
                 */
                .add("dataverses", jsonStateOrMissingDataverses)
                .add("datasets", datasetsInDatabaseButNotSolr);
        return contentInDatabaseButStaleInOrMissingFromSolr;
    }

    private JsonObjectBuilder getContentInSolrButNotDatabase() throws SearchException {
        List<Long> dataversesInSolrOnly = indexService.findDataversesInSolrOnly();
        List<Long> datasetsInSolrOnly = indexService.findDatasetsInSolrOnly();
        JsonArrayBuilder dataversesInSolrButNotDatabase = Json.createArrayBuilder();
        for (Long dataverseId : dataversesInSolrOnly) {
            dataversesInSolrButNotDatabase.add(dataverseId);
        }
        JsonArrayBuilder datasetsInSolrButNotDatabase = Json.createArrayBuilder();
        for (Long datasetId : datasetsInSolrOnly) {
            datasetsInSolrButNotDatabase.add(datasetId);
        }
        JsonObjectBuilder contentInSolrButNotDatabase = Json.createObjectBuilder()
                /**
                 * @todo What about files? Currently files are always indexed
                 * along with their parent dataset
                 */
                .add("dataverses", dataversesInSolrButNotDatabase)
                .add("datasets", datasetsInSolrButNotDatabase);
        return contentInSolrButNotDatabase;
    }

    private JsonObjectBuilder getPermissionsInDatabaseButStaleInOrMissingFromSolr() throws Exception {
        List<Long> staleOrMissingPermissions;
        staleOrMissingPermissions = solrIndexService.findPermissionsMissingFromSolr();
        JsonArrayBuilder stalePermissionList = Json.createArrayBuilder();
        for (Long dvObjectId : staleOrMissingPermissions) {
            stalePermissionList.add(dvObjectId);
        }
        return Json.createObjectBuilder()
                .add("dvobjects", stalePermissionList);
    }

    private JsonObjectBuilder getPermissionsInSolrButNotDatabase() {
        List<Long> staleOrMissingPermissions = solrIndexService.findPermissionsInSolrNoLongerInDatabase();
        JsonArrayBuilder stalePermissionList = Json.createArrayBuilder();
        for (Long dvObjectId : staleOrMissingPermissions) {
            stalePermissionList.add(dvObjectId);
        }
        return Json.createObjectBuilder()
                .add("dvobjects", stalePermissionList);
    }

}
