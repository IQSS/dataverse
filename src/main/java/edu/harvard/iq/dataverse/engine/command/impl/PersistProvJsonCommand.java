package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
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


@RequiredPermissions(Permission.EditDataset)
public class PersistProvJsonCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(PersistProvJsonCommand.class.getCanonicalName());

    private DataFile dataFile;
    private final String jsonInput;
    private final String entityName;
    private final boolean saveContext;
    
//MAD: Maybe this shouldn't take entityName at all, as that can be stored in the dataFile. But we can also just pass it in separately if that's needed for the api
    public PersistProvJsonCommand(DataverseRequest aRequest, DataFile dataFile, String jsonInput, String entityName, boolean saveContext) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.jsonInput = jsonInput;
        this.entityName = entityName;
        this.saveContext = saveContext;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {
        //First, save the name of the entity in the json so CPL can later connect the uploaded json
        if(null == entityName || "".equals(entityName)) {
            String error = "A valid entityName must be provided to connect the DataFile to the provenance data.";
            throw new IllegalCommandException(error, this);
        }
        
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
         * 
         * The below chunk just validates that the input is basic json.
         */
        StringReader rdr = new StringReader(jsonInput);
        try {
            JsonObject jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            String error = "A valid JSON object could not be found. PROV-JSON format is expected.";
            throw new IllegalCommandException(error, this);
        }

        // Write to StorageIO.
        // "json.json" looks a little redundand but the standard is called PROV-JSON and it's nice to see .json on disk.
        final String provJsonExtension = "prov-json.json";
        try {
            StorageIO<DataFile> storageIO = dataFile.getStorageIO();
            InputStream inputStream = new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8.name()));
            storageIO.saveInputStreamAsAux(inputStream, provJsonExtension);
        } catch (IOException ex) {
            String error = "Exception caught persisting PROV-JSON: " + ex;
            throw new IllegalCommandException(error, this);
        }
        
        //FileMetadata fileMetadata = dataFile.getFileMetadata();
        //fileMetadata.setProvEntityName(entityName);
        
        dataFile.setProvEntityName(entityName);
        if(saveContext) {
            dataFile = ctxt.files().save(dataFile);
        }
        
        return dataFile;
    }

}
