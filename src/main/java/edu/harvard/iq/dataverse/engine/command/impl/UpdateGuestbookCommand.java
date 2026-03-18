package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions( Permission.EditDataverse )
public class UpdateGuestbookCommand extends AbstractCommand<Guestbook> {

    private final Guestbook guestbook;

    public UpdateGuestbookCommand(Guestbook guestbook, DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        this.guestbook = guestbook;
    }

    @Override
    public Guestbook execute(CommandContext ctxt) throws CommandException {
        return ctxt.guestbooks().save(guestbook);
    }
}
