package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class DeleteProvFreeFormCommand extends AbstractCommand<Boolean> {

    private static final Logger logger = Logger.getLogger(DeleteProvFreeFormCommand.class.getCanonicalName());

    private final DataFile dataFile;

    public DeleteProvFreeFormCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public Boolean execute(CommandContext ctxt) throws CommandException {
        FileMetadata fileMetadata = dataFile.getFileMetadata();
        fileMetadata.setProvFreeForm(null);
        DataFile savedDataFile = ctxt.files().save(dataFile);
        logger.info("prov free-form: " + savedDataFile.getFileMetadata().getProvFreeForm());
        return savedDataFile.getFileMetadata().getProvFreeForm() == null;
    }

}
