/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author sarahferry
 */

@RequiredPermissions( Permission.LinkDataset )
public class DeleteDatasetLinkingDataverseCommand extends AbstractCommand<Dataset>{
    private final DatasetLinkingDataverse doomed;
    private final Dataset editedDs;
    private final boolean index;
    
    public DeleteDatasetLinkingDataverseCommand(DataverseRequest aRequest, Dataset editedDs , DatasetLinkingDataverse doomed, boolean index) {
        super(aRequest, doomed.getLinkingDataverse());
        this.editedDs = editedDs;
        this.doomed = doomed;
        this.index = index;
    }
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        Dataset merged = ctxt.em().merge(editedDs);
        DatasetLinkingDataverse doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);

        try {
            ctxt.index().indexDataverse(doomed.getLinkingDataverse());
        } catch (IOException | SolrServerException e) {    
            String failureLogText = "Post delete linking dataverse indexing failed for Dataverse. ";
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(this, failureLogText,  doomed.getLinkingDataverse());
        }

        return merged;
    }
    
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        boolean retVal = true;
        Dataset dataset = (Dataset) r;

        if (index) {
            ctxt.index().asyncIndexDataset(dataset, true);
        }

        return retVal;
    }
}
