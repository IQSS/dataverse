package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final List<MetadataBlock> metadataBlocks;

    public CreateDataverseCommand(Dataverse created,
                                  DataverseRequest aRequest,
                                  List<DatasetFieldType> facetList,
                                  List<DataverseFieldTypeInputLevel> inputLevelList) {
        this(created, aRequest, facetList, inputLevelList, null);
    }

    public CreateDataverseCommand(Dataverse created,
                                  DataverseRequest aRequest,
                                  List<DatasetFieldType> facetList,
                                  List<DataverseFieldTypeInputLevel> inputLevelList,
                                  List<MetadataBlock> metadataBlocks) {
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
        if (metadataBlocks != null) {
            this.metadataBlocks = new ArrayList<>(metadataBlocks);
        } else {
            this.metadataBlocks = null;
        }
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {

        Dataverse owner = created.getOwner();
        if (owner == null) {
            if (ctxt.dataverses().isRootDataverseExists()) {
                throw new IllegalCommandException("Root Dataverse already exists. Cannot create another one", this);
            }
        }

        if (metadataBlocks != null && !metadataBlocks.isEmpty()) {
            created.setMetadataBlockRoot(true);
            created.setMetadataBlocks(metadataBlocks);
        }

        if (created.getCreateDate() == null) {
            created.setCreateDate(new Timestamp(new Date().getTime()));
        }

        if (created.getCreator() == null) {
            final User user = getRequest().getUser();
            if (user.isAuthenticated()) {
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

        if (ctxt.dataverses().findByAlias(created.getAlias()) != null) {
            throw new IllegalCommandException("A dataverse with alias " + created.getAlias() + " already exists", this);
        }

        if (created.getFilePIDsEnabled() != null && !ctxt.settings().isTrueForKey(SettingsServiceBean.Key.AllowEnablingFilePIDsPerCollection, false)) {
            throw new IllegalCommandException("File PIDs cannot be enabled per collection", this);
        }

        // Save the dataverse
        Dataverse managedDv = ctxt.dataverses().save(created);

        // Find the built in admin role (currently by alias)
        DataverseRole adminRole = ctxt.roles().findBuiltinRoleByAlias(DataverseRole.ADMIN);
        String privateUrlToken = null;

        ctxt.roles().save(new RoleAssignment(adminRole, getRequest().getUser(), managedDv, privateUrlToken), false);
        // Add additional role assignments if inheritance is set
        boolean inheritAllRoles = false;
        String rolesString = ctxt.settings().getValueForKey(SettingsServiceBean.Key.InheritParentRoleAssignments, "");
        ArrayList<String> rolesToInherit = new ArrayList<String>(Arrays.asList(rolesString.split("\\s*,\\s*")));
        if (rolesString.length() > 0) {
            if (!rolesToInherit.isEmpty()) {
                if (rolesToInherit.contains("*")) {
                    inheritAllRoles = true;
                }

                List<RoleAssignment> assignedRoles = ctxt.roles().directRoleAssignments(owner);
                for (RoleAssignment role : assignedRoles) {
                    //Only supporting built-in/non-dataverse-specific custom roles. Custom roles all have an owner.
                    if (role.getRole().getOwner() == null) {
                        // And... If all roles are to be inherited, or this role is in the list, and, in both
                        // cases, this is not an admin role for the current user which was just created
                        // above...
                        if ((inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias()))
                                && !(role.getAssigneeIdentifier().equals(getRequest().getUser().getIdentifier())
                                && role.getRole().equals(adminRole))) {
                            String identifier = role.getAssigneeIdentifier();
                            if (identifier.startsWith(AuthenticatedUser.IDENTIFIER_PREFIX)) {
                                identifier = identifier.substring(AuthenticatedUser.IDENTIFIER_PREFIX.length());
                                ctxt.roles().save(new RoleAssignment(role.getRole(),
                                        ctxt.authentication().getAuthenticatedUser(identifier), managedDv, privateUrlToken), false);
                            } else if (identifier.startsWith(Group.IDENTIFIER_PREFIX)) {
                                identifier = identifier.substring(Group.IDENTIFIER_PREFIX.length());
                                Group roleGroup = ctxt.groups().getGroup(identifier);
                                if (roleGroup != null) {
                                    ctxt.roles().save(new RoleAssignment(role.getRole(),
                                            roleGroup, managedDv, privateUrlToken), false);
                                }
                            }
                        }
                    }
                }
            }
        }

        managedDv.setPermissionModificationTime(new Timestamp(new Date().getTime()));

        if (facetList != null) {
            ctxt.facets().deleteFacetsFor(managedDv);

            if (!facetList.isEmpty()) {
                managedDv.setFacetRoot(true);
            }

            int i = 0;
            for (DatasetFieldType df : facetList) {
                ctxt.facets().create(i++, df, managedDv);
            }
        }

        if (inputLevelList != null) {
            if (!inputLevelList.isEmpty()) {
                managedDv.addInputLevelsMetadataBlocksIfNotPresent(inputLevelList);
            }
            ctxt.fieldTypeInputLevels().deleteFacetsFor(managedDv);
            for (DataverseFieldTypeInputLevel inputLevel : inputLevelList) {
                inputLevel.setDataverse(managedDv);
                ctxt.fieldTypeInputLevels().create(inputLevel);
            }
        }

        // TODO: save is called here and above; we likely don't need both
        managedDv = ctxt.dataverses().save(managedDv);
        return managedDv;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        return ctxt.dataverses().index((Dataverse) r);
    }

}
