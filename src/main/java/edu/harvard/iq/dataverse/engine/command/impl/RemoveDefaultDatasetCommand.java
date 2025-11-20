package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/** 
 * @author J.P. Tosca
 * Removes the default template {@link Template} for a {@link Dataverse}.
 */
@RequiredPermissions(Permission.EditDataverse)
public class RemoveDefaultDatasetCommand extends AbstractCommand<Dataverse>{

    private final Dataverse dataverse;

    public RemoveDefaultDatasetCommand(DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        dataverse.setDefaultTemplate(null);
        return dataverse;
    }
    
}
