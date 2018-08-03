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
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;


@RequiredPermissions(Permission.EditDataset)
public class PersistProvJsonCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(PersistProvJsonCommand.class.getCanonicalName());

    private DataFile dataFile;
    private final String jsonInput;
    private final String entityName;
    private final boolean saveContext;
    
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

        // Write to StorageIO.
        // "prov-json.json" looks a little redundand but the standard is called PROV-JSON and it's nice to see .json on disk.
        final String provJsonExtension = "prov-json.json";
        try {
            StorageIO<DataFile> storageIO = dataFile.getStorageIO();
            InputStream inputStream = new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8.name()));
            storageIO.saveInputStreamAsAux(inputStream, provJsonExtension);
        } catch (IOException ex) {
            String error = "Exception caught persisting PROV-JSON: " + ex;
            throw new IllegalCommandException(error, this);
        }
        
        dataFile.setProvEntityName(entityName);
        if(saveContext) {
            dataFile = ctxt.files().save(dataFile);
        }
        
        return dataFile;
    }

}
