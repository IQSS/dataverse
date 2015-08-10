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
 * @author skraffmiller
 */
@RequiredPermissions( Permission.EditDataverse )
public class CreateGuestbookCommand extends AbstractCommand<Guestbook> {
    	private final Guestbook created;
	private final Dataverse dv;
	
	public CreateGuestbookCommand(Guestbook guestbook, DataverseRequest aRequest, Dataverse anAffectedDataverse) {
		super(aRequest, anAffectedDataverse);
		created = guestbook;
		dv = anAffectedDataverse;
	}

	@Override
	public Guestbook execute(CommandContext ctxt) throws CommandException {
                
		return ctxt.guestbooks().save(created);
	}
    
}
