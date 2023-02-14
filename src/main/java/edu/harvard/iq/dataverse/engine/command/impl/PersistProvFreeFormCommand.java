package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class PersistProvFreeFormCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(PersistProvFreeFormCommand.class.getCanonicalName());

    private final DataFile dataFile;
    private final String userInput;

    public PersistProvFreeFormCommand(DataverseRequest aRequest, DataFile dataFile, String userInput) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.userInput = userInput;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {
        if (dataFile.getOwner() == null){
            // this is a new file through upload, set freeform
            dataFile.getFileMetadata().setProvFreeForm(userInput);
            return dataFile;
        }
        else {
            Dataset dataset = dataFile.getOwner();
            DatasetVersion workingVersion = dataset.getOrCreateEditVersion();

            if (workingVersion.isDraft()) { 
                if (dataset.isReleased()){
                    for (FileMetadata fmw : workingVersion.getFileMetadatas()) {
                        if (dataFile.equals(fmw.getDataFile())) {
                            fmw.setProvFreeForm(userInput);
                        }
                    }
                } else {
                    dataFile.getFileMetadata().setProvFreeForm(userInput);
                }
                
                return dataFile;
            } else {
                throw new CommandExecutionException("Exception occurred creating draft dataset", this);
            }
        
        }
    }

}
