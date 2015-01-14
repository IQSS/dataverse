package edu.harvard.iq.dataverse.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportDDI;
import edu.harvard.iq.dataverse.api.imports.ImportDDI.ImportType;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.validation.ConstraintViolation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

@Stateless
@Path("batch")
public class BatchImport extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(BatchImport.class.getCanonicalName());
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetFieldServiceBean datasetfieldService;
    @EJB
    MetadataBlockServiceBean metadataBlockService;
    
      
    @GET
    @Path("migrate/{identifier}")
    public Response migrate(@QueryParam("path") String fileDir, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
        JsonArrayBuilder status = Json.createArrayBuilder();
        ImportType importType = ImportType.MIGRATION;
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
            return this.errorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        }
        return this.okResponse(status);
    }
    
    private JsonArrayBuilder handleDirectory(User u, File dir, ImportType importType)throws ImportException {
        JsonArrayBuilder status = Json.createArrayBuilder();
        Dataverse owner = findDataverse(dir.getName());
        if (owner==null) {
            throw new ImportException( "Can't find dataverse with identifier='" + dir.getName() + "'");
 
        }
        for (File file : dir.listFiles()) {
            try {
                JsonObjectBuilder fileStatus = handleFile(u, owner, file, importType);
                status.add( fileStatus);
            } catch (ImportException e) {
                status.add(Json.createObjectBuilder().add("importStatus","Exception importing "+ file.getName()+", message = "+ e.getMessage()));
                
            }
        }
        return status;
    }
    
    private JsonObjectBuilder handleFile(User u, Dataverse owner, File file, ImportType importType) throws ImportException {
        System.out.println("handling file: "+file.getAbsolutePath());
        String status = "";
        Long createdId = null;
        DatasetDTO dsDTO = null;
        try {
            // Read XML into DTO object
            String ddiXMLToParse = new String(Files.readAllBytes(file.toPath()));
            ImportDDI importDDI = new ImportDDI(importType);
            dsDTO = importDDI.doImport(ddiXMLToParse);

        } catch (IOException e) {
            throw new ImportException("Error reading path " + file.getAbsolutePath(), e);
        } catch (XMLStreamException e) {

            throw new ImportException("XMLStreamException" + file.getName(), e);
        }
        // convert DTO to Json, 
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dsDTO);
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();
                     //and call parse Json to read it into a dataset
        // TODO: add more information in Http Response
        try {
            Dataset ds = new JsonParser(datasetFieldSvc, metadataBlockService).parseDataset(obj);
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
                    execCommand(new DestroyDatasetCommand(existingDs,u), "Destroying existing Harvested Dataset");
                    Dataset managedDs = execCommand(new CreateDatasetCommand(ds, u, false, true), "Creating Harvested Dataset");
                    status = " updated dataset, id=" + managedDs.getId() + ".";   
                } else {
                    // If we are adding a new version to an existing dataset,
                    // check that the version number isn't already in the dataset
                    for (DatasetVersion dsv : existingDs.getVersions()) {
                        if (dsv.getVersionNumber().equals(ds.getLatestVersion().getVersionNumber())) {
                            throw new ImportException("VersionNumber " + ds.getLatestVersion().getVersionNumber() + " already exists in dataset " + existingDs.getGlobalId());
                        }
                    }
                DatasetVersion dsv = execCommand(new CreateDatasetVersionCommand(u, existingDs, ds.getVersions().get(0)), "Creating DatasetVersion");
                status = " created datasetVersion, id=" + dsv.getId() + ".";       
                createdId = dsv.getId();
                }
               
            } else {
                Dataset managedDs = execCommand(new CreateDatasetCommand(ds, u, false, true), "Creating Dataset");
                status = " created dataset, id=" + managedDs.getId() + ".";
                createdId = managedDs.getId();
            }

        } catch (JsonParseException ex) {
            logger.log(Level.INFO, "Error parsing dataset version from Json", ex);
            throw new ImportException("Error parsing initialVersion: " + ex.getMessage(), ex);
        } catch (WrappedResponse e) {
            throw new ImportException("Error executing command" + e.getMessage(), e.getCause());
        }
        return Json.createObjectBuilder().add("message", status).add("id", createdId);
    }

}
