/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataset)
public class DeaccessionDatasetVersionCommand extends AbstractCommand<DatasetVersion> {


   final DatasetVersion theVersion;
   final boolean deleteDOIIdentifier;
    
    public DeaccessionDatasetVersionCommand(DataverseRequest aRequest, DatasetVersion deaccessionVersion, boolean deleteDOIIdentifierIn) {
        super(aRequest, deaccessionVersion.getDataset());
        theVersion = deaccessionVersion;
        deleteDOIIdentifier = deleteDOIIdentifierIn;
    }


    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        Dataset ds = theVersion.getDataset();        

        theVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        
        if (deleteDOIIdentifier){
           ctxt.doiEZId().deleteIdentifier(ds);
        }       
        DatasetVersion managed = ctxt.em().merge(theVersion);
        
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(managed.getDataset(), doNormalSolrDocCleanUp);
        
        return managed;
    }
    
}
