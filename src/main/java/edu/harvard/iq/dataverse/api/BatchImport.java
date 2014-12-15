package edu.harvard.iq.dataverse.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportDDI;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
       

         File dir = new File(fileDir);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!file.isHidden()) {
                DatasetDTO dsDTO = null;
                try {
                    // Read XML into DTO object
                    String ddiXMLToParse = new String(Files.readAllBytes(file.toPath()));
                    ImportDDI importDDI = new ImportDDI(ImportDDI.ImportType.MIGRATION);
                    dsDTO = importDDI.doImport(ddiXMLToParse);
                    // EMK TODO: replace this hard-coded version state
                    dsDTO.getDatasetVersion().setVersionState(VersionState.DRAFT);
                } catch (IOException e) {
                    return errorResponse(Response.Status.NOT_FOUND, "Error reading path " + fileDir);
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                    return errorResponse(Response.Status.BAD_REQUEST, "XMLStreamException");
                }
                // 
                // convert DTO to Json, 
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(dsDTO);
                System.out.println("json: "+json);
                JsonReader jsonReader = Json.createReader(new StringReader(json));
                JsonObject obj = jsonReader.readObject();
                //and call parse Json to read it into a dataset
                try {
                    Dataset ds = new JsonParser(datasetFieldSvc, metadataBlockService).parseDataset(obj);
                    ds.setOwner(owner);
                    Dataset managedDs = execCommand(new CreateDatasetCommand(ds, u), "Creating Dataset");
                    return createdResponse("/datasets/" + managedDs.getId(),
                            Json.createObjectBuilder().add("id", managedDs.getId()));

                } catch (JsonParseException ex) {
                    logger.log(Level.INFO, "Error parsing dataset version from Json", ex);
                    return errorResponse(Response.Status.BAD_REQUEST, "Error parsing initialVersion: " + ex.getMessage());
                } catch (WrappedResponse e) {
                    return e.getResponse();
                }
            }
            }

        }
        return this.okResponse("completed");
    }

    
}
