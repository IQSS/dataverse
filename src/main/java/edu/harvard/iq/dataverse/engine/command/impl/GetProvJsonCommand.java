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
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@RequiredPermissions(Permission.EditDataset)
public class GetProvJsonCommand extends AbstractCommand<JsonObject> {

    private static final Logger logger = Logger.getLogger(GetProvJsonCommand.class.getCanonicalName());

    private final DataFile dataFile;

    public GetProvJsonCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public JsonObject execute(CommandContext ctxt) throws CommandException {

        final String provJsonExtension = "prov-json.json";

        try {
            StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
            InputStream inputStream = dataAccess.getAuxFileAsInputStream(provJsonExtension);
            JsonObject jsonObject = null;
            if(null != inputStream) {
                JsonReader jsonReader = Json.createReader(inputStream);
                jsonObject = jsonReader.readObject();
            }
            return jsonObject;
        } catch (IOException ex) {
            String error = "Exception caught in DataAccess.getStorageIO(dataFile) getting file. Error: " + ex;
            throw new IllegalCommandException(error, this);
        }
    }
}
