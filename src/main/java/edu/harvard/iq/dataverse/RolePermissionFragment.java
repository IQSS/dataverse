/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataversePermissionRootCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class RolePermissionFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(RolePermissionFragment.class.getCanonicalName());

    @EJB
    DataverseRoleServiceBean roleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    EjbDataverseEngine commandEngine;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataverseSession session;

    DvObject dvObject;

    public DvObject getDvObject() {
        return dvObject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
        if (dvObject instanceof Dataverse) {
            inheritAssignments = !((Dataverse) dvObject).isPermissionRoot();
        }
    }

    /* Inherit assignments related code */
    boolean inheritAssignments;
    
    public boolean isInheritAssignments() {
        return inheritAssignments;
    }

    public void setInheritAssignments(boolean inheritAssignments) {
        this.inheritAssignments = inheritAssignments;
    }

    public void updatePermissionRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        try {
            dvObject = commandEngine.submit(new UpdateDataversePermissionRootCommand(!inheritAssignments, session.getUser(), (Dataverse) dvObject));
            inheritAssignments = !((Dataverse) dvObject).isPermissionRoot();
        } catch (CommandException ex) {
            Logger.getLogger(RolePermissionFragment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* permissions tab related methods */
    private String assignRoleUsername;
    private Long assignRoleRoleId;

    private List<String> identifierList = new ArrayList();

    public List<String> getIdentifiers(String query) {
        if (identifierList.isEmpty()) {
            for (AuthenticatedUser au : authenticationService.findAllAuthenticatedUsers()) {
                identifierList.add(au.getIdentifier());
            }
        }
        List<String> returnList = new ArrayList();
        for (String identifier : identifierList) {
            if (identifier.contains(query)) {
                returnList.add(identifier);
            }
        }
        return returnList;
    }

    public List<DataverseRole> getAvailableRoles() {
        List<DataverseRole> roles = new LinkedList<>();
        if (dvObject != null && (dvObject instanceof Dataverse || dvObject instanceof Dataset)) {
            // current the available roles for a dataset are gotten from its parent
            Dataverse dv = dvObject instanceof Dataverse ? (Dataverse) dvObject : ((Dataset) dvObject).getOwner();

            roles.addAll(roleService.availableRoles(dv.getId()));

            Collections.sort(roles, DataverseRole.CMP_BY_NAME);
        }
        return roles;
    }

    public List<RoleAssignmentRow> getRoleAssignments() {
        List<RoleAssignmentRow> raList = null;
        if (dvObject != null) {
            Set<RoleAssignment> ras = roleService.rolesAssignments(dvObject);
            raList = new ArrayList<>(ras.size());
            for (RoleAssignment ra : ras) {
                raList.add(new RoleAssignmentRow(ra, roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo()));
            }
        }
        return raList;
    }

    public void toggleFileRestrict(ActionEvent evt) {
        DataFile file = (DataFile) dvObject;
        file.setRestricted(!file.isRestricted());
    }
    
    public void grantAccess(ActionEvent evt) {
        // @todo add the logic to get the built in role for file download
        assignRole(assignRoleUsername, new Long(1));
    }
    public void assignRole(ActionEvent evt) {
        assignRole(assignRoleUsername, assignRoleRoleId);
    }

    private void assignRole(String identifier, Long assignedRoleId) {
        RoleAssignee roas = roleAssigneeService.getRoleAssignee(identifier);
        DataverseRole r = roleService.find(assignedRoleId);

        try {
            commandEngine.submit(new AssignRoleCommand(roas, r, dvObject, session.getUser()));
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Role " + r.getName() + " assigned to " + roas.getDisplayInfo().getTitle() + " on " + dvObject.getDisplayName());
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Can't assign role: " + ex.getMessage());
        }
    }

    public void revokeRole(Long roleAssignmentId) {
        try {
            commandEngine.submit(new RevokeRoleCommand(em.find(RoleAssignment.class, roleAssignmentId), session.getUser()));
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Role assignment revoked successfully");
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot revoke role assignment - you're missing permission", ex.getRequiredPermissions().toString());
            logger.log(Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex);

        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot revoke role assignment: " + ex.getMessage());
            logger.log(Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex);
        }
    }

    public String getAssignRoleUsername() {
        return assignRoleUsername;
    }

    public void setAssignRoleUsername(String assignRoleUsername) {
        this.assignRoleUsername = assignRoleUsername;
    }

    public Long getAssignRoleRoleId() {
        return assignRoleRoleId;
    }

    public void setAssignRoleRoleId(Long assignRoleRoleId) {
        this.assignRoleRoleId = assignRoleRoleId;
    }

    public static class RoleAssignmentRow {

        private final RoleAssigneeDisplayInfo assigneeDisplayInfo;
        private final RoleAssignment ra;

        public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf) {
            ra = anRa;
            assigneeDisplayInfo = disInf;
        }

        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return assigneeDisplayInfo;
        }

        public DataverseRole getRole() {
            return ra.getRole();
        }

        public String getRoleName() {
            return getRole().getName();
        }

        public DvObject getDefinitionPoint() {
            return ra.getDefinitionPoint();
        }

        public String getAssignedDvName() {
            return ra.getDefinitionPoint().getDisplayName();
        }

        public Long getId() {
            return ra.getId();
        }

    }

    /* Roles tab related methods */
    private DataverseRole role = new DataverseRole();
    private List<String> selectedPermissions;

    public List<Permission> getPermissions() {
        return Arrays.asList(Permission.values());
    }

    public List<DataverseRole> getRoles() {
        if (dvObject != null) {
            return roleService.findByOwnerId(dvObject.getId());
        }
        return new ArrayList();
    }

    public void createNewRole(ActionEvent e) {
        setRole(new DataverseRole());
    }

    public void editRole(String roleId) {
        setRole(roleService.find(Long.parseLong(roleId)));
    }

    public void updateRole(ActionEvent e) {
        // @todo currently only works for Dataverse since CreateRoleCommand only takes a dataverse
        // we need to decide if we want roles at the dataset level or not
        if (dvObject instanceof Dataverse) {
            role.setOwner(dvObject);
            role.clearPermissions();
            for (String pmsnStr : getSelectedPermissions()) {
                role.addPermission(Permission.valueOf(pmsnStr));
            }
            try {
                setRole(commandEngine.submit(new CreateRoleCommand(role, session.getUser(), (Dataverse) dvObject)));
                JH.addMessage(FacesMessage.SEVERITY_INFO, "Role '" + role.getName() + "' saved", "");
            } catch (CommandException ex) {
                JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot save role", ex.getMessage());
                Logger.getLogger(ManageRolesPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public DataverseRole getRole() {
        return role;
    }

    public void setRole(DataverseRole role) {
        this.role = role;
        selectedPermissions = new LinkedList<>();
        if (role != null) {
            for (Permission p : role.permissions()) {
                selectedPermissions.add(p.name());
            }
        }
    }

    public List<String> getSelectedPermissions() {
        return selectedPermissions;
    }

    public void setSelectedPermissions(List<String> selectedPermissions) {
        this.selectedPermissions = selectedPermissions;
    }

}
