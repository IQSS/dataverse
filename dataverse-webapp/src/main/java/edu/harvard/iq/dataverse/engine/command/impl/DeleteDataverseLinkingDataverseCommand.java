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
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;

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
            ctxt.index().indexDataverse(editedDv);
            ctxt.index().indexDataverse(doomed.getLinkingDataverse());
        }
        return merged;
    } 
}
