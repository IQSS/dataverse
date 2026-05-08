package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.Collections;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.authorization.Permission;
import java.util.Map;
import java.util.Set;

public class GetDataverseStorageDriverCommand extends AbstractCommand<String> {

    private Dataverse dv;
    private Boolean getEffective;

    public GetDataverseStorageDriverCommand(DataverseRequest aRequest, Dataverse dv, Boolean getEffective) {
        super(aRequest, dv);
        this.dv = dv;
        this.getEffective = getEffective;
    }

    @Override
    public String execute(CommandContext ctxt) {

        if (getEffective != null && getEffective) {
            return dv.getEffectiveStorageDriverId();
        } else {
            return dv.getStorageDriverId();
        }
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dv.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }  
    
}
