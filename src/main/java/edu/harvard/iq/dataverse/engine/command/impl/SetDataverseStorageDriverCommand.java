package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.Map.Entry;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.authorization.Permission;

@RequiredPermissions(Permission.EditDataverse)
public class SetDataverseStorageDriverCommand extends AbstractCommand<String> {

    private Dataverse dv;
    private String label;

    public SetDataverseStorageDriverCommand(DataverseRequest aRequest, Dataverse dv, String label) {
        super(aRequest, dv);
        this.dv = dv;
        this.label = label;
    }

    @Override
    public String execute(CommandContext ctxt) {

        String storageDriverId = null;
        for (Entry<String, String> store: DataAccess.getStorageDriverLabels().entrySet()) {
            if(store.getKey().equals(label)) {
                storageDriverId = store.getValue();
            }  
        }

        if (storageDriverId != null) {
            dv.setStorageDriverId(storageDriverId);
            return "Storage set to: " + storageDriverId + "/" + label;
        } else {
            throw new IllegalArgumentException("No Storage Driver found for : " + label);
        }

    }
    
}
