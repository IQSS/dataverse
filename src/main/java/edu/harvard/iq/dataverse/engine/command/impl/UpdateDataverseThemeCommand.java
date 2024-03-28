package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Update an existing dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.EditDataverse )
public class UpdateDataverseThemeCommand extends AbstractCommand<Dataverse> {
    private final Dataverse editedDv;

    public UpdateDataverseThemeCommand(Dataverse editedDv, DataverseRequest aRequest) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
    }

    /**
     * Update Theme and Widget related data for this dataverse.
     * 
     * @param ctxt
     * @return
     * @throws CommandException 
     */
    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        // save updated dataverse to db
        return ctxt.dataverses().save(editedDv);
    }

}
