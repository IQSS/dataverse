package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.logging.Logger;


@RequiredPermissions(Permission.EditDataset)
/**
 * This command gets the freeform provenance input
 */
public class GetProvFreeFormCommand extends AbstractCommand<String> {

    private static final Logger logger = Logger.getLogger(GetProvFreeFormCommand.class.getCanonicalName());

    private final DataFile dataFile;

    public GetProvFreeFormCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public String execute(CommandContext ctxt) throws CommandException {
        FileMetadata fileMetadata = dataFile.getFileMetadata();

        //logger.info("prov free-form: " + fileMetadata.getProvFreeForm());
        return fileMetadata.getProvFreeForm();
    }

}
