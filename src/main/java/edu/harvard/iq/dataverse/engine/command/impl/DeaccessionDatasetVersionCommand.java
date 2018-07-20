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
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataset)
public class DeaccessionDatasetVersionCommand extends AbstractCommand<DatasetVersion> {

   private static final Logger logger = Logger.getLogger(DeaccessionDatasetVersionCommand.class.getCanonicalName());

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
        /* We do not want to delete the identifier if the dataset is completely deaccessioned
        
        logger.fine("deleteDOIIdentifier=" + deleteDOIIdentifier);
        if (deleteDOIIdentifier) {
            String nonNullDefaultIfKeyNotFound = "";
            String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
            ArrayList<String> currentProtocol = new ArrayList<>();
            currentProtocol.add(protocol);
            IdServiceBean idServiceBean = IdServiceBean.getBean(ctxt);

            logger.fine("protocol=" + protocol);
            try {
                idServiceBean.deleteIdentifier(ds);
            } catch (Exception e) {
                if (e.toString().contains("Internal Server Error")) {
                     throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", idServiceBean.getProviderInformation()),this); 
                }
                throw new CommandException(BundleUtil.getStringFromBundle("dataset.delete.error", currentProtocol),this); 
            }
        }*/
        DatasetVersion managed = ctxt.em().merge(theVersion);
        
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(managed.getDataset(), doNormalSolrDocCleanUp);

        
        ExportService instance = ExportService.getInstance(ctxt.settings());
        

        if (managed.getDataset().getReleasedVersion() != null) {
            try {
                instance.exportAllFormats(managed.getDataset());
            } catch (ExportException ex) {
                // Something went wrong!
                // But we're not going to treat it as a fatal condition.
            }
        } else {
            try {
                // otherwise, we need to wipe clean the exports we may have cached:
                instance.clearAllCachedFormats(managed.getDataset());
            } catch (IOException ex) {
                //Try catch required due to original method for clearing cached metadata (non fatal)
            }
        }
        // And save the dataset, to get the "last exported" timestamp right:

        Dataset managedDs = ctxt.em().merge(managed.getDataset());

        return managed;
    }
    
}
