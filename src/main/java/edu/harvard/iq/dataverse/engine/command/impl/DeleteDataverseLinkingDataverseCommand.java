/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
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

@RequiredPermissions( Permission.LinkDataverse )
public class DeleteDataverseLinkingDataverseCommand extends AbstractCommand<Dataverse> {

    private final DataverseLinkingDataverse doomed;
    private final Dataverse linkedDataverse;
    private final boolean index;
    
    public DeleteDataverseLinkingDataverseCommand(DataverseRequest aRequest, Dataverse linkedDataverse , DataverseLinkingDataverse doomed, boolean index) {
        super(aRequest, linkedDataverse);
        this.linkedDataverse = linkedDataverse;
        this.doomed = doomed;
        this.index = index;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        ctxt.em().remove(doomed);
        return linkedDataverse;
    } 

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {

        try {
            if (index) {
                ctxt.index().indexDataverse(doomed.getDataverse());
                ctxt.index().indexDataverse(doomed.getLinkingDataverse());
            }
        } catch (IOException | SolrServerException e) {
            Dataverse dv = (Dataverse) r;
            String failureLogText = "Indexing failed for Dataverse delinking. You can kickoff a re-index of this datavese with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dv.getId().toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(this, failureLogText, (Dataverse) r);
            return false;
        }

        return true;

    }

}
