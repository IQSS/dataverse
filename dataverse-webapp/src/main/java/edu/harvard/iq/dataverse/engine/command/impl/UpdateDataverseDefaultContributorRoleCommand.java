package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author skraffmiller
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class UpdateDataverseDefaultContributorRoleCommand extends AbstractCommand<Dataverse> {

    private final DataverseRole role;
    private Dataverse dv;

    public UpdateDataverseDefaultContributorRoleCommand(DataverseRole role, DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        this.role = role;
        this.dv = anAffectedDataverse;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) {
        dv.setDefaultContributorRole(role);
        dv = ctxt.dataverses().save(dv);
        return dv;
    }

}
