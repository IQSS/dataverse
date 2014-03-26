package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;

    @GET
    public String list(@QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        List<Dataset> datasets = datasetService.findAll();
        JsonArrayBuilder datasetsArrayBuilder = Json.createArrayBuilder();
        for (Dataset dataset : datasets) {
           datasetsArrayBuilder.add( json(dataset) );
        }
		return ok(datasetsArrayBuilder);
        
    }
	
	@GET
	@Path("{id}")
    public String getDataset( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(id);
        return (ds != null) ? ok(json(ds))
							: error("dataset not found");
		
        
    }
	
	@GET
	@Path("{id}/versions")
    public String listVersions( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(id);
        if (ds == null) return error("dataset not found");
		
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( DatasetVersion dsv : ds.getVersions() ) {
			bld.add( json(dsv) );
		}
		
		return ok( bld );
    }
	
	@GET
	@Path("{id}/versions/{versionId}")
    public String getVersion( @PathParam("id") Long id, @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(id);
        if (ds == null) return error("dataset not found");
		
		DatasetVersion dsv = null;
		switch (versionId) {
			case ":latest":
				dsv = ds.getLatestVersion();
				break;
			case ":edit":
				dsv = ds.getEditVersion();
				break;
			default:
				try {
					long versionNumericId = Long.parseLong(versionId);
					for ( DatasetVersion aDsv : ds.getVersions() ) {
						if ( aDsv.getId().equals(versionNumericId) ) {
							dsv = aDsv;
							break; // for, not while
						}
					}
				} catch ( NumberFormatException nfe ) {
					return error("Illegal id number '" + versionId + "'");
				}	break;
		}
		
		return (dsv==null)
				? error("dataset version not found")
				: ok( json(dsv)  );
    }
	
	@GET
	@Path("{id}/versions/{versionId}/files/")
	public String listFiles() {
		// TODO implement
		return error("Not implemented yet");
	}
	
	
	@POST
	@Path("{id}/versions")
	public String addVersion( @PathParam("id") Long id, @QueryParam("key") String apikey, DatasetDTO dsDto ){
		// CONTPOINT accept the dsDto and push it to the DB.
		return null;
	}
	
    // used to primarily to feed data into elasticsearch
    @GET
	@Deprecated
    @Path("deprecated/{id}/{verb}")
    public Dataset get(@PathParam("id") Long id, @PathParam("verb") String verb) {
        logger.info("GET called");
        if (verb.equals("dump")) {
            Dataset dataset = datasetService.find(id);
            if (dataset != null) {
                logger.info("found " + dataset);
                // prevent HTTP Status 500 - Internal Server Error
                dataset.setFiles(null);
                dataset.setAuthority(null);
//                dataset.setDescription(null);
                dataset.setIdentifier(null);
                dataset.setProtocol(null);
                dataset.setVersions(null);
                // elasticsearch fails on "today" with
                // MapperParsingException[failed to parse date field [today],
                // tried both date format [dateOptionalTime], and timestamp number with locale []]
                //dataset.setCitationDate(null);
                // too much information
                dataset.setOwner(null);
                return dataset;
            }
        }
        /**
         * @todo return an error instead of "204 No Content"?
         *
         */
        logger.info("GET attempted with dataset id " + id + " and verb " + verb);
        return null;
    }

	@Path("deprecated/")
    @POST
	@Deprecated
    public String add(Dataset dataset, @QueryParam("owner") String owner) {
        try {
            DatasetVersion editVersion = new DatasetVersion();
            editVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
            editVersion.setDataset(dataset);
            Dataverse owningDataverse = dataverseService.findByAlias(owner);
            dataset.setOwner(owningDataverse);
            editVersion.setDatasetFieldValues(editVersion.initDatasetFieldValues());
            dataset.getVersions().add(editVersion);
            dataset.setIdentifier("myIdentifier");
            dataset.setProtocol("myProtocol");
            datasetService.save(dataset);
            return "dataset " + dataset.getId() + " created/updated (and probably indexed, check server.log)\n";
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
//                } else if (cause instanceof NullPointerException) {
                } else {
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
}
