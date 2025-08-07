package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;

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
public class CreateDataverseCommand extends AbstractWriteDataverseCommand {

    public CreateDataverseCommand(Dataverse created,
                                  DataverseRequest request,
                                  List<DatasetFieldType> facets,
                                  List<DataverseFieldTypeInputLevel> inputLevels) {
        this(created, request, facets, inputLevels, null);
    }

    public CreateDataverseCommand(Dataverse created,
                                  DataverseRequest request,
                                  List<DatasetFieldType> facets,
                                  List<DataverseFieldTypeInputLevel> inputLevels,
                                  List<MetadataBlock> metadataBlocks) {
        super(created, created.getOwner(), request, facets, inputLevels, metadataBlocks);
    }

    @Override
    protected Dataverse innerExecute(CommandContext ctxt) throws IllegalCommandException {
        Dataverse owner = dataverse.getOwner();
        if (owner == null) {
            if (ctxt.dataverses().isRootDataverseExists()) {
                throw new IllegalCommandException("Root Dataverse already exists. Cannot create another one", this);
            }
        }
        if (!getUser().isSuperuser() && dataverse.isDatasetFileCountLimitSet(dataverse.getDatasetFileCountLimit())) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("file.dataset.error.set.file.count.limit"), this);
        }

        if (metadataBlocks != null && !metadataBlocks.isEmpty()) {
            dataverse.setMetadataBlockRoot(true);
            dataverse.setMetadataBlocks(metadataBlocks);
        }

        if (dataverse.getCreateDate() == null) {
            dataverse.setCreateDate(new Timestamp(new Date().getTime()));
        }

        if (dataverse.getCreator() == null) {
            final User user = getRequest().getUser();
            if (user.isAuthenticated()) {
                dataverse.setCreator((AuthenticatedUser) user);
            } else {
                throw new IllegalCommandException("Guest users cannot create a Dataverse.", this);
            }
        }

        if (dataverse.getDataverseType() == null) {
            dataverse.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        }

        if (dataverse.getDefaultContributorRole() == null) {
            dataverse.setDefaultContributorRole(ctxt.roles().findBuiltinRoleByAlias(DataverseRole.EDITOR));
        }

        // @todo for now we are saying all dataverses are permission root
        dataverse.setPermissionRoot(true);

        if (ctxt.dataverses().findByAlias(dataverse.getAlias()) != null) {
            throw new IllegalCommandException("A dataverse with alias " + dataverse.getAlias() + " already exists", this);
        }

        if (dataverse.getFilePIDsEnabled() != null && !ctxt.settings().isTrueForKey(SettingsServiceBean.Key.AllowEnablingFilePIDsPerCollection, false)) {
            throw new IllegalCommandException("File PIDs cannot be enabled per collection", this);
        }

        // Save the dataverse
        Dataverse managedDv = ctxt.dataverses().save(dataverse);

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
        return managedDv;
    }

    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        return ctxt.dataverses().index((Dataverse) r);
    }
}
