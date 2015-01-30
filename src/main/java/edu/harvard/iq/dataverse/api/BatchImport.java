package edu.harvard.iq.dataverse.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportDDI;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

@Stateless
@Path("batch")
public class BatchImport extends AbstractApiBean  {
@EJB
	protected EjbDataverseEngine engineSvc;
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
            JsonObjectBuilder status = doImport(u,owner,body, ImportType.NEW);
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
        return processFilePath(fileDir, parentIdtf, apiKey, ImportType.NEW);
    }
    
    
    private Response processFilePath(String fileDir, String parentIdtf, String apiKey, ImportType importType ) {
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
                            status.add(handleFile(u, owner, file, importType));
                        }
                    }
                }
            } else {
                status.add(handleFile(u, owner, dir, importType));
            }

        } catch (ImportException e) {
            e.printStackTrace();
            return this.errorResponse(Response.Status.BAD_REQUEST, "Import Exception!!");
        }
        return this.okResponse(status); 
    }
    
    private JsonArrayBuilder handleDirectory(User u, File dir, ImportType importType) throws ImportException {
        JsonArrayBuilder status = Json.createArrayBuilder();
        Dataverse owner = findDataverse(dir.getName());
        if (owner == null) {
            // For now, we will create dataverses that aren't found - this is only for load testing and migration testing
            if (importType.equals(ImportType.MIGRATION) || importType.equals(ImportType.NEW)) {
                System.out.println("creating new dataverse: " + dir.getName());
                Dataverse d = new Dataverse();
                Dataverse root = dataverseService.findByAlias("root");
                d.setOwner(root);
                d.setAlias(dir.getName());
                d.setName(dir.getName());
                d.setAffiliation("affiliation");
                d.setPermissionRoot(false);
                d.setDescription("description");
                d.setDataverseType(DataverseType.RESEARCHERS);
                DataverseContact dc = new DataverseContact();
                dc.setContactEmail("pete@mailinator.com");
                ArrayList<DataverseContact> dcList = new ArrayList<>();
                dcList.add(dc);
                d.setDataverseContacts(dcList);

                try {
                   owner= engineSvc.submit(new CreateDataverseCommand(d, u, null, null));
                } catch (EJBException ex) {
                    Throwable cause = ex;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Error creating dataverse.");
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                        if (cause instanceof ConstraintViolationException) {
                            ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                            for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                                sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                        .append(violation.getPropertyPath()).append(" at ")
                                        .append(violation.getLeafBean()).append(" - ")
                                        .append(violation.getMessage());
                            }
                        }
                    }
                    logger.log(Level.SEVERE, sb.toString());
                    System.out.println("Error creating dataverse: " + sb.toString());
                    throw new ImportException(sb.toString());
                } catch (Exception e) {
                    throw new ImportException(e.getMessage());
                }

            } else {
                throw new ImportException("Can't find dataverse with identifier='" + dir.getName() + "'");
            }
        }
        for (File file : dir.listFiles()) {
            if (!file.isHidden()) {
                try {
                    JsonObjectBuilder fileStatus = handleFile(u, owner, file, importType);
                    status.add(fileStatus);
                } catch (ImportException e) {
                    status.add(Json.createObjectBuilder().add("importStatus", "Exception importing " + file.getName() + ", message = " + e.getMessage()));

                }
            }  
        }
        return status;
    }

    private JsonObjectBuilder handleFile(User u, Dataverse owner, File file, ImportType importType) throws ImportException {
        System.out.println("handling file: " + file.getAbsolutePath());
        String ddiXMLToParse;
        try {
            // Read XML into DTO object
            ddiXMLToParse = new String(Files.readAllBytes(file.toPath()));
            JsonObjectBuilder status = doImport(u, owner, ddiXMLToParse, importType);
            logger.info("completed doImport "+file.getParentFile().getName()+"/"+file.getName());
            return status;
        } catch (IOException e) {
            throw new ImportException("Error reading file " + file.getAbsolutePath(), e);
        } catch (ImportException ex) {
            logger.info("Import Exception processing file "+ file.getParentFile().getName()+"/"+file.getName()+", msg:" + ex.getMessage());
            return Json.createObjectBuilder().add("message", "Import Exception processing file "+ file.getParentFile().getName()+"/"+file.getName()+", msg:" + ex.getMessage());
        }
    }
    
     private JsonObjectBuilder doImport(User u, Dataverse owner, String xmlToParse, ImportType importType) throws ImportException {
      
        String status = "";
        Long createdId = null;
        DatasetDTO dsDTO = null;
        try {            
            ImportDDI importDDI = new ImportDDI(importType);
            dsDTO = importDDI.doImport(xmlToParse);
        } catch (XMLStreamException e) {
            throw new ImportException("XMLStreamException" +  e);
        }
        // convert DTO to Json, 
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dsDTO);
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();
        //and call parse Json to read it into a dataset   
        try {
            Dataset ds = new JsonParser(datasetFieldSvc, metadataBlockService, settingsService).parseDataset(obj);
            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // If there are missing values in Required fields, fill them with "N/A"
            if (importType.equals(ImportType.MIGRATION) || importType.equals(ImportType.HARVEST)) {
                Set<ConstraintViolation> violations = ds.getVersions().get(0).validateRequired();
                if (!violations.isEmpty()) {
                    for (ConstraintViolation v : violations) {
                        DatasetField f = ((DatasetField) v.getRootBean());
                        f.setSingleValue(DatasetField.NA_VALUE);
                    }
                }
            }
            Dataset existingDs = datasetService.findByGlobalId(ds.getGlobalId());

            if (existingDs != null) {
                if (importType.equals(ImportType.HARVEST)) {
                    // For harvested datasets, there should always only be one version.
                    // We will replace the current version with the imported version.
                    if (existingDs.getVersions().size()!=1) {
                        throw new ImportException("Error importing Harvested Dataset, existing dataset has "+ existingDs.getVersions().size() + " versions");
                    }
                    engineSvc.submit(new DestroyDatasetCommand(existingDs,u));
                    Dataset managedDs = engineSvc.submit(new CreateDatasetCommand(ds, u, false, importType));
                    status = " updated dataset, id=" + managedDs.getId() + ".";   
                } else {
                    // If we are adding a new version to an existing dataset,
                    // check that the version number isn't already in the dataset
                    for (DatasetVersion dsv : existingDs.getVersions()) {
                        if (dsv.getVersionNumber().equals(ds.getLatestVersion().getVersionNumber())) {
                            throw new ImportException("VersionNumber " + ds.getLatestVersion().getVersionNumber() + " already exists in dataset " + existingDs.getGlobalId());
                        }
                    }
                DatasetVersion dsv = engineSvc.submit(new CreateDatasetVersionCommand(u, existingDs, ds.getVersions().get(0)));
                status = " created datasetVersion, id=" + dsv.getId() + ".";       
                createdId = dsv.getId();
                }
               
            } else {
                Dataset managedDs = engineSvc.submit(new CreateDatasetCommand(ds, u, false, importType));
                status = " created dataset, id=" + managedDs.getId() + ".";
                createdId = managedDs.getId();
            }

        } catch (JsonParseException ex) {
            
            logger.info("Error parsing datasetVersion: " + ex.getMessage());
            throw new ImportException("Error parsing datasetVersion: " + ex.getMessage(), ex);
        } catch(CommandException ex) {  
            logger.info("Error excuting dataverse command: " + ex.getMessage());
            throw new ImportException("Error excuting dataverse command: " + ex.getMessage(), ex);
        }
        return Json.createObjectBuilder().add("message", status).add("id", createdId);
     }
   
    
       

}
