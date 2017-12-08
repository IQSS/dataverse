package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("files")
public class ProvApi extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(ProvApi.class.getCanonicalName());

    @POST
    @Path("{id}/prov-json")
    @Consumes("application/json")
    public Response addProvJson(String body, @PathParam("id") Long idSupplied) {
        // TODO: Permissions.
        // TODO: Save PROV-JSON file to filesystem/S3. This format: https://www.w3.org/Submission/prov-json/
        DataFile dataFile = fileSvc.find(idSupplied);
        if (dataFile == null) {
            return error(NOT_FOUND, "Could not file a file based on id " + idSupplied + ".");
        }
        StringReader rdr = new StringReader(body);
        try {
            JsonObject jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            return error(Response.Status.BAD_REQUEST, "A valid JSON object could not be found. PROV-JSON format is expected.");
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        /**
         * We are not yet validating the JSON received as PROV-JSON, but we
         * could some day.
         *
         * Section "2.3 Validating PROV-JSON Documents" of the spec says, "A
         * schema for PROV-JSON is provided, which defines all the valid
         * PROV-JSON constructs described in this document. The schema was
         * written using the schema language specified in [JSON-SCHEMA] (Version
         * 4). It can be used for the purpose of validating PROV-JSON documents.
         * A number of libraries for JSON schema validation are available at
         * json-schema.org/implementations.html." It links to
         * https://www.w3.org/Submission/prov-json/schema
         */
        // Write to StorageIO.
        StorageIO<DataFile> storageIO = null;
        try {
            storageIO = DataAccess.createNewStorageIO(dataFile, "prov-json");
            Channel outputChannel = storageIO.openAuxChannel(dataFile.getId() + ".json", DataAccessOption.WRITE_ACCESS);
            OutputStream outputStream = Channels.newOutputStream((WritableByteChannel) outputChannel);
            logger.info("writing body: " + body);
            outputStream.write(body.toString().getBytes("UTF8"));
            // FIXME: Investigate why two file are being saved:
            // - 10.5072/FK2/TZF120/prov-json (empty)
            // - 10.5072/FK2/TZF120/prov-json.165.json (looks good)
            logger.info("done writing body");
        } catch (IOException ex) {
            logger.warning("Exception caught writing body: " + ex);
        }
        // Read from StorageIO. FIXME: This isn't working. The idea was to make sure we can read back the bits we persisted to StorageIO above.
        boolean readBackFromStorageIo = false;
        if (readBackFromStorageIo) {
            StorageIO<DataFile> dataAccess = null;
            try {
                dataAccess = DataAccess.getStorageIO(dataFile);
            } catch (IOException ex) {
                logger.info("Exception caught in DataAccess.getStorageIO(dataFile): " + ex);
            }
            InputStream cachedExportInputStream = null;
            try {
                cachedExportInputStream = dataAccess.getAuxFileAsInputStream("prov-json" + dataFile.getId() + ".json");
            } catch (IOException ex) {
                logger.info("Exception caught in dataAccess.getAuxFileAsInputStream: " + ex);
            }
            if (cachedExportInputStream == null) {
                return error(INTERNAL_SERVER_ERROR, "cachedExportInputStream was null.");
            }
            JsonReader jsonReader = Json.createReader(cachedExportInputStream);
            JsonObject jsonObj2 = jsonReader.readObject();
            response.add("message", "PROV-JSON provenance data saved: " + jsonObj2.toString());
        } else {
            response.add("message", "PROV-JSON provenance data saved.");
        }
        return ok(response);
    }

    @POST
    @Path("{id}/prov-freeform")
    @Consumes("application/json")
    public Response addProvFreeForm(String body, @PathParam("id") Long idSupplied) {
        // TODO: Permissions.
        // TODO: Save prov free form text to FileMetadata table.
        DataFile dataFile = fileSvc.find(idSupplied);
        if (dataFile == null) {
            return error(NOT_FOUND, "Could not file a file based on id " + idSupplied + ".");
        }
        StringReader rdr = new StringReader(body);
        JsonObject jsonObj = null;
        try {
            jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            return error(BAD_REQUEST, "A valid JSON object could not be found.");
        }
        String provFreeForm = jsonObj.getString("text");
        FileMetadata fileMetadata = dataFile.getFileMetadata();
        fileMetadata.setProvFreeForm(provFreeForm);
        DataFile savedDataFile = fileSvc.save(dataFile);
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("message", "Free-form provenance data saved: " + savedDataFile.getFileMetadata().getProvFreeForm());
        return ok(response);
    }

}
