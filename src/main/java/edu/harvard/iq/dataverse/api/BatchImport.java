package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.File;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Stateless
@Path("batch")
public class BatchImport extends AbstractApiBean  {
    private static final Logger logger = Logger.getLogger(BatchImport.class.getCanonicalName());
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetFieldServiceBean datasetfieldService;
    @EJB
    MetadataBlockServiceBean metadataBlockService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    ImportServiceBean batchImportService;
    
    /** migrate - only needed for importing studies from old DVN installations into Dataverse 4.0
     * read ddi files from the filesystem, and import them in "migrate" mode
     * @param fileDir - the full path of the parent directory where the files are located. 
     * If there are subdirectories, then ddi's will be imported into the dataverse matching the subdirectory name (alias)
     * @param parentIdtf - the dataverse that the top-level files should be imported to - if null, then use root dataverse.
     * @param apiKey - users's apiKey
     * @return 
     */
    @GET
    @Path("migrate")
    public Response migrate(@QueryParam("path") String fileDir, @QueryParam("dv") String parentIdtf, @QueryParam("key") String apiKey) {
        return processFilePath(fileDir, parentIdtf, apiKey, ImportType.MIGRATION);
    }
    
    @GET
    @Path("harvest")
    public Response harvest(@QueryParam("path") String fileDir, @QueryParam("dv") String parentIdtf, @QueryParam("key") String apiKey) {
        return processFilePath(fileDir, parentIdtf, apiKey, ImportType.HARVEST);
    }
    
    /**
     * Import a new Dataset with DDI xml data posted in the request
     * @param body  the xml 
     * @param parentIdtf the dataverse to import into (id or alias)
     * @param apiKey  user's api key
     * @return import status (including id of the dataset created)
     */
    @POST 
    @Path("import")
    public Response postImport(String body, @QueryParam("dv") String parentIdtf, @QueryParam("key") String apiKey) {
        User u = findUserByApiToken(apiKey);
        if (u == null) {
            return badApiKey(apiKey);
        }
        if (parentIdtf == null) {
            parentIdtf = "root";
        }
        Dataverse owner = findDataverse(parentIdtf);
        if (owner == null) {
            return errorResponse(Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
        }
        try { 
            JsonObjectBuilder status = batchImportService.doImport(u,owner,body, ImportType.NEW);
            return this.okResponse(status);
        } catch(ImportException e) {
            return this.errorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }
    /**
     * Import single or multiple datasets that are in the local filesystem
     * @param fileDir the absolute path of the file or directory (all files within the directory will be imported
     * @param parentIdtf the dataverse to import into (id or alias)
     * @param apiKey  user's api key
     * @return import status (including id's of the datasets created)
     */
    @GET 
    @Path("import")
    public Response getImport(@QueryParam("path") String fileDir, @QueryParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
         Response r= processFilePath(fileDir, parentIdtf, apiKey, ImportType.NEW);
        return r;
    }
    
    
    private Response processFilePath(String fileDir, String parentIdtf, String apiKey, ImportType importType ) {
   logger.info("BEGIN IMPORT");
            JsonArrayBuilder status = Json.createArrayBuilder();
        
        User u = findUserByApiToken(apiKey);
        if (u == null) {
            return badApiKey(apiKey);
        }
        if (parentIdtf == null) {
            parentIdtf = "root";
        }
        Dataverse owner = findDataverse(parentIdtf);
        if (owner == null) {
            return errorResponse(Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
        }
        try {
            File dir = new File(fileDir);
            if (dir.isDirectory()) { 
                for (File file : dir.listFiles()) {
                    if (!file.isHidden()) {
                        if (file.isDirectory()) {
                            status.add(handleDirectory(u, file, importType));
                        } else {
                            status.add(batchImportService.handleFile(u, owner, file, importType));
                        }
                    }
                }
            } else {
                status.add(batchImportService.handleFile(u, owner, dir, importType));
            }

        } catch (ImportException e) {
            e.printStackTrace();
            return this.errorResponse(Response.Status.BAD_REQUEST, "Import Exception!!");
        }
        
        logger.info("END IMPORT");
        return this.okResponse(status); 
    }
   public JsonArrayBuilder handleDirectory(User u, File dir, ImportType importType) throws ImportException {
        JsonArrayBuilder status = Json.createArrayBuilder();
        Dataverse owner = dataverseService.findByAlias(dir.getName());
        if (owner == null) {
            if (importType.equals(ImportUtil.ImportType.MIGRATION) || importType.equals(ImportUtil.ImportType.NEW)) {
                System.out.println("creating new dataverse: " + dir.getName());
                owner=batchImportService.createDataverse(dir, u);
            } else {
                throw new ImportException("Can't find dataverse with identifier='" + dir.getName() + "'");
            }
        }
        for (File file : dir.listFiles()) {
            if (!file.isHidden()) {
                try {
                    JsonObjectBuilder fileStatus = batchImportService.handleFile(u, owner, file, importType);
                    status.add(fileStatus);
                } catch (ImportException e) {
                    status.add(Json.createObjectBuilder().add("importStatus", "Exception importing " + file.getName() + ", message = " + e.getMessage()));
                }
            }
        }
        return status;
    }

    
    
   
    
       

}
