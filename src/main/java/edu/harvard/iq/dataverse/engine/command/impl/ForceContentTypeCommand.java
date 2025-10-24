package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.util.EjbUtil;
import io.gdcc.spi.export.ExportException;
import jakarta.ejb.EJBException;

import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class ForceContentTypeCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(ForceContentTypeCommand.class.getCanonicalName());

    final DataFile fileToRedetect;
    final boolean dryRun;
    final String contentType;

    public ForceContentTypeCommand(DataverseRequest dataveseRequest, DataFile dataFile, boolean dryRun, String contentType) {
        super(dataveseRequest, dataFile);
        this.fileToRedetect = dataFile;
        this.dryRun = dryRun;
        this.contentType = contentType;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {
        DataFile filetoReturn = null;

        try {
            logger.fine("Setting new contentType for target file: " + fileToRedetect.getDisplayName());
            fileToRedetect.setContentType(contentType);
        } catch (Exception ex) {
            throw new CommandException("Exception while enforcing the new contentType of a file: " + ex.getLocalizedMessage(), this);
        }
        
        
        filetoReturn = fileToRedetect;
        if (!dryRun) {
            try {
                filetoReturn = ctxt.files().save(fileToRedetect);
            } catch (EJBException ex) {
                throw new CommandException("Exception while attempting to save the new file type: " + EjbUtil.ejbExceptionToString(ex), this);
            }
            Dataset dataset = fileToRedetect.getOwner();
            boolean doNormalSolrDocCleanUp = true;
            ctxt.index().asyncIndexDataset(dataset, doNormalSolrDocCleanUp);
            try {
                ExportService instance = ExportService.getInstance();
                instance.exportAllFormats(dataset);
            } catch (ExportException ex) {
                // Just like with indexing, a failure to export is not a fatal condition.
                logger.info("Exception while exporting metadata files during file type redetection: " + ex.getLocalizedMessage());
            }
        }
        return filetoReturn;
    }

}