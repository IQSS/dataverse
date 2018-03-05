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
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class DeleteProvJsonProvCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(PersistProvJsonProvCommand.class.getCanonicalName());

    private DataFile dataFile;
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
    public DataFile execute(CommandContext ctxt) throws CommandException {
        
        final String provJsonExtension = "prov-json.json";

        try {
            StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
            dataAccess.deleteAuxObject(provJsonExtension);
            logger.info("provenance json delete passed io step");
        } catch (NoSuchFileException nf) {
            //if this command is called and there is no file, we keep going
        } catch (IOException ex) {
            String error = "Exception caught deleting provenance aux object: " + ex;
            throw new IllegalCommandException(error, this);
        }
        
        FileMetadata fileMetadata = dataFile.getFileMetadata();
        fileMetadata.setProvJsonObjName("");
        if(saveContext) {
            dataFile = ctxt.files().save(dataFile);
        }
        return dataFile;
    }

}
