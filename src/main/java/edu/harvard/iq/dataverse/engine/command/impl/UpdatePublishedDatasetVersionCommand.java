package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * Updates the specified dataset version, which must already be published/not
 * draft. The command is only usable by superusers. The initial use is to allow
 * adding version notes to existing versions but it is otherwise generic
 * assuming there may be other cases where updating some aspect of an existing
 * version is needed. Note this is similar to the
 * CuratePublishedDatasetVersionCommand, but in that case changes in an existing
 * draft version are pushed into the latest published version as a form of
 * republishing (and the draft version ceases to exist). This command assumes
 * changes have been made to the existing dataset version of interest, which may
 * not be the latest published one, and it does not make any changes to a
 * dataset's draft version if that exists.
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdatePublishedDatasetVersionCommand extends AbstractCommand<DatasetVersion> {

    private final DatasetVersion datasetVersion;

    public UpdatePublishedDatasetVersionCommand(DataverseRequest aRequest, DatasetVersion datasetVersion) {
        super(aRequest, datasetVersion.getDataset());
        this.datasetVersion = datasetVersion;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        // Check if the user is a superuser
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException("Only superusers can update published dataset versions", this);
        }

        // Ensure the version is published
        if (!datasetVersion.isReleased()) {
            throw new IllegalCommandException("This command can only be used on published dataset versions", this);
        }

        // Save the changes
        DatasetVersion savedVersion = ctxt.em().merge(datasetVersion);

        return savedVersion;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        DatasetVersion version = (DatasetVersion) r;
        // Only need to reindex if this version is the latest published version for the
        // dataset
        if (version.equals(version.getDataset().getLatestVersionForCopy())) {
            ctxt.index().asyncIndexDataset(version.getDataset(), true);
        }
        return true;
    }
}
