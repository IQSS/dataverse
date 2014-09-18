/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionDatasetUser;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.engine.Permission;

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
@RequiredPermissions(Permission.DestructiveEdit)
public class DeleteDatasetVersionCommand extends AbstractVoidCommand {

    private final Dataset doomed;

    public DeleteDatasetVersionCommand(DataverseUser aUser, Dataset dataset) {
        super(aUser, dataset);
        this.doomed = dataset;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        if (doomed.isReleased()) {
            if (doomed.getLatestVersion().isDraft()) {
                DatasetVersion doomedVersion = doomed.getLatestVersion();
                Long versionId = doomedVersion.getId();
                // Users
                Iterator<DatasetVersionDatasetUser> duIt = doomedVersion.getDatasetVersionDataverseUsers().iterator();

                while (duIt.hasNext()) {
                    DatasetVersionDatasetUser dfn = duIt.next();
                    DatasetVersionDatasetUser doomedAndMerged = ctxt.em().merge(dfn);
                    ctxt.em().remove(doomedAndMerged);
                    duIt.remove();

                }

                // files
                Iterator <FileMetadata> fmIt = doomedVersion.getFileMetadatas().iterator();
                while (fmIt.hasNext()){
                    FileMetadata fmd = fmIt.next();
                    ctxt.engine().submit(new DeleteDataFileCommand(fmd.getDataFile(), getUser()));
                    fmIt.remove();
                }

                
                DatasetVersion doomedAndMerged = ctxt.em().merge(doomedVersion);
                ctxt.em().remove(doomedAndMerged);
                //remove version from ds obj before indexing....
                Iterator <DatasetVersion> dvIt = doomed.getVersions().iterator();
                while (dvIt.hasNext()){
                    DatasetVersion dv = dvIt.next();
                    if(versionId.equals(dv.getId())){
                          dvIt.remove();
                    }
                }
                ctxt.index().indexDataset(doomed);
                return;
            }
            throw new IllegalCommandException("Cannot delete a released version", this);
        }
    }
}
