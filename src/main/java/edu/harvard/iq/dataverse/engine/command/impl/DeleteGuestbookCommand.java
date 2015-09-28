/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions( Permission.EditDataverse )
public class DeleteGuestbookCommand extends AbstractCommand<Dataverse>{

    private final Guestbook doomed;
    private final Dataverse editedDv;
    
    public DeleteGuestbookCommand(DataverseRequest aRequest, Dataverse editedDv , Guestbook doomed) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.doomed = doomed;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        Dataverse merged = ctxt.em().merge(editedDv);
        Guestbook doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);
        return merged;
    }
    
}
