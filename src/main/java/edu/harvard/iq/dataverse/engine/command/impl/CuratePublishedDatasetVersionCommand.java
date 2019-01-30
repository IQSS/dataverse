package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author qqmyers
 * 
 *         Adapted from UpdateDatasetVersionCommand
 */
@RequiredPermissions(Permission.EditDataset)
public class CuratePublishedDatasetVersionCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(CuratePublishedDatasetVersionCommand.class.getCanonicalName());
    final private boolean validateLenient = true;

    public CuratePublishedDatasetVersionCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset);
    }

    public boolean isValidateLenient() {
        return validateLenient;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException("Only superusers can curate published dataset versions", this);
        }

        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);
        // Invariant: Dataset has no locks preventing the update
        DatasetVersion updateVersion = getDataset().getLatestVersionForCopy();

        // Copy metadata from draft version to latest published version
        updateVersion.setDatasetFields(getDataset().getEditVersion().initDatasetFields());

        validateOrDie(updateVersion, isValidateLenient());

        // final DatasetVersion editVersion = getDataset().getEditVersion();
        tidyUpFields(updateVersion);

        // Merge the new version into out JPA context
        ctxt.em().merge(updateVersion);

        for (DataFile dataFile : getDataset().getFiles()) {
            List<FileMetadata> fmdList = dataFile.getFileMetadatas();
            FileMetadata draftFmd = dataFile.getLatestFileMetadata();
            FileMetadata publishedFmd = null;
            for (FileMetadata fmd : fmdList) {
                if (fmd.getDatasetVersion().equals(getDataset().getEditVersion())) {
                    publishedFmd = fmd;
                }
            }
            boolean metadataUpdated = false;
            if (draftFmd != null && publishedFmd != null) {
                if (!draftFmd.getLabel().equals(publishedFmd.getLabel())) {
                    publishedFmd.setLabel(draftFmd.getLabel());
                    metadataUpdated = true;
                }
                if (!draftFmd.getCategories().equals(publishedFmd.getCategories())) {
                    publishedFmd.setCategories(draftFmd.getCategories());
                    metadataUpdated = true;
                }
                if (!draftFmd.isRestricted() == publishedFmd.isRestricted()) {
                    publishedFmd.setRestricted(draftFmd.isRestricted());
                    metadataUpdated = true;
                }
                if (!draftFmd.getProvFreeForm().equals(publishedFmd.getProvFreeForm())) {
                    publishedFmd.setProvFreeForm(draftFmd.getProvFreeForm());
                    metadataUpdated = true;
                }

            } else {
                throw new IllegalCommandException("Cannot change files in the dataset", this);
            }
            if (metadataUpdated) {
                dataFile.setModificationTime(getTimestamp());
            }
            // Now delete filemetadata in draft version before deleting the version itself
            FileMetadata mergedFmd = ctxt.em().merge(draftFmd);
            ctxt.em().remove(mergedFmd);
            dataFile.getFileMetadatas().remove(draftFmd);
            getDataset().getEditVersion().getFileMetadatas().remove(draftFmd);

            for (DataFileCategory cat : getDataset().getCategories()) {
                cat.getFileMetadatas().remove(draftFmd);
            }
        }

        // we have to merge to update the database but not flush because
        // we don't want to create two draft versions!
        Dataset tempDataset = ctxt.em().merge(getDataset());

        tempDataset.getEditVersion().setLastUpdateTime(getTimestamp());
        tempDataset.setModificationTime(getTimestamp());
        Dataset savedDataset = ctxt.em().merge(tempDataset);

        ctxt.em().flush();

        // Now delete draft version
        DeleteDatasetVersionCommand cmd;

        cmd = new DeleteDatasetVersionCommand(getRequest(), savedDataset);
        ctxt.engine().submit(cmd);
        // And update metadata at PID provider
        ctxt.engine().submit(
                new UpdateDvObjectPIDMetadataCommand(savedDataset, getRequest()));

        updateDatasetUser(ctxt);
        ctxt.index().indexDataset(savedDataset, true);

        return savedDataset;
    }

}
