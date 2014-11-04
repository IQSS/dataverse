package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
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
       User aUser, List<DatasetFieldType> facetList, List<DataverseFieldTypeInputLevel> inputLevelList) {
        super(aUser, created.getOwner());
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
                throw new CommandException("Root Dataverse already exists. Cannot create another one", this);
            }
        }

		if ( created.getCreateDate() == null )  {
			created.setCreateDate( new Timestamp(new Date().getTime()) );
        }
        // By default, themeRoot should be true
        created.setThemeRoot(true);
        if (created.getCreator() == null) {
            // FIXME Is the "creator" concept being carried over from 3.x?
//			created.setCreator(getUser());
        }

        if (created.getDataverseType() == null) {
            created.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        }

        // Save the dataverse
        Dataverse managedDv = ctxt.dataverses().save(created);

		// Create the manager role and assign it to the creator. This can't be done using commands,
        // as no one is allowed to do anything on the newly created dataverse yet.
        // TODO this can be optimized out if the creating user has full permissions
        // on the parent dv, and the created dv is not a permission root.
        DataverseRole managerRole = new DataverseRole();
	managerRole.addPermissions( EnumSet.allOf(Permission.class) );

        managerRole.setAlias("manager");
        managerRole.setName("Dataverse Manager");
        managerRole.setDescription("Auto-generated role for the creator of this dataverse");
        managerRole.setOwner(managedDv);

        ctxt.roles().save(managerRole);

        ctxt.roles().save(new RoleAssignment(managerRole, getUser(), managedDv));
        
        ctxt.index().indexDataverse(managedDv);  
       if (facetList != null) {
            ctxt.facets().deleteFacetsFor(managedDv);
            int i = 0;
            for (DatasetFieldType df : facetList) {
                ctxt.facets().create(i++, df.getId(), managedDv.getId());
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
