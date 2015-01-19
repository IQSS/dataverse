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
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class ManagePermissionsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ManagePermissionsPage.class.getCanonicalName());

    @EJB
    DvObjectServiceBean dvObjectService;
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

    DvObject dvObject = new Dataverse(); // by default we use a Dataverse, but this will be overridden in init by the findById

    public DvObject getDvObject() {
        return dvObject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
        /*if (dvObject instanceof DvObjectContainer) {
         inheritAssignments = !((DvObjectContainer) dvObject).isPermissionRoot();
         }*/
    }

    public String init() {
        //@todo deal with any kind of dvObject
        if (dvObject.getId() != null) {
            dvObject = dvObjectService.findDvObject(dvObject.getId());
        }

        // check if dvObject exists and user has permission
        if (dvObject == null) {
            return "/404.xhtml";
        }

        // for dataFiles, check the perms on its owning dataset
        DvObject checkPermissionsdvObject = dvObject instanceof DataFile ? dvObject.getOwner() : dvObject;
        if (!permissionService.on(checkPermissionsdvObject).has(checkPermissionsdvObject instanceof Dataverse ? Permission.ManageDataversePermissions : Permission.ManageDatasetPermissions)) {
            return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
        }

        // initialize the configure settings
        if (dvObject instanceof Dataverse) {
            initAccessSettings();
        }
        return "";
    }

    /* 
     main page - role assignment table
     */
  
    public List<RoleAssignmentRow> getRoleAssignments() {
        List<RoleAssignmentRow> raList = null;
        if (dvObject != null && dvObject.getId() != null) {
            Set<RoleAssignment> ras = roleService.rolesAssignments(dvObject);
            raList = new ArrayList<>(ras.size());
            for (RoleAssignment ra : ras) {
                // for files, only show role assignments which can download
                if (!(dvObject instanceof DataFile) || ra.getRole().permissions().contains(Permission.DownloadFile)) {
                    raList.add(new RoleAssignmentRow(ra, roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo()));
                }
            }
        }
        return raList;
    }
    
    public void removeRoleAssignment(Long roleAssignmentId) {
        revokeRole(roleAssignmentId);
        
        initAccessSettings(); // in case the revoke was for the AuthenticatedUsers group
        showAssignmentMessages();        
    }
    
    // internal method used by removeRoleAssignment and saveConfiguration
    private void revokeRole(Long roleAssignmentId) {
        try {
            RoleAssignment ra = em.find(RoleAssignment.class, roleAssignmentId);
            commandEngine.submit(new RevokeRoleCommand(ra, session.getUser()));
            JsfHelper.addSuccessMessage(ra.getRole().getName() + " role for " + roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo().getTitle() + " was removed.");
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "The role assignment was not able to be removed.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role assignment could not be removed.");
            logger.log(Level.SEVERE, "Error removing role assignment: " + ex.getMessage(), ex);
        }
    }
    
    /* 
     main page - roles table
     */    

    public List<DataverseRole> getRoles() {
        if (dvObject != null && dvObject.getId() != null) {
            return roleService.findByOwnerId(dvObject.getId());
        }
        return new ArrayList();
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
    
    /*
     edit configuration dialog
     */
    
    private String authenticatedUsersContributorRoleAlias = null;
    private String defaultContributorRoleAlias = DataverseRole.EDITOR;

    public String getAuthenticatedUsersContributorRoleAlias() {
        return authenticatedUsersContributorRoleAlias;
    }

    public void setAuthenticatedUsersContributorRoleAlias(String authenticatedUsersContributorRoleAlias) {
        this.authenticatedUsersContributorRoleAlias = authenticatedUsersContributorRoleAlias;
    }

    public String getDefaultContributorRoleAlias() {
        return defaultContributorRoleAlias;
    }

    public void setDefaultContributorRoleAlias(String defaultContributorRoleAlias) {
        this.defaultContributorRoleAlias = defaultContributorRoleAlias;
    }    
    
   public void initAccessSettings() {       
        authenticatedUsersContributorRoleAlias = "";

        List<RoleAssignment> aUsersRoleAssignments = roleService.directRoleAssignments(AuthenticatedUsers.get(), dvObject);
        for (RoleAssignment roleAssignment : aUsersRoleAssignments) {
            String roleAlias = roleAssignment.getRole().getAlias();
            authenticatedUsersContributorRoleAlias = roleAlias;
            break;
            // @todo handle case where more than one role has been assigned to the AutenticatedUsers group!
        }
        
        defaultContributorRoleAlias = ((Dataverse) dvObject).getDefaultContributorRole().getAlias();      
    }
   
    
   public void saveConfiguration(ActionEvent e) {
        // Set role (if any) for authenticatedUsers
        DataverseRole roleToAssign = null;
        List<String> contributorRoles = Arrays.asList(DataverseRole.FULL_CONTRIBUTOR, DataverseRole.DV_CONTRIBUTOR, DataverseRole.DS_CONTRIBUTOR);

        if (!StringUtil.isEmpty(authenticatedUsersContributorRoleAlias)) {
            roleToAssign = roleService.findBuiltinRoleByAlias(authenticatedUsersContributorRoleAlias);
        }

        // then, check current contributor role
        List<RoleAssignment> aUsersRoleAssignments = roleService.directRoleAssignments(AuthenticatedUsers.get(), dvObject);
        for (RoleAssignment roleAssignment : aUsersRoleAssignments) {
            DataverseRole currentRole = roleAssignment.getRole();
            if (contributorRoles.contains(currentRole.getAlias())) {
                if (currentRole.equals(roleToAssign)) {
                    roleToAssign = null; // found the role, so no need to assign
                } else {
                    revokeRole(roleAssignment.getId());
                }
            }
        }
        // finally, assign role, if new
        if (roleToAssign != null) {
            assignRole(AuthenticatedUsers.get(), roleToAssign);
        }

        // set dataverse default contributor role
        if (dvObject instanceof Dataverse) {
            Dataverse dv = (Dataverse) dvObject;
            DataverseRole defaultRole = roleService.findBuiltinRoleByAlias(defaultContributorRoleAlias);
            if (!defaultRole.equals(dv.getDefaultContributorRole())) {
                try {
                    commandEngine.submit(new UpdateDataverseDefaultContributorRoleCommand(defaultRole, session.getUser(), dv));
                    JsfHelper.addSuccessMessage("The default permissions for this dataverse have been updated.");
                } catch (PermissionException ex) {
                    JH.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot assign default permissions.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
                } catch (CommandException ex) {
                    JH.addMessage(FacesMessage.SEVERITY_FATAL, "Cannot assign default permissions.");
                    logger.log(Level.SEVERE, "Error assigning default permissions: " + ex.getMessage(), ex);
                }
            }
        }
        showConfigureMessages();
    }    

    /*
     assign roles dialog
     */
    private List<RoleAssignee> selectedRoleAssignees;
    private Long selectedRoleId;
    private List<RoleAssignee> roleAssigneeList = new ArrayList();

    public List<RoleAssignee> getSelectedRoleAssignees() {
        return selectedRoleAssignees;
    }

    public void setSelectedRoleAssignees(List<RoleAssignee> selectedRoleAssignees) {
        this.selectedRoleAssignees = selectedRoleAssignees;
    }

    public Long getSelectedRoleId() {
        return selectedRoleId;
    }

    public void setSelectedRoleId(Long selectedRoleId) {
        this.selectedRoleId = selectedRoleId;
    }

    public void initAssigneeDialog(ActionEvent ae) {
        selectedRoleAssignees = null;
        selectedRoleId = null;
        showNoMessages();
    }

    public List<RoleAssignee> completeRoleAssignee(String query) {
        if (roleAssigneeList.isEmpty()) {
            for (AuthenticatedUser au : authenticationService.findAllAuthenticatedUsers()) {
                roleAssigneeList.add(au);
            }
        }
        List<RoleAssignee> returnList = new ArrayList();
        for (RoleAssignee ra : roleAssigneeList) {
            // @todo unsure if containsIgnore case will work for all locales
            if (StringUtils.containsIgnoreCase(ra.getDisplayInfo().getTitle(), query) && (selectedRoleAssignees == null || !selectedRoleAssignees.contains(ra))) {
                returnList.add(ra);
            }
        }
        return returnList;
    }

    public List<DataverseRole> getAvailableRoles() {
        List<DataverseRole> roles = new LinkedList<>();
        if (dvObject != null && dvObject.getId() != null && (dvObject instanceof Dataverse || dvObject instanceof Dataset)) {
            // current the available roles for a dataset are gotten from its parent
            Dataverse dv = dvObject instanceof Dataverse ? (Dataverse) dvObject : ((Dataset) dvObject).getOwner();

            roles.addAll(roleService.availableRoles(dv.getId()));

            Collections.sort(roles, DataverseRole.CMP_BY_NAME);
        }
        return roles;
    }

    public DataverseRole getAssignedRole() {
        if (selectedRoleId != null) {
            return roleService.find(selectedRoleId);
        }
        return null;
    }

    public void assignRole(ActionEvent evt) {        
        for (RoleAssignee roleAssignee : selectedRoleAssignees) {
            assignRole(roleAssignee, roleService.find(selectedRoleId));
        }
    }

    private void assignRole(RoleAssignee ra, DataverseRole r) {
        try {
            commandEngine.submit(new AssignRoleCommand(ra, r, dvObject, session.getUser()));
            JsfHelper.addSuccessMessage(r.getName() + " role assigned to " + ra.getDisplayInfo().getTitle() + " for " + dvObject.getDisplayName() + ".");
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "The role was not able to be assigned.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role was not able to be assigned.");
            logger.log(Level.SEVERE, "Error assiging role: " + ex.getMessage(), ex);
        }
        
        showAssignmentMessages();
    }

    /*
     edit role dialog
     */
    private DataverseRole role = new DataverseRole();
    private List<String> selectedPermissions;

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

    public List<Permission> getPermissions() {
        return Arrays.asList(Permission.values());
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
                String roleState = role.getId() != null ? "updated" : "created";
                setRole(commandEngine.submit(new CreateRoleCommand(role, session.getUser(), (Dataverse) role.getOwner())));
                JsfHelper.addSuccessMessage("The role was " + roleState + ". To assign it to a user and/or group, click on the Assign Roles to Users/Groups button in the Users/Groups section of this page.");
            } catch (PermissionException ex) {
                JH.addMessage(FacesMessage.SEVERITY_ERROR, "The role was not able to be saved.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
            } catch (CommandException ex) {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role was not able to be saved.");
                logger.log(Level.SEVERE, "Error saving role: " + ex.getMessage(), ex);
            }
        }
        showRoleMessages();
    }

    // currently not used
    public void toggleFileRestrict(ActionEvent evt) {
        DataFile file = (DataFile) dvObject;
        file.setRestricted(!file.isRestricted());
    }

    public void grantAccess(ActionEvent evt) {
        // Find the built in file downloader role (currently by alias) 
        for (RoleAssignee roleAssignee : selectedRoleAssignees) {
            assignRole(roleAssignee, roleService.findBuiltinRoleByAlias("filedownloader"));
        }
    }

    boolean renderConfigureMessages = false;
    boolean renderAssignmentMessages = false;
    boolean renderRoleMessages = false;

    private void showNoMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = false;
        renderRoleMessages = false;
    }    
    
    private void showConfigureMessages() {
        renderConfigureMessages = true;
        renderAssignmentMessages = false;
        renderRoleMessages = false;
    }
    
    private void showAssignmentMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = true;
        renderRoleMessages = false;
    }
    
    private void showRoleMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = false;
        renderRoleMessages = true;
    }    

    public Boolean getRenderConfigureMessages() {
        return renderConfigureMessages;
    }

    public void setRenderConfigureMessages(Boolean renderConfigureMessages) {
        this.renderConfigureMessages = renderConfigureMessages;
    }

    public Boolean getRenderAssignmentMessages() {
        return renderAssignmentMessages;
    }

    public void setRenderAssignmentMessages(Boolean renderAssignmentMessages) {
        this.renderAssignmentMessages = renderAssignmentMessages;
    }

    public Boolean getRenderRoleMessages() {
        return renderRoleMessages;
    }

    public void setRenderRoleMessages(Boolean renderRoleMessages) {
        this.renderRoleMessages = renderRoleMessages;
    }

    // inner class used fordisplay of role assignments
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
}
