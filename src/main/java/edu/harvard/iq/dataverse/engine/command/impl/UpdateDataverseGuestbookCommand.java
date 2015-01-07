/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseGuestbookCommand extends AbstractCommand<Dataverse> {

    private final Dataverse editedDv;
    private final Guestbook guestbook;

    public UpdateDataverseGuestbookCommand(Dataverse editedDv, Guestbook guestbook, User aUser) {
        super(aUser, editedDv);
        this.editedDv = editedDv;
        this.guestbook = guestbook;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        ctxt.em().merge(this.guestbook);
        Dataverse result = ctxt.dataverses().save(editedDv);
        return result;
    }
    
}
