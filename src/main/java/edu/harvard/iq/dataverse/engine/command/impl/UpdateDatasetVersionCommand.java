package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * Updates a {@link DatasetVersion}, as long as that version is in a "draft" state.
 * @author michael
 */
@RequiredPermissions(Permission.DestructiveEdit)
public class UpdateDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    
    final DatasetVersion newVersion;
    
    public UpdateDatasetVersionCommand(DataverseUser aUser, DatasetVersion theNewVersion) {
        super(aUser, theNewVersion.getDataset());
        newVersion = theNewVersion;
    }
    
    
    
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        
        Dataset ds = newVersion.getDataset();
        DatasetVersion latest = ds.getLatestVersion();
        
        if ( latest == null ) {
            throw new IllegalCommandException("Dataset " + ds.getId() + " does not have a latest version.", this);
        }
            
        if ( ! latest.isDraft() ) {
            throw new IllegalCommandException("Cannot update a dataset version that's not a draft", this);
        }
        
        DatasetVersion edit = ds.getEditVersion();
        edit.setDatasetFields( newVersion.getDatasetFields() );
        
        DatasetVersion managed = ctxt.em().merge(edit);
        
        ctxt.index().indexDataset(ds);
        
        return managed;
    }
    
}
