package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 * Gets the default contributor role of a collection.
 * Used by the /api/dataverses/.../defaultContributorRole API.
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class GetCollectionDefaultContributorRoleCommand extends AbstractCommand<DataverseRole> {

    private final Dataverse dataverse;

    public GetCollectionDefaultContributorRoleCommand(DataverseRequest aRequest, Dataverse target) {
        super(aRequest, target);
        dataverse = target;
    }

    @Override
    public DataverseRole execute(CommandContext ctxt) throws CommandException {

        if (dataverse != null) {
            return dataverse.getDefaultContributorRole();
        }

        return null;
    }
}


