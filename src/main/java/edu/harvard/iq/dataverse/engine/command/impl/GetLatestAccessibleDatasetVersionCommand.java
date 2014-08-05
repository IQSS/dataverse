/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.Access )
public class GetLatestAccessibleDatasetVersionCommand extends AbstractCommand<DatasetVersion>{
    private final Dataset ds;
    private final DataverseUser u;

    public GetLatestAccessibleDatasetVersionCommand(DataverseUser aUser, Dataset anAffectedDataset) {
        super(aUser, anAffectedDataset);
        u = aUser;
        ds = anAffectedDataset;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        DatasetVersion d = null;
        
        try {
            d = ctxt.engine().submit(new GetDraftDatasetVersionCommand(u, ds));
        } catch(PermissionException ex) {
            
        }
        
        if (d == null || d.getId() == null) {
            d = ctxt.engine().submit(new GetLatestPublishedDatasetVersionCommand(u,ds));
        }
    
        
        
    /*    return (published) ?
            ds.getReleasedVersion() : ds.getLatestVersion(); */
        return d;
    }
    
    
    
}
