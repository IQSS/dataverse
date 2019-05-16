package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.util.EjbUtil;
import edu.harvard.iq.dataverse.util.FileTypeDetection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import javax.ejb.EJBException;

@RequiredPermissions(Permission.ManageDatasetPermissions)
public class RedetectFileTypeCommand extends AbstractCommand<DataFile> {

    private static final Logger logger = Logger.getLogger(RedetectFileTypeCommand.class.getCanonicalName());

    final DataFile fileToRedetect;
    final boolean dryRun;

    public RedetectFileTypeCommand(DataverseRequest dataveseRequest, DataFile dataFile, boolean dryRun) {
        super(dataveseRequest, dataFile);
        this.fileToRedetect = dataFile;
        this.dryRun = dryRun;
    }

    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {
        DataFile filetoReturn = null;
        Path path;
        try {
            // FIXME: Get this working with S3 and Swift.
            path = DataAccess.getStorageIO(fileToRedetect).getFileSystemPath();
            logger.fine("path: " + path);
            File file = path.toFile();
            String newlyDetectedContentType = FileTypeDetection.determineFileType(file);
            fileToRedetect.setContentType(newlyDetectedContentType);
        } catch (IOException ex) {
            throw new CommandException("Exception while attempting to get the bytes of the file during file type redetection: " + ex.getLocalizedMessage(), this);
        }
        filetoReturn = fileToRedetect;
        if (!dryRun) {
            try {
                filetoReturn = ctxt.files().save(fileToRedetect);
            } catch (EJBException ex) {
                throw new CommandException("Exception while attempting to save the new file type: " + EjbUtil.ejbExceptionToString(ex), this);
            }
            Dataset dataset = fileToRedetect.getOwner();
            try {
                boolean doNormalSolrDocCleanUp = true;
                ctxt.index().indexDataset(dataset, doNormalSolrDocCleanUp);
            } catch (Exception ex) {
                logger.info("Exception while reindexing files during file type redetection: " + ex.getLocalizedMessage());
            }
            try {
                ExportService instance = ExportService.getInstance(ctxt.settings());
                instance.exportAllFormats(dataset);
            } catch (ExportException ex) {
                // Just like with indexing, a failure to export is not a fatal condition.
                logger.info("Exception while exporting metadata files during file type redetection: " + ex.getLocalizedMessage());
            }
        }
        return filetoReturn;
    }

}
