/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseGuestbookCommand extends AbstractCommand<Dataverse> {

    private final Dataverse editedDv;
    private final Guestbook guestbook;

    public UpdateDataverseGuestbookCommand(Dataverse editedDv, Guestbook guestbook, DataverseRequest aRequest) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.guestbook = guestbook;
    }

    @Override
    public Dataverse execute(CommandContext ctxt)  {
        //remove dataset assignments of disabled guestbooks
        if (!this.guestbook.isEnabled()) {
            ctxt.em().createNativeQuery(
                    "Update Dataset set guestbook_id = null "
                            + "WHERE guestbook_id =" + this.guestbook.getId()
            ).executeUpdate();
        }

        ctxt.em().merge(this.guestbook);
        Dataverse result = ctxt.dataverses().save(editedDv);
        return result;
    }

}
