package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.Iterator;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.DeleteDatasetDraft)
public class DeleteDatasetVersionCommand extends AbstractVoidCommand {

    private final Dataset doomed;

    public DeleteDatasetVersionCommand(User aUser, Dataset dataset) {
        super(aUser, dataset);
        this.doomed = dataset;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        // if you are deleting a dataset that only has 1 draft, we are actually destroying the dataset
        if (doomed.getVersions().size() == 1) {
            ctxt.engine().submit(new DestroyDatasetCommand(doomed, getUser()));
        } else {
            // we are only deleting a version
            // todo: for now, it's only the latest and if it's a draft
            // but we should add the ability to destroy a specific version
            DatasetVersion doomedVersion = doomed.getLatestVersion();
            if (doomedVersion.isDraft()) {
                Long versionId = doomedVersion.getId();

                // files
                Iterator<FileMetadata> fmIt = doomedVersion.getFileMetadatas().iterator();
                while (fmIt.hasNext()) {
                    FileMetadata fmd = fmIt.next();
                    if (!fmd.getDataFile().isReleased()) {
                        // if file is draft (ie. new to this version, delete
                        // and remove fileMetadata from list (so that it won't try to merge)
                        ctxt.engine().submit(new DeleteDataFileCommand(fmd.getDataFile(), getUser()));
                        fmIt.remove(); 
                    }
                }

                DatasetVersion doomedAndMerged = ctxt.em().merge(doomedVersion);
                ctxt.em().remove(doomedAndMerged);

                //remove version from ds obj before indexing....
                Iterator<DatasetVersion> dvIt = doomed.getVersions().iterator();
                while (dvIt.hasNext()) {
                    DatasetVersion dv = dvIt.next();
                    if (versionId.equals(dv.getId())) {
                        dvIt.remove();
                    }
                }
                boolean doNormalSolrDocCleanUp = true;
                ctxt.index().indexDataset(doomed, doNormalSolrDocCleanUp);
                return;
            }

            throw new IllegalCommandException("Cannot delete a released version", this);
        }
    }
}
