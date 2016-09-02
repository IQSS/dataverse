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
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.ResourceBundle;
import java.util.logging.Level;

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
        
        if (deleteDOIIdentifier) {
            String nonNullDefaultIfKeyNotFound = "";

            String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);

            if (doiProvider.equals("EZID")) {
                ctxt.doiEZId().deleteIdentifier(ds);
            }
            if (doiProvider.equals("DataCite")) {
                try {
                    ctxt.doiDataCite().deleteIdentifier(ds);
                } catch (Exception e) {
                    if (e.toString().contains("Internal Server Error")) {
                        throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.publish.error.datacite"), this);
                    }
                    throw new CommandException(ResourceBundle.getBundle("Bundle").getString("dataset.delete.error.datacite"), this);
                }
            }
        }     
        DatasetVersion managed = ctxt.em().merge(theVersion);
        
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(managed.getDataset(), doNormalSolrDocCleanUp);
        
        // if there is still another released version of this dataset, 
        // we want to re-export it : 
        
        ExportService instance = ExportService.getInstance();
        
        if (managed.getDataset().getReleasedVersion() != null) {
            try {
                instance.exportAllFormats(managed.getDataset());
            } catch (ExportException ex) {
                // Something went wrong!  
                // But we're not going to treat it as a fatal condition. 
            }
        } else {
            // otherwise, we need to wipe clean the exports we may have cached: 
            instance.clearAllCachedFormats(managed.getDataset());
        }
        // And save the dataset, to get the "last exported" timestamp right:
        
        Dataset managedDs = ctxt.em().merge(managed.getDataset());
        
        return managed;
    }
    
}
