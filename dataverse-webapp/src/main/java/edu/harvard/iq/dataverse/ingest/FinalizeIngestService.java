package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIOConstants;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestReport;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class FinalizeIngestService {
    private static final Logger logger = Logger.getLogger(FinalizeIngestService.class.getSimpleName());

    @Inject
    DataFileServiceBean fileService;

    private DataAccess dataAccess = DataAccess.dataAccess();

    /**
     * Writes the results of ingest into DB and storage.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean finalizeIngest(DataFile dataFile, File additionalData, TabularDataIngest tabDataIngest,
                                  File tabFile, IngestServiceBean.OriginalFileData originalFileData) {
        long dataFileId = dataFile.getId();

        boolean databaseSaveSuccessful = false;

        dataFile.setIngestDone();
        // delete the ingest request, if exists:
        if (dataFile.getIngestRequest() != null) {
            dataFile.getIngestRequest().setDataFile(null);
            dataFile.setIngestRequest(null);
        }
        try {
            dataFile = fileService.saveInNewTransaction(dataFile);
            databaseSaveSuccessful = true;

            logger.fine("Ingest (" + dataFile.getFileMetadata().getLabel() + ".");

            if (additionalData != null) {
                // remove the extra tempfile, if there was one:
                additionalData.delete();
            }
        } catch (Exception unknownEx) {
            // this means that an error occurred while saving the datafile
            // in the database.
            logger.log(Level.SEVERE, "Ingest Exception: ", unknownEx);
            logger.warning(
                    "Ingest failure: Failed to save tabular metadata (datatable, datavariables, etc.) in the database. Clearing the datafile object.");

            dataFile = fileService.find(dataFileId);

            if (dataFile != null) {
                dataFile.setIngestProblem();
                dataFile.setIngestReport(IngestReport.createIngestFailureReport(dataFile, IngestError.DB_FAIL));

                originalFileData.restoreIngestedDataFile(dataFile, tabDataIngest);

                dataFile = fileService.save(dataFile);
            }
        }

        if (!databaseSaveSuccessful) {
            logger.warning("Ingest failure (!databaseSaveSuccessful).");
            return false;
        }

        // Finally, let's swap the original and the tabular files:
        try {
            // Start of save as backup

            StorageIO<DataFile> tabularStorageIO = dataAccess.getStorageIO(dataFile);
            tabularStorageIO.open();

            // and we want to save the original of the ingested file:
            try {
                tabularStorageIO.backupAsAux(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);
                logger.fine("Saved the ingested original as a backup aux file: " + StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to save the ingested original!", ioe);
                throw ioe;
            }

            // Replace contents of the file with the tab-delimited data produced:
            tabularStorageIO.savePath(Paths.get(tabFile.getAbsolutePath()));

            // end of save as backup
        } catch (Exception e) {
            // this probably means that an error occurred while saving the file to the file system
            logger.log(Level.WARNING, "Failed to save the tabular file produced by the ingest (resetting the ingested DataFile back to its original state)", e);

            dataFile = fileService.find(dataFileId);

            if (dataFile != null) {
                dataFile.setIngestProblem();
                dataFile.setIngestReport(IngestReport.createIngestFailureReport(dataFile, IngestError.DB_FAIL));

                originalFileData.restoreIngestedDataFile(dataFile, tabDataIngest);

                fileService.save(dataFile);
                return false;
            }
        }
        return true;
    }
}
