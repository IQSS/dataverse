package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

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
    };

    public UpdateDatasetThumbnailCommand(DataverseRequest aRequest, Dataset theDataset, UserIntent theUserIntent, Long theDataFileIdSupplied, InputStream theInputStream) {
        super(aRequest, theDataset);
        dataset = theDataset;
        userIntent = theUserIntent;
        inputStream = theInputStream;
        this.dataFileIdSupplied = theDataFileIdSupplied;
    }

    @Override
    public DatasetThumbnail execute(CommandContext ctxt) throws CommandException {
        if (dataset == null) {
            String message = "Can't update dataset thumbnail. Dataset is null.";
            logger.info(message);
            throw new IllegalCommandException(message, this);
        }
//        if (true) {
//            throw new CommandException("Just testing what an error would look like in the GUI.", this);
//        }
        if (userIntent == null) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.noChange"), this);
        }
        switch (userIntent) {

            case setDatasetFileAsThumbnail:
                if (dataFileIdSupplied == null) {
                    throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.fileNotSupplied"), this);
                }
                DataFile datasetFileThumbnailToSwitchTo = ctxt.files().find(dataFileIdSupplied);
                if (datasetFileThumbnailToSwitchTo == null) {
                    throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.fileNotFound",
                            List.of(dataFileIdSupplied.toString())), this);
                }
                Dataset ds1 = ctxt.datasets().setDatasetFileAsThumbnail(dataset, datasetFileThumbnailToSwitchTo);
                DatasetThumbnail datasetThumbnail = ds1.getDatasetThumbnail(ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                if (datasetThumbnail != null) {
                    DataFile dataFile = datasetThumbnail.getDataFile();
                    if (dataFile != null) {
                        if (dataFile.getId().equals(dataFileIdSupplied)) {
                            return datasetThumbnail;
                        } else {
                            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.basedOnWrongFileId",
                                    List.of(String.valueOf(dataFile.getId()),String.valueOf(dataFileIdSupplied))), this);
                        }
                    }
                } else {
                    throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.missing"), this);
                }

            case setNonDatasetFileAsThumbnail:
                File uploadedFile;
                try {
                    uploadedFile = FileUtil.inputStreamToFile(inputStream);
                } catch (IOException ex) {
                    throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.inputStreamToFile.exception", List.of(ex.getMessage())), this);
                }
                if (uploadedFile == null) {
                    throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.nonDatasetsFileIsNull"), this);
                }
                long uploadLogoSizeLimit = ctxt.systemConfig().getUploadLogoSizeLimit();
                if (uploadedFile.length() > uploadLogoSizeLimit) {
                    throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.fileToLarge", List.of(String.valueOf(uploadLogoSizeLimit))), this);
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
                    DatasetThumbnail thumbnail = datasetWithNewThumbnail.getDatasetThumbnail(ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                    if (thumbnail != null) {
                        return thumbnail;
                    }
                }
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.nonDatasetFailed"), this);

            case removeThumbnail:
                Dataset ds2 = ctxt.datasets().clearDatasetLevelThumbnail(dataset);
                DatasetThumbnail datasetThumbnail2 = ds2.getDatasetThumbnail(ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                if (datasetThumbnail2 == null) {
                    return null;
                } else {
                    throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.notDeleted"), this);
                }
            default:
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.thumbnail.actionNotSupported"), this);
        }
    }

}
