/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import io.gdcc.spi.export.ExportException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.PublishDataset)
public class DeaccessionDatasetVersionCommand extends AbstractCommand<DatasetVersion> {

   private static final Logger logger = Logger.getLogger(DeaccessionDatasetVersionCommand.class.getCanonicalName());

   final DatasetVersion theVersion;
    
    public DeaccessionDatasetVersionCommand(DataverseRequest aRequest, DatasetVersion deaccessionVersion) {
        super(aRequest, deaccessionVersion.getDataset());
        theVersion = deaccessionVersion;
    }


    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        Dataset ds = theVersion.getDataset();        

        theVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        // PID changes, if any, are made in onSuccess()
        DatasetVersion managed = ctxt.em().merge(theVersion);
        
        boolean doNormalSolrDocCleanUp = true;

        
        ExportService instance = ExportService.getInstance();
        

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

        // delete all FeaturedItems with links to this deaccessioned dataset
        ctxt.dataverseFeaturedItems().deleteAllByDvObjectId(ds.getId());

        return managed;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        boolean retVal = true;
        DatasetVersion version = (DatasetVersion) r;
        Dataset dataset = version.getDataset();

        ctxt.index().asyncIndexDataset(dataset, true);

        // Let the PID provider(s) know, e.g. so that DataCite DOIs of datasets/files
        // no longer in any released version move from "findable" to "registered".
        // Only files in this version can have changed state.
        // Done after the transaction has committed and not treated as fatal.
        if (!dataset.isHarvested()) {
            deaccessionPid(dataset);
            for (FileMetadata fmd : version.getFileMetadatas()) {
                deaccessionPid(fmd.getDataFile());
            }
        }
        return retVal;
    }

    private void deaccessionPid(DvObject dvObject) {
        GlobalId pid = dvObject.getGlobalId();
        if (pid == null || !dvObject.isIdentifierRegistered()) {
            return;
        }
        try {
            PidProvider pidProvider = PidUtil.getPidProvider(pid.getProviderId());
            if (pidProvider != null) {
                pidProvider.deaccessionIdentifier(dvObject);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to update PID status after deaccession for " + pid.asString(), e);
        }
    }

}
