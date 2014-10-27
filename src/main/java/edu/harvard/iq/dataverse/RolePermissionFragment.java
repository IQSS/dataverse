/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
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
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService; 
    @EJB
    EjbDataverseEngine commandEngine;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;    

    @Inject
    DataverseSession session;    
    
    Dataverse dvObject; 

    public Dataverse getDvObject() {
        return dvObject;
    }

    public void setDvObject(Dataverse dvObject) {
        this.dvObject = dvObject;
    }
    
    
            
    /* permissions tab related methods */
    private String assignRoleUsername;
    private Long assignRoleRoleId;

    public List<DataverseRole> getAvailableRoles() {
        List<DataverseRole> roles = new LinkedList<>();
        if (dvObject != null) {
            for (Map.Entry<Dataverse, Set<DataverseRole>> e
                    : roleService.availableRoles(dvObject.getId()).entrySet()) {
                for (DataverseRole aRole : e.getValue()) {
                    roles.add(aRole);
                }
            }
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

    public void assignRole(ActionEvent evt) {
        RoleAssignee roas = roleAssigneeService.getRoleAssignee(getAssignRoleUsername());
        DataverseRole r = roleService.find(getAssignRoleRoleId());

        try {
            commandEngine.submit(new AssignRoleCommand(roas, r, dvObject, session.getUser()));
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Role " + r.getName() + " assigned to " + roas.getDisplayInfo().getTitle() + " on " + dvObject.getName());
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

        public Dataverse getAssignDv() {
            return (Dataverse) ra.getDefinitionPoint();
        }

        public String getRoleName() {
            return getRole().getName();
        }

        public String getAssignedDvName() {
            return getAssignDv().getName();
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

    public boolean isHasRoles() {
        return !getRoles().isEmpty();
    }

    public void createNewRole(ActionEvent e) {
        setRole(new DataverseRole());
    }

    public void editRole(String roleId) {
        setRole(roleService.find(Long.parseLong(roleId)));
    }

    public void updateRole(ActionEvent e) {
        role.setOwner(dvObject);
        role.clearPermissions();
        for (String pmsnStr : getSelectedPermissions()) {
            role.addPermission(Permission.valueOf(pmsnStr));
        }
        try {
            setRole(commandEngine.submit(new CreateRoleCommand(role, session.getUser(), dvObject)));
            JH.addMessage(FacesMessage.SEVERITY_INFO, "Role '" + role.getName() + "' saved", "");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot save role", ex.getMessage());
            Logger.getLogger(ManageRolesPage.class.getName()).log(Level.SEVERE, null, ex);
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
