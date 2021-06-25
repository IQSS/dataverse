package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import javax.persistence.NoResultException;

/**
 * Create a new role in a dataverse.
 *
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class CreateRoleCommand extends AbstractCommand<DataverseRole> {

    private final DataverseRole created;
    private final Dataverse dv;

    public CreateRoleCommand(DataverseRole aRole, DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        created = aRole;
        dv = anAffectedDataverse;
    }

    @Override
    public DataverseRole execute(CommandContext ctxt) throws CommandException {
        User user = getUser();
        //todo: temporary for 4.0 - only superusers can create and edit roles
        if ((!(user instanceof AuthenticatedUser) || !user.isSuperuser())) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("permission.role.must.be.created.by.superuser"),this);
        }
        //Test to see if the role already exists in DB
        try {
            DataverseRole testRole = ctxt.em().createNamedQuery("DataverseRole.findDataverseRoleByAlias", DataverseRole.class)
                    .setParameter("alias", created.getAlias())
                    .getSingleResult();
            if (!(testRole == null)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("permission.role.not.created.alias.already.exists"), this);
            }
        } catch (NoResultException nre) {
            //  we want no results because that meand we can create a role
        }
        dv.addRole(created);
        return ctxt.roles().save(created);
    }
    
}
