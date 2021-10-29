package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.dataset.difference.DatasetVersionDifference;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(UpdateDatasetVersionCommand.class.getCanonicalName());
    private final List<DataFile> dataFilesToDelete;
    private boolean validateLenient = false;
    private final DatasetVersion clone;

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        this(theDataset, aRequest, new ArrayList<>());
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, List<DataFile> filesToDelete) {
        this(theDataset, aRequest, filesToDelete, null);
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, DatasetVersion clone) {
        this(theDataset, aRequest, new ArrayList<>(), clone);
    }

    public UpdateDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest, List<DataFile> filesToDelete, DatasetVersion clone) {
        super(aRequest, theDataset);
        this.clone = clone;
        this.dataFilesToDelete = filesToDelete;
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    public void setValidateLenient(boolean validateLenient) {
        this.validateLenient = validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt)  {
        if (!(getUser() instanceof AuthenticatedUser)) {
            throw new IllegalCommandException("Only authenticated users can update datasets", this);
        }

        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks prventing the update

        getDataset().getEditVersion().setDatasetFields(getDataset().getEditVersion().initDatasetFields());
        validateOrDie(getDataset().getEditVersion(), isValidateLenient(), ctxt);

        final DatasetVersion editVersion = getDataset().getEditVersion();
        tidyUpFields(editVersion);

        // Merge the new version into out JPA context, if needed.
        if (editVersion.isNew()) {
            ctxt.em().persist(editVersion);
        } else {
            ctxt.em().merge(editVersion);
        }

        for (DataFile dataFile : getDataset().getFiles()) {
            if (dataFile.getCreateDate() == null) {
                dataFile.setCreateDate(getTimestamp());
                dataFile.setCreator((AuthenticatedUser) getUser());
            }
            dataFile.setModificationTime(getTimestamp());
        }

        // Remove / delete any files that were removed

        // If any of the files that we are deleting has a UNF, we will need to
        // re-calculate the UNF of the version - since that is the product
        // of the UNFs of the individual files.
        boolean recalculateUNF = false;
        /* The separate loop is just to make sure that the dataset database is
        updated, specifically when an image datafile is being deleted, which
        is being used as the dataset thumbnail as part of a batch delete.
        if we don't remove the thumbnail association with the dataset before the
        actual deletion of the file, it might throw foreign key integration
        violation exceptions.
        */
        for (DataFile df : dataFilesToDelete) {
            //  check if this file is being used as the default thumbnail
            if (df.equals(getDataset().getThumbnailFile())) {
                logger.fine("deleting the dataset thumbnail designation");
                getDataset().setThumbnailFile(null);
            }

            if (df.getUnf() != null) {
                recalculateUNF = true;
            }
        }
        // we have to merge to update the database but not flush because
        // we don't want to create two draft versions!
        Dataset tempDataset = ctxt.em().merge(getDataset());

        List<DataFile> managedDataFilesToDelete = collectDatafileFromDataset(tempDataset, dataFilesToDelete);
        Map<Long, FileMetadata> fileMetadatasToDelete = collectFileMetadataFromVersion(tempDataset.getEditVersion(), managedDataFilesToDelete);

        for (DataFile dataFileToDelete : managedDataFilesToDelete) {
            FileMetadata fileMetadataInEditVersion = fileMetadatasToDelete.get(dataFileToDelete.getId());

            if (!dataFileToDelete.isReleased()) {
                // if file is draft (ie. new to this version, delete; otherwise just remove filemetadata object)
                ctxt.engine().submit(new DeleteDataFileCommand(dataFileToDelete, getRequest()));
                tempDataset.getFiles().remove(dataFileToDelete);
                tempDataset.getEditVersion().getFileMetadatas()
                    .removeIf(fileMetadata -> fileMetadata.getDataFile().equals(dataFileToDelete));
                // added this check to handle issue where you could not deleter a file that shared a category with a new file
                // the relation ship does not seem to cascade, yet somehow it was trying to merge the filemetadata
                // todo: clean this up some when we clean the create / update dataset methods
                for (DataFileCategory cat : tempDataset.getCategories()) {
                    cat.getFileMetadatas().removeIf(fileMetadata -> fileMetadata.getDataFile().equals(dataFileToDelete));
                }
            } else {
                if (fileMetadataInEditVersion != null) {
                    dataFileToDelete.getFileMetadatas().remove(fileMetadataInEditVersion);
                }

                tempDataset.getEditVersion().getFileMetadatas().
                        removeIf(fileMetadata -> fileMetadata.getDataFile().equals(dataFileToDelete));
            }
        }

        if (recalculateUNF) {
            ctxt.ingest().recalculateDatasetVersionUNF(tempDataset.getEditVersion());
        }

        tempDataset.getEditVersion().setLastUpdateTime(getTimestamp());
        tempDataset.setModificationTime(getTimestamp());

        Dataset savedDataset = ctxt.em().merge(tempDataset);
        ctxt.em().flush();

        updateDatasetUser(ctxt);
        ctxt.index().indexDataset(savedDataset, true);
        if (clone != null) {
            DatasetVersionDifference dvd = new DatasetVersionDifference(savedDataset.getEditVersion(), clone);
            AuthenticatedUser au = (AuthenticatedUser) getUser();
            ctxt.datasetVersion().writeEditVersionLog(dvd, au);
        }
        return savedDataset;
    }

    private List<DataFile> collectDatafileFromDataset(Dataset dataset, List<DataFile> dataFiles) {
        return dataset.getFiles().stream()
                .filter(df -> dataFiles.contains(df))
                .collect(toList());
    }
    private Map<Long, FileMetadata> collectFileMetadataFromVersion(DatasetVersion version, List<DataFile> dataFiles) {
        return version.getFileMetadatas().stream()
                .filter(fm -> dataFiles.contains(fm.getDataFile()))
                .collect(toMap(fm -> fm.getDataFile().getId(), Function.identity()));

    }
}
