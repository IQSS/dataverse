/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "", value = Permission.Publish)
})
public class PublishDatasetCommand extends AbstractCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(PublishDatasetCommand.class.getCanonicalName());
    boolean minorRelease = false;
    Dataset theDataset;

    public PublishDatasetCommand(Dataset datasetIn, User user, boolean minor) {
        super(user, datasetIn);
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        if (!theDataset.getOwner().isReleased()) {
            throw new IllegalCommandException("This dataset may not be published because its host dataverse (" + theDataset.getOwner().getAlias() + ") has not been published.", this);
        }

        if (theDataset.getLatestVersion().isReleased()) {
            throw new IllegalCommandException("Latest version of dataset " + theDataset.getIdentifier() + " is already released. Only draft versions can be released.", this);
        }

        if (minorRelease && !theDataset.getLatestVersion().isMinorUpdate()) {
            throw new IllegalCommandException("Cannot release as minor version. Re-try as major release.", this);
        }

        if (theDataset.getPublicationDate() == null) {
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
            theDataset.setReleaseUserIdentifier(getUser().getIdentifier());
            if (!minorRelease) {
                theDataset.getEditVersion().setVersionNumber(new Long(1));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            } else {
                theDataset.getEditVersion().setVersionNumber(new Long(0));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(1));
            }
        } else {
            if (!minorRelease) {
                theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber().intValue() + 1));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            } else {
                theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber().intValue()));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber().intValue() + 1));
            }
        }
        
        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setReleaseTime(updateTime);
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.getEditVersion().setVersionState(DatasetVersion.VersionState.RELEASED);

        for (DataFile dataFile : theDataset.getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                dataFile.setPublicationDate(updateTime);
            }
        }

        Dataset savedDataset = ctxt.em().merge(theDataset);

        ctxt.index().indexDataset(savedDataset);

        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionDatasetUser(savedDataset.getLatestVersion(), this.getUser());

        if (ddu != null) {
            ddu.setLastUpdateDate(updateTime);
            ctxt.em().merge(ddu);
        } else {
            DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
            datasetDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
            datasetDataverseUser.setLastUpdateDate((Timestamp) updateTime);
            datasetDataverseUser.setDatasetversionid(savedDataset.getLatestVersion().getId());
            datasetDataverseUser.setUserIdentifier(getUser().getIdentifier());
            ctxt.em().merge(datasetDataverseUser);
        }

        return savedDataset;
    }

}
