package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.AddDatasetVersion )
public class CreateDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    
    public enum BumpWhat {
        BumpMinor,
        BumpMajor
    }
    final BumpWhat whatToBump;
    final DatasetVersion newVersion;
    final Dataset dataset;
    
    public CreateDatasetVersionCommand(DataverseUser aUser, Dataset theDataset, DatasetVersion aVersion, BumpWhat bump) {
        super(aUser, theDataset);
        dataset = theDataset;
        newVersion = aVersion;
        whatToBump = bump;
    }
    
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        DatasetVersion latest = dataset.getLatestVersion();
        switch ( whatToBump ) {
            case BumpMajor:
                newVersion.setMinorVersionNumber(0l);
                newVersion.setVersionNumber( latest.getVersionNumber() + 1l );
                break;
            case BumpMinor:
                newVersion.setMinorVersionNumber( latest.getMinorVersionNumber() + 1l);
                newVersion.setVersionNumber( latest.getVersionNumber());
                break;
        }
        
        newVersion.setDataset(dataset);
        ctxt.em().persist(newVersion);

        // TODO make async
        ctxt.index().indexDataset(dataset);
        
        return newVersion;
    }
    
}
