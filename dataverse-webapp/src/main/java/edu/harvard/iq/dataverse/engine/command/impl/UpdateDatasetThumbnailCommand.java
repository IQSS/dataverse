package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetThumbnailCommand extends AbstractCommand<DatasetThumbnail> {

    private static final Logger logger = Logger.getLogger(UpdateDatasetThumbnailCommand.class.getCanonicalName());

    private final Dataset dataset;
    private final UserIntent userIntent;
    /**
     * @todo make this a long rather than a Long.
     */
    private final Long dataFileIdSupplied;
    private final InputStream inputStream;

    public enum UserIntent {
        setDatasetFileAsThumbnail,
        setNonDatasetFileAsThumbnail,
        removeThumbnail
    }

    public UpdateDatasetThumbnailCommand(DataverseRequest aRequest, Dataset theDataset, UserIntent theUserIntent, Long theDataFileIdSupplied, InputStream theInputStream) {
        super(aRequest, theDataset);
        dataset = theDataset;
        userIntent = theUserIntent;
        inputStream = theInputStream;
        this.dataFileIdSupplied = theDataFileIdSupplied;
    }

    @Override
    public DatasetThumbnail execute(CommandContext ctxt) {
        if (dataset == null) {
            String message = "Can't update dataset thumbnail. Dataset is null.";
            logger.info(message);
            throw new IllegalCommandException(message, this);
        }
//        if (true) {
//            throw new CommandException("Just testing what an error would look like in the GUI.", this);
//        }
        if (userIntent == null) {
            throw new IllegalCommandException("No changes to save.", this);
        }
        switch (userIntent) {

            case setDatasetFileAsThumbnail:
                if (dataFileIdSupplied == null) {
                    throw new CommandException("A file was not selected to be the new dataset thumbnail.", this);
                }
                DataFile datasetFileThumbnailToSwitchTo = ctxt.files().find(dataFileIdSupplied);
                if (datasetFileThumbnailToSwitchTo == null) {
                    throw new CommandException("Could not find file based on id supplied: " + dataFileIdSupplied + ".", this);
                }
                Dataset ds1 = ctxt.datasets().setDatasetFileAsThumbnail(dataset, datasetFileThumbnailToSwitchTo);
                DatasetThumbnail datasetThumbnail = DatasetUtil.getThumbnail(ds1);
                if (datasetThumbnail != null) {
                    DataFile dataFile = datasetThumbnail.getDataFile();
                    if (dataFile != null) {
                        if (dataFile.getId().equals(dataFileIdSupplied)) {
                            return datasetThumbnail;
                        } else {
                            throw new CommandException("Dataset thumbnail is should be based on file id " + dataFile.getId() + " but instead it is " + dataFileIdSupplied + ".", this);
                        }
                    }
                } else {
                    throw new CommandException("Dataset thumbnail is unexpectedly absent.", this);
                }

            case setNonDatasetFileAsThumbnail:
                File uploadedFile;
                try {
                    uploadedFile = FileUtil.inputStreamToFile(inputStream);
                } catch (IOException ex) {
                    throw new CommandException("In setNonDatasetFileAsThumbnail caught exception calling inputStreamToFile: " + ex, this);
                }
                if (uploadedFile == null) {
                    throw new CommandException("In setNonDatasetFileAsThumbnail uploadedFile was null.", this);
                }
                long uploadLogoSizeLimit = ctxt.systemConfig().getUploadLogoSizeLimit();
                if (uploadedFile.length() > uploadLogoSizeLimit) {
                    throw new IllegalCommandException("File is larger than maximum size: " + uploadLogoSizeLimit + ".", this);
                }
                FileInputStream fileAsStream = null;
                try {
                    fileAsStream = new FileInputStream(uploadedFile);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(UpdateDatasetThumbnailCommand.class.getName()).log(Level.SEVERE, null, ex);
                }
                Dataset datasetWithNewThumbnail = ctxt.datasets().setNonDatasetFileAsThumbnail(dataset, fileAsStream);
                IOUtils.closeQuietly(fileAsStream);
                if (datasetWithNewThumbnail != null) {
                    return DatasetUtil.getThumbnail(datasetWithNewThumbnail);
                } else {
                    return null;
                }

            case removeThumbnail:
                Dataset ds2 = ctxt.datasets().removeDatasetThumbnail(dataset);
                DatasetThumbnail datasetThumbnail2 = DatasetUtil.getThumbnail(ds2);
                if (datasetThumbnail2 == null) {
                    return null;
                } else {
                    throw new CommandException("User wanted to remove the thumbnail it still has one!", this);
                }
            default:
                throw new IllegalCommandException("Whatever you are trying to do to the dataset thumbnail is not supported.", this);
        }
    }

}
