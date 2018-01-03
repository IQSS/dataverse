package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

@RequiredPermissions(Permission.EditDataset)
public class PersistProvJsonProvCommand extends AbstractCommand<JsonObjectBuilder> {

    private static final Logger logger = Logger.getLogger(PersistProvJsonProvCommand.class.getCanonicalName());

    private final DataFile dataFile;
    private final String userInput;

    public PersistProvJsonProvCommand(DataverseRequest aRequest, DataFile dataFile, String userInput) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.userInput = userInput;
    }

    @Override
    public JsonObjectBuilder execute(CommandContext ctxt) throws CommandException {
        StringReader rdr = new StringReader(userInput);
        try {
            JsonObject jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            String error = "A valid JSON object could not be found. PROV-JSON format is expected.";
            throw new IllegalCommandException(error, this);
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        /**
         * TODO: We are not yet validating the JSON received as PROV-JSON, but
         * we could some day.
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
        // "json.json" looks a little redundand but the standard is called PROV-JSON and it's nice to see .json on disk.
        final String provJsonExtension = "prov-json.json";
        try {
            StorageIO<DataFile> storageIO = dataFile.getStorageIO();
            InputStream inputStream = new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8.name()));
            storageIO.saveInputStreamAsAux(inputStream, provJsonExtension);
        } catch (IOException ex) {
            String error = "Exception caught persisting PROV-JSON: " + ex;
            throw new IllegalCommandException(error, this);
        }
        // Read from StorageIO and show it to the user. This is sort of overkill. We're just making sure we can get it from disk.
        try {
            StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
            InputStream inputStream = dataAccess.getAuxFileAsInputStream(provJsonExtension);
            JsonReader jsonReader = Json.createReader(inputStream);
            JsonObject jsonObject = jsonReader.readObject();
            response.add("message", "PROV-JSON provenance data saved: " + jsonObject.toString());
            return response;
        } catch (IOException ex) {
            String error = "Exception caught in DataAccess.getStorageIO(dataFile): " + ex;
            throw new IllegalCommandException(error, this);
        }
    }

}
