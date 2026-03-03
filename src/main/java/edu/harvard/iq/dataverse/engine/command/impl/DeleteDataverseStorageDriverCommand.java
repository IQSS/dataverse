package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.authorization.Permission;

@RequiredPermissions(Permission.EditDataverse)
public class DeleteDataverseStorageDriverCommand extends AbstractCommand<String> {

    private Dataverse dv;

    public DeleteDataverseStorageDriverCommand(DataverseRequest aRequest, Dataverse dv) {
        super(aRequest, dv);
        this.dv = dv;
    }

    @Override
    public String execute(CommandContext ctxt) { 
        dv.setStorageDriverId("");
        return "Storage reset to default: " + DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER;
    }
    
}
