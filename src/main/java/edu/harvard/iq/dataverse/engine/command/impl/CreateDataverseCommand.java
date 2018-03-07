package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TODO make override the date and user more active, so prevent code errors.
 * e.g. another command, with explicit parameters.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataverse)
public class CreateDataverseCommand extends AbstractCommand<Dataverse> {

    private final Dataverse created;
    private final List<DataverseFieldTypeInputLevel> inputLevelList;
    private final List<DatasetFieldType> facetList;

    public CreateDataverseCommand(Dataverse created,
            DataverseRequest aRequest, List<DatasetFieldType> facetList, List<DataverseFieldTypeInputLevel> inputLevelList) {
        super(aRequest, created.getOwner());
        this.created = created;
        if (facetList != null) {
            this.facetList = new ArrayList<>(facetList);
        } else {
            this.facetList = null;
        }
        if (inputLevelList != null) {
            this.inputLevelList = new ArrayList<>(inputLevelList);
        } else {
            this.inputLevelList = null;
        }
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {

        if (created.getOwner() == null) {
            if (ctxt.dataverses().isRootDataverseExists()) {
                throw new IllegalCommandException("Root Dataverse already exists. Cannot create another one", this);
            }
        }

        if (created.getCreateDate() == null) {
            created.setCreateDate(new Timestamp(new Date().getTime()));
        }
        
        if (created.getCreator() == null) {
            final User user = getRequest().getUser();
            if ( user.isAuthenticated() ) {
                created.setCreator((AuthenticatedUser) user);
            } else {
                throw new IllegalCommandException("Guest users cannot create a Dataverse.", this);
            }
        }

        if (created.getDataverseType() == null) {
            created.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        }
        
        if (created.getDefaultContributorRole() == null) {
            created.setDefaultContributorRole(ctxt.roles().findBuiltinRoleByAlias(DataverseRole.EDITOR));
        }
        
        // @todo for now we are saying all dataverses are permission root
        created.setPermissionRoot(true);
        
        if ( ctxt.dataverses().findByAlias( created.getAlias()) != null ) {
            throw new IllegalCommandException("A dataverse with alias " + created.getAlias() + " already exists", this );
        }
        
        // Save the dataverse
        Dataverse managedDv = ctxt.dataverses().save(created);

        // Find the built in admin role (currently by alias)
        DataverseRole adminRole = ctxt.roles().findBuiltinRoleByAlias(DataverseRole.ADMIN);
        String privateUrlToken = null;
        ctxt.roles().save(new RoleAssignment(adminRole, getRequest().getUser(), managedDv, privateUrlToken));

        managedDv.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        managedDv = ctxt.dataverses().save(managedDv);

        ctxt.index().indexDataverse(managedDv);
        if (facetList != null) {
            ctxt.facets().deleteFacetsFor(managedDv);
            int i = 0;
            for (DatasetFieldType df : facetList) {
                ctxt.facets().create(i++, df, managedDv);
            }
        }

        if (inputLevelList != null) {
            ctxt.fieldTypeInputLevels().deleteFacetsFor(managedDv);
            for (DataverseFieldTypeInputLevel obj : inputLevelList) {
                obj.setDataverse(managedDv);
                ctxt.fieldTypeInputLevels().create(obj);
            }
        }
        return managedDv;
    }

}
