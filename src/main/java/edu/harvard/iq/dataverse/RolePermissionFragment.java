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
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdatePermissionRootCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

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
    @Inject
    DataverseRequestServiceBean dvRequestService;

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

    public void updatePermissionRoot(AjaxBehaviorEvent event) throws AbortProcessingException {
        try {
            dvObject = commandEngine.submit(
                    new UpdatePermissionRootCommand(!inheritAssignments, 
                                                    dvRequestService.getDataverseRequest(),
                                                    (Dataverse) dvObject));
            inheritAssignments = !((DvObjectContainer) dvObject).isPermissionRoot();
        } catch (CommandException ex) {
            Logger.getLogger(RolePermissionFragment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* permissions tab related methods */
    private String assignRoleUsername; // used if input accepts a username
    private RoleAssignee assignRoleRoleAssignee; // used if input accepts a RoleAssignee through a converter
    private Long assignRoleRoleId;

    private final List<String> identifierList = new ArrayList<>();

    public List<String> completeIdentifier(String query) {
        if (identifierList.isEmpty()) {
            for (AuthenticatedUser au : authenticationService.findAllAuthenticatedUsers()) {
                identifierList.add(au.getIdentifier());
            }
        }
        List<String> returnList = new ArrayList<>();
        for (String identifier : identifierList) {
            if (identifier.contains(query)) {
                returnList.add(identifier);
            }
        }
        return returnList;
    }
    
    private final List<RoleAssignee> roleAssigneeList = new ArrayList<>();
    
    public List<RoleAssignee> completeRoleAssignee(String query) {
        if (roleAssigneeList.isEmpty()) {
            for (AuthenticatedUser au : authenticationService.findAllAuthenticatedUsers()) {
                roleAssigneeList.add(au);
            }
        }
        List<RoleAssignee> returnList = new ArrayList<>();
        for (RoleAssignee ra : roleAssigneeList) {
            // @todo unsure if containsIgnore case will work for all locales
            if (StringUtils.containsIgnoreCase(ra.getDisplayInfo().getTitle(),query)) {
                returnList.add(ra);
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
                // for files, only show role assihnemnts which can download
                if (!(dvObject instanceof DataFile) || ra.getRole().permissions().contains(Permission.DownloadFile)) {
                    raList.add(new RoleAssignmentRow(ra, roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo()));
                }
            }
        }
        return raList;
    }

    public void toggleFileRestrict(ActionEvent evt) {
        DataFile file = (DataFile) dvObject;
        file.setRestricted(!file.isRestricted());
    }
    
    public void grantAccess(ActionEvent evt) {
        //RoleAssignee assignRoleRoleAssignee = roleAssigneeService.getRoleAssignee(assignRoleUsername);
	// Find the built in file downloader role (currently by alias)        
        assignRole(assignRoleRoleAssignee, roleService.findBuiltinRoleByAlias("fileDownloader"));
    }
    public void assignRole(ActionEvent evt) {
        //RoleAssignee assignRoleRoleAssignee = roleAssigneeService.getRoleAssignee(assignRoleUsername);
        assignRole(assignRoleRoleAssignee, roleService.find(assignRoleRoleId));
    }

    private void assignRole(RoleAssignee ra, DataverseRole r) {
        try {
            String privateUrlToken = null;
            commandEngine.submit(new AssignRoleCommand(ra, r, dvObject, dvRequestService.getDataverseRequest(), privateUrlToken));
            JH.addMessage(FacesMessage.SEVERITY_INFO,
                BundleUtil.getStringFromBundle("permission.roleAssignedToOn" ,
                        Arrays.asList( r.getName() , ra.getDisplayInfo().getTitle() , StringEscapeUtils.escapeHtml4(dvObject.getDisplayName()) )) );
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.cannotAssignRole" , Arrays.asList( ex.getMessage())));
        }
    }

    public void revokeRole(Long roleAssignmentId) {
        try {
            commandEngine.submit(new RevokeRoleCommand(em.find(RoleAssignment.class, roleAssignmentId), dvRequestService.getDataverseRequest()));
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("permission.roleRevoked" ));
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.cannotRevokeRole1" , Arrays.asList(ex.getRequiredPermissions().toString())));
            logger.log(Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex);

        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.cannotRevokeRole2" , Arrays.asList( ex.getMessage())));
            logger.log(Level.SEVERE, "Error revoking role assignment: " + ex.getMessage(), ex);
        }
    }

    public String getAssignRoleUsername() {
        return assignRoleUsername;
    }

    public void setAssignRoleUsername(String assignRoleUsername) {
        this.assignRoleUsername = assignRoleUsername;
    }

    public RoleAssignee getAssignRoleRoleAssignee() {
        return assignRoleRoleAssignee;
    }

    public void setAssignRoleRoleAssignee(RoleAssignee assignRoleRoleAssignee) {
        this.assignRoleRoleAssignee = assignRoleRoleAssignee;
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
        return new ArrayList<>();
    }

    public void createNewRole(ActionEvent e) {
        setRole(new DataverseRole());
        role.setOwner(dvObject);
    }
    
    public void cloneRole(String roleId) {
        DataverseRole clonedRole = new DataverseRole();
        clonedRole.setOwner(dvObject);
        
        DataverseRole originalRole = roleService.find(Long.parseLong(roleId));
        clonedRole.addPermissions(originalRole.permissions());
        setRole(clonedRole);
    }
    

    public void editRole(String roleId) {
        setRole(roleService.find(Long.parseLong(roleId)));
    }

    public void updateRole(ActionEvent e) {
        // @todo currently only works for Dataverse since CreateRoleCommand only takes a dataverse
        // we need to decide if we want roles at the dataset level or not
        if (dvObject instanceof Dataverse) {
            role.clearPermissions();
            for (String pmsnStr : getSelectedPermissions()) {
                role.addPermission(Permission.valueOf(pmsnStr));
            }
            try {
                setRole(commandEngine.submit(new CreateRoleCommand(role, dvRequestService.getDataverseRequest(), (Dataverse) role.getOwner())));
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("permission.roleSave" , Arrays.asList( role.getName() )));
            } catch (CommandException ex) {
                JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.cannotSaveRole" , Arrays.asList( ex.getMessage())));
                logger.log(Level.SEVERE, "Saving role failed", ex);
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
