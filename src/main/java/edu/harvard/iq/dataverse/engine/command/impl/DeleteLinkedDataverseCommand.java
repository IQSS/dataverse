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
public class DeleteLinkedDataverseCommand extends AbstractCommand<Dataverse> {

    private final DataverseLinkingDataverse doomed;
    private final Dataverse editedDv;
    
    public DeleteLinkedDataverseCommand(DataverseRequest aRequest, Dataverse editedDv , DataverseLinkingDataverse doomed) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.doomed = doomed;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        if ((!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException("Move Dataset can only be called by superusers.",
                    this, Collections.singleton(Permission.DeleteDataverse), editedDv);
        }
        Dataverse merged = ctxt.em().merge(editedDv);
        DataverseLinkingDataverse doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);
        return merged;
    } 
}
