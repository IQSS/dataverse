package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.util.EjbUtil;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;
import javax.ejb.EJBException;

@RequiredPermissions(Permission.EditDataset)
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
        File tempFile = null; 
        File localFile;

        
        try {
            StorageIO<DataFile> storageIO;

            storageIO = fileToRedetect.getStorageIO();
            storageIO.open();

            if (storageIO.isLocalFile()) {
                localFile = storageIO.getFileSystemPath().toFile();
            } else {
                // Need to create a temporary local file: 

                tempFile = File.createTempFile("tempFileTypeCheck", ".tmp");
                try (ReadableByteChannel targetFileChannel = (ReadableByteChannel) storageIO.getReadChannel();
                		FileChannel tempFileChannel = new FileOutputStream(tempFile).getChannel();) {
                    tempFileChannel.transferFrom(targetFileChannel, 0, storageIO.getSize());
                }
                localFile = tempFile;
            }

            logger.fine("target file: " + localFile);
            String newlyDetectedContentType = FileUtil.determineFileType(localFile, fileToRedetect.getDisplayName());
            fileToRedetect.setContentType(newlyDetectedContentType);
        } catch (IOException ex) {
            throw new CommandException("Exception while attempting to get the bytes of the file during file type redetection: " + ex.getLocalizedMessage(), this);
        } finally {
            // If we had to create a temp file, delete it now:
            if (tempFile != null) {
                tempFile.delete();
            }
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
