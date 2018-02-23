package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
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
public class DeleteProvJsonProvCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(PersistProvJsonProvCommand.class.getCanonicalName());

    private final DataFile dataFile;
    private final boolean saveContext;

    
    public DeleteProvJsonProvCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.saveContext = false;
    }
    
    public DeleteProvJsonProvCommand(DataverseRequest aRequest, DataFile dataFile, boolean saveContext) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.saveContext = saveContext;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {

        FileMetadata fileMetadata = dataFile.getFileMetadata();
        fileMetadata.setProvJsonObjName("");
        if(saveContext) {
            ctxt.files().save(dataFile); //MAD BAD 
        }
        
        
        final String provJsonExtension = "prov-json.json";

        try {
            StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
            dataAccess.deleteAuxObject(provJsonExtension);
            logger.info("provenance json delete passed io step");
        } catch (IOException ex) {
            String error = "Exception caught deleting provenance aux object: " + ex;
            throw new IllegalCommandException(error, this);
        }
    }

}
