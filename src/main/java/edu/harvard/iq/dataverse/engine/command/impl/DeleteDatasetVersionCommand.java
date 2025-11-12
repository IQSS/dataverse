package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.DeleteDatasetDraft)
public class DeleteDatasetVersionCommand extends AbstractVoidCommand {

    private static final Logger logger = Logger.getLogger(DeleteDatasetVersionCommand.class.getCanonicalName());

    private Dataset doomed;

    public DeleteDatasetVersionCommand(DataverseRequest aRequest, Dataset dataset) {
        super(aRequest, dataset);
        this.doomed = dataset;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.permissions().checkEditDatasetLock(doomed, getRequest(), this);
        doomed = ctxt.em().find(Dataset.class, doomed.getId());
        // if you are deleting a dataset that only has 1 version, we are actually destroying the dataset
        if (doomed.getVersions().size() == 1) {
            ctxt.engine().submit(new DestroyDatasetCommand(doomed, getRequest()));
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
                        ctxt.engine().submit(new DeleteDataFileCommand(fmd.getDataFile(), getRequest()));
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
                /**
                 * DeleteDatasetDraft, which is required by this command,
                 * DeleteDatasetVersionCommand is not sufficient for running
                 * GetPrivateUrlCommand nor DeletePrivateUrlCommand, both of
                 * which require ManageDatasetPermissions because
                 * DeletePrivateUrlCommand calls RevokeRoleCommand which
                 * requires ManageDatasetPermissions when executed on a dataset
                 * so we make direct calls to the service bean so that a lowly
                 * Contributor who does NOT have ManageDatasetPermissions can
                 * still successfully delete a Private URL.
                 */
                PrivateUrl privateUrl = ctxt.privateUrl().getPrivateUrlFromDatasetId(doomed.getId());
                if (privateUrl != null) {
                    logger.fine("Deleting Private URL for dataset id " + doomed.getId());
                    PrivateUrlUser privateUrlUser = new PrivateUrlUser(doomed.getId());
                    List<RoleAssignment> roleAssignments = ctxt.roles().directRoleAssignments(privateUrlUser, doomed);
                    for (RoleAssignment roleAssignment : roleAssignments) {
                        ctxt.roles().revoke(roleAssignment, getRequest());
                    }
                }
                boolean doNormalSolrDocCleanUp = true;
                ctxt.index().asyncIndexDataset(doomed, doNormalSolrDocCleanUp);

                return;
            }

            throw new IllegalCommandException("Cannot delete a released version", this);
        }
    }
}
