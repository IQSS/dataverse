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
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.Map;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.DestructiveEdit)
public class DeaccessionDatasetVersionCommand extends AbstractCommand<DatasetVersion> {


   final DatasetVersion theVersion;
    
    public DeaccessionDatasetVersionCommand(DataverseUser aUser, DatasetVersion deaccessionVersion) {
        super(aUser, deaccessionVersion.getDataset());
        theVersion = deaccessionVersion;
    }


    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        Dataset ds = theVersion.getDataset();        
        
        theVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        
        DatasetVersion managed = ctxt.em().merge(theVersion);
        
        ctxt.index().indexDataset(ds);
        
        return managed;
    }
    
}
