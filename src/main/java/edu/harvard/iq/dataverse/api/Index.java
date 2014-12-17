package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import java.util.List;
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
import javax.ws.rs.core.Response;

@Path("index")
public class Index extends AbstractApiBean {

    @EJB
    IndexServiceBean indexService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean dataFileService;

    @GET
    public String indexAll() {
        try {
            return indexService.indexAll();
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>> for " + violation.getPropertyPath() + " at " + violation.getLeafBean() + " - " + violation.getMessage() + ")");
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
                return "indexing went as well as can be expected... got java.lang.IllegalStateException but some indexing may have happened anyway\n";
            } else {
                return Util.message2ApiError(sb.toString());
            }
        }
    }

    /**
     * @todo return Response rather than String
     */
    @GET
    @Path("{type}/{id}")
    public String indexTypeById(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            if (type.equals("dataverses")) {
                Dataverse dataverse = dataverseService.find(id);
                if (dataverse != null) {
                    return indexService.indexDataverse(dataverse) + "\n";
                } else {
                    String response = indexService.removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataverse + id);
                    return "Could not find dataverse with id of " + id + ". Result from deletion attempt: " + response;
                }
            } else if (type.equals("datasets")) {
                Dataset dataset = datasetService.find(id);
                if (dataset != null) {
                    return indexService.indexDataset(dataset) + "\n";
                } else {
                    /**
                     * @todo what about published, deaccessioned, etc.? Need
                     * method to target those, not just drafts!
                     */
                    String response = indexService.removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataset + id + IndexServiceBean.draftSuffix);
                    return "Could not find dataset with id of " + id + ". Result from deletion attempt: " + response;
                }
            } else if (type.equals("files")) {
                DataFile dataFile = dataFileService.find(id);
                Dataset datasetThatOwnsTheFile = datasetService.find(dataFile.getOwner().getId());
                String output = indexService.indexDataset(datasetThatOwnsTheFile);
                return "indexed " + type + "/" + id + " " + output + "\n";
            } else {
                return Util.message2ApiError("illegal type: " + type);
            }
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append("Problem indexing " + type + "/" + id + ": ");
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>> for " + violation.getPropertyPath() + " at " + violation.getLeafBean() + " - " + violation.getMessage() + ")");
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
            return Util.message2ApiError(sb.toString());
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
        List<Dataverse> stateOrMissingDataverses = indexService.findStaleOrMissingDataverses();
        List<Dataset> staleOrMissingDatasets = indexService.findStaleOrMissingDatasets();
        List<Long> dataversesInSolrOnly;
        List<Long> datasetsInSolrOnly;
        try {
            dataversesInSolrOnly = indexService.findDataversesInSolrOnly();
            datasetsInSolrOnly = indexService.findDatasetsInSolrOnly();
        } catch (SearchException ex) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Can not determine index status. " + ex.getLocalizedMessage() + ". Is Solr down? Exception: " + ex.getCause().getLocalizedMessage());
        }

        JsonArrayBuilder jsonStateOrMissingDataverses = Json.createArrayBuilder();
        for (Dataverse dataverse : stateOrMissingDataverses) {
            jsonStateOrMissingDataverses.add(dataverse.getId());
        }
        JsonArrayBuilder datasetsInDatabaseButNotSolr = Json.createArrayBuilder();
        for (Dataset dataset : staleOrMissingDatasets) {
            datasetsInDatabaseButNotSolr.add(dataset.getId());
        }

        JsonArrayBuilder dataversesInSolrButNotDatabase = Json.createArrayBuilder();
        JsonArrayBuilder datasetsInSolrButNotDatabase = Json.createArrayBuilder();
        for (Long dataverseId : dataversesInSolrOnly) {
            dataversesInSolrButNotDatabase.add(dataverseId);
        }
        for (Long datasetId : datasetsInSolrOnly) {
            datasetsInSolrButNotDatabase.add(datasetId);
        }
        JsonObjectBuilder contentInDatabaseButStaleInOrMissingFromSolr = Json.createObjectBuilder()
                .add("dataverses", jsonStateOrMissingDataverses)
                .add("datasets", datasetsInDatabaseButNotSolr);

        JsonObjectBuilder contentInSolrButNotDatabase = Json.createObjectBuilder()
                .add("dataverses", dataversesInSolrButNotDatabase)
                .add("datasets", datasetsInSolrButNotDatabase);

        JsonObjectBuilder permissions = Json.createObjectBuilder()
                .add("dataverses", "FIXME")
                .add("datasets", "FIXME")
                .add("files", "FIXME");

        JsonObjectBuilder data = Json.createObjectBuilder()
                .add("contentInDatabaseButStaleInOrMissingFromIndex", contentInDatabaseButStaleInOrMissingFromSolr)
                .add("contentInIndexButNotDatabase", contentInSolrButNotDatabase)
                .add("permissions", permissions);

        return okResponse(data);
    }

}
