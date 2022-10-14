/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author sarahferry
 */

@RequiredPermissions( Permission.EditDataverse )
public class DeleteDataverseLinkingDataverseCommand extends AbstractCommand<Dataverse> {

    private final DataverseLinkingDataverse doomed;
    private final Dataverse editedDv;
    private final boolean index;
    
    public DeleteDataverseLinkingDataverseCommand(DataverseRequest aRequest, Dataverse editedDv , DataverseLinkingDataverse doomed, boolean index) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.doomed = doomed;
        this.index = index;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        if ((!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException("Delete dataverse linking dataverse can only be called by superusers.",
                    this, Collections.singleton(Permission.DeleteDataverse), editedDv);
        }
        Dataverse merged = ctxt.em().merge(editedDv);
        DataverseLinkingDataverse doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);
        
        if (index) {
            //can only index merged in the onSuccess method so must index doomed linking dataverse here
            try {
                ctxt.index().indexDataverse(doomed.getLinkingDataverse());
            } catch (IOException | SolrServerException e) {
                String failureLogText = "Indexing failed for Linked Dataverse. You can kickoff a re-index of this datavese with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + doomed.getLinkingDataverse().getId().toString();
                failureLogText += "\r\n" + e.getLocalizedMessage();
                LoggingUtil.writeOnSuccessFailureLog(this, failureLogText, doomed.getLinkingDataverse());
            } 
        }
        return merged;
    } 

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {

        try {
            Future<String> retVal = ctxt.index().indexDataverse((Dataverse) r);
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
