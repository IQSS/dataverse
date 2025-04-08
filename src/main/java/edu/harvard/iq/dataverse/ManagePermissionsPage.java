
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.text.StringEscapeUtils;

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
    FileAccessRequestServiceBean fileAccessRequestService;
    @EJB
    DataverseRoleServiceBean roleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    ExplicitGroupServiceBean explicitGroupService;
    @EJB
    GroupServiceBean groupService;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;


    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataverseSession session;

    private DataverseRolePermissionHelper dataverseRolePermissionHelper;
    private List<DataverseRole> roleList;

    DvObject dvObject = new Dataverse(); // by default we use a Dataverse, but this will be overridden in init by the findById

    public DvObject getDvObject() {
        return dvObject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
        /*
        SEK 09/15/2016 - may need to do something here if permissions are transmitted/inherited from dataverse to dataverse
        */

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
            return permissionsWrapper.notFound();
        }

        // for dataFiles, check the perms on its owning dataset
        DvObject checkPermissionsdvObject = dvObject instanceof DataFile ? dvObject.getOwner() : dvObject;
        if (!permissionService.on(checkPermissionsdvObject).has(checkPermissionsdvObject instanceof Dataverse ? Permission.ManageDataversePermissions : Permission.ManageDatasetPermissions)) {
            return permissionsWrapper.notAuthorized();
        }

        // initialize the configure settings
        if (dvObject instanceof Dataverse) {
            initAccessSettings();
        }
        roleList = roleService.findAll();
        roleAssignments = initRoleAssignments();
        dataverseRolePermissionHelper = new DataverseRolePermissionHelper(roleList);
        return "";
    }

    /*
     main page - role assignment table
     */

    // used by remove Role Assignment
    private RoleAssignment selectedRoleAssignment;

    public RoleAssignment getSelectedRoleAssignment() {
        return selectedRoleAssignment;
    }

    public void setSelectedRoleAssignment(RoleAssignment selectedRoleAssignment) {
        this.selectedRoleAssignment = selectedRoleAssignment;
    }

    private List<RoleAssignmentRow> roleAssignments;

    public List<RoleAssignmentRow> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(List<RoleAssignmentRow> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<RoleAssignmentRow> initRoleAssignments() {
        List<RoleAssignmentRow> raList = null;
        if (dvObject != null && dvObject.getId() != null) {
            Set<RoleAssignment> ras = roleService.rolesAssignments(dvObject);
            List<DataverseRole> availableRoles = getAvailableRoles();
            raList = new ArrayList<>(ras.size());
            for (RoleAssignment roleAssignment : ras) {
                // only show roles that are available for this DVObject
                if (availableRoles.contains(roleAssignment.getRole())) {
                    RoleAssignee roleAssignee = roleAssigneeService.getRoleAssignee(roleAssignment.getAssigneeIdentifier());
                    if (roleAssignee != null) {
                        raList.add(new RoleAssignmentRow(roleAssignment, roleAssignee.getDisplayInfo()));
                    } else {
                        logger.info("Could not find role assignee based on role assignment id " + roleAssignment.getId());
                    }
                }
            }
        }
        return raList;
    }

    public void removeRoleAssignment() {
        revokeRole(selectedRoleAssignment);

        if (dvObject instanceof Dataverse) {
            initAccessSettings(); // in case the revoke was for the AuthenticatedUsers group
        }
        roleAssignments = initRoleAssignments();
        showAssignmentMessages();
    }

    // internal method used by removeRoleAssignment and saveConfiguration
    private void revokeRole(RoleAssignment ra) {
        try {
            commandEngine.submit(new RevokeRoleCommand(ra, dvRequestService.getDataverseRequest()));
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.roleWasRemoved", Arrays.asList(ra.getRole().getName(), roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo().getTitle())));
            RoleAssignee assignee = roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier());
            notifyRoleChange(assignee, UserNotification.Type.REVOKEROLE);
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"), BundleUtil.getStringFromBundle("permission.permissionsMissing", Arrays.asList(ex.getRequiredPermissions().toString())));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"));
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
    
    /** Role Assignment History */
    private List<RoleAssignmentHistoryEntry> roleAssignmentHistory;

    public List<RoleAssignmentHistoryEntry> getRoleAssignmentHistory() {
        if (roleAssignmentHistory == null) {
            roleAssignmentHistory = new ArrayList<>();
            
            List<RoleAssignmentAudit> audits = em.createNamedQuery("RoleAssignmentAudit.findByDefinitionPointId", RoleAssignmentAudit.class)
                    .setParameter("definitionPointId", dvObject.getId())
                    .getResultList();
            
            Map<Long, RoleAssignmentHistoryEntry> historyMap = new HashMap<>();
            
            for (RoleAssignmentAudit audit : audits) {
                Long roleAssignmentId = audit.getRoleAssignmentId();
                RoleAssignmentHistoryEntry entry = historyMap.get(roleAssignmentId);
                
                if (entry == null) {
                    entry = new RoleAssignmentHistoryEntry(audit.getAssigneeIdentifier(), audit.getRoleAlias());
                    historyMap.put(roleAssignmentId, entry);
                }
                
                if (audit.getActionType() == RoleAssignmentAudit.ActionType.ASSIGN) {
                    entry.setAssignedBy(audit.getActionByIdentifier());
                    entry.setAssignedAt(audit.getActionTimestamp());
                } else if (audit.getActionType() == RoleAssignmentAudit.ActionType.REVOKE) {
                    entry.setRevokedBy(audit.getActionByIdentifier());
                    entry.setRevokedAt(audit.getActionTimestamp());
                }
            }
            
            roleAssignmentHistory.addAll(historyMap.values());
            roleAssignmentHistory.sort(Comparator.comparing(RoleAssignmentHistoryEntry::getAssignedAt).reversed());
        }
        return roleAssignmentHistory;
    }
    

    /*
    ============================================================================
     edit configuration dialog // only for dataverse version of page
    ============================================================================
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
    
    public Boolean isCustomDefaultContributorRole() {
        if (defaultContributorRoleAlias == null) {
            initAccessSettings();
        }
        return !(defaultContributorRoleAlias.equals(DataverseRole.EDITOR) || defaultContributorRoleAlias.equals(DataverseRole.CURATOR));
    }
    
    public String getCustomDefaultContributorRoleName(){
        if (dvObject instanceof Dataverse && isCustomDefaultContributorRole() ){           
            return defaultContributorRoleAlias.equals(DataverseRole.NONE) ? BundleUtil.getStringFromBundle("permission.default.contributor.role.none.name") : roleService.findCustomRoleByAliasAndOwner(defaultContributorRoleAlias,dvObject.getId()).getName();
        } else {
            return "";
        }
    }
    
    public String getCustomDefaultContributorRoleAlias(){
        if (dvObject instanceof Dataverse && isCustomDefaultContributorRole()){
            return defaultContributorRoleAlias.equals(DataverseRole.NONE) ? DataverseRole.NONE : roleService.findCustomRoleByAliasAndOwner(defaultContributorRoleAlias,dvObject.getId()).getAlias();
        } else {
            return "";
        }
    }
    
    public void  setCustomDefaultContributorRoleAlias(String dummy){
        //dummy method for interface
    }
    
    public void  setCustomDefaultContributorRoleName(String dummy){
        //dummy method for interface
    }
    
    public String getCustomDefaultContributorRoleDescription(){
        if (dvObject instanceof Dataverse  && isCustomDefaultContributorRole()){
            return defaultContributorRoleAlias.equals(DataverseRole.NONE) ? BundleUtil.getStringFromBundle("permission.default.contributor.role.none.decription" ) :roleService.findCustomRoleByAliasAndOwner(defaultContributorRoleAlias,dvObject.getId() ).getDescription();
        } else {
            return "";
        }
    }
    
    public void  setCustomDefaultContributorRoleDescription(String dummy){
        //dummy method for interface
    }

    public void setDefaultContributorRoleAlias(String defaultContributorRoleAlias) {
        this.defaultContributorRoleAlias = defaultContributorRoleAlias;
    }

    public void initAccessSettings() {
        if (dvObject instanceof Dataverse) {
            authenticatedUsersContributorRoleAlias = "";

            List<RoleAssignment> aUsersRoleAssignments = roleService.directRoleAssignments(AuthenticatedUsers.get(), dvObject);
            for (RoleAssignment roleAssignment : aUsersRoleAssignments) {
                String roleAlias = roleAssignment.getRole().getAlias();
                authenticatedUsersContributorRoleAlias = roleAlias;
                break;
                // @todo handle case where more than one role has been assigned to the AutenticatedUsers group!
            }

            defaultContributorRoleAlias = ((Dataverse) dvObject).getDefaultContributorRole() == null ? DataverseRole.NONE : ((Dataverse) dvObject).getDefaultContributorRole().getAlias();
        } else {
            //There are only default roles assigned at the dataverse level
            defaultContributorRoleAlias = DataverseRole.NONE;
        }
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
                    revokeRole(roleAssignment);
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
                    commandEngine.submit(new UpdateDataverseDefaultContributorRoleCommand(defaultRole, dvRequestService.getDataverseRequest(), dv));
                    JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.defaultPermissionDataverseUpdated"));
                } catch (PermissionException ex) {
                    JH.addMessage(FacesMessage.SEVERITY_ERROR,  BundleUtil.getStringFromBundle("permission.CannotAssigntDefaultPermissions"),
                            BundleUtil.getStringFromBundle("permission.permissionsMissing" , Arrays.asList(ex.getRequiredPermissions().toString())));

                } catch (CommandException ex) {
                    JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.CannotAssigntDefaultPermissions"));
                    logger.log(Level.SEVERE, "Error assigning default permissions: " + ex.getMessage(), ex);
                }
            }
        }
        roleAssignments = initRoleAssignments();
        showConfigureMessages();
    }

    /*
   ============================================================================
     assign roles dialog
   ============================================================================
     */
    private List<RoleAssignee> roleAssignSelectedRoleAssignees;
    private Long selectedRoleId;

    public List<RoleAssignee> getRoleAssignSelectedRoleAssignees() {
        return roleAssignSelectedRoleAssignees;
    }

    public void setRoleAssignSelectedRoleAssignees(List<RoleAssignee> selectedRoleAssignees) {
        this.roleAssignSelectedRoleAssignees = selectedRoleAssignees;
    }

    public Long getSelectedRoleId() {
        return selectedRoleId;
    }

    public void setSelectedRoleId(Long selectedRoleId) {
        this.selectedRoleId = selectedRoleId;
    }

    public void initAssigneeDialog(ActionEvent ae) {
        roleAssignSelectedRoleAssignees = new LinkedList<>();
        selectedRoleId = null;
        showNoMessages();
    }

    public List<RoleAssignee> completeRoleAssignee( String query ) {
        return roleAssigneeService.filterRoleAssignees(query, dvObject, roleAssignSelectedRoleAssignees);
    }

    public List<DataverseRole> getAvailableRoles() {
        List<DataverseRole> roles = new LinkedList<>();
        if (dvObject != null && dvObject.getId() != null) {

            if (dvObject instanceof Dataverse) {
                roles.addAll(roleService.availableRoles(dvObject.getId()));

            } else if (dvObject instanceof Dataset) {
                // don't show roles that only have Dataverse level permissions
                // current the available roles for a dataset are gotten from its parent
                for (DataverseRole role : roleService.availableRoles(dvObject.getOwner().getId())) {
                    for (Permission permission : role.permissions()) {
                        if (permission.appliesTo(Dataset.class) || permission.appliesTo(DataFile.class)) {
                            roles.add(role);
                            break;
                        }
                    }
                }

            } else if (dvObject instanceof DataFile) {
                // only show roles that have File level permissions
                // current the available roles for a file are gotten from its parent's parent                
                for (DataverseRole role : roleService.availableRoles(dvObject.getOwner().getOwner().getId())) {
                    for (Permission permission : role.permissions()) {
                        if (permission.appliesTo(DataFile.class)) {
                            roles.add(role);
                            break;
                        }
                    }
                }
            }

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

    public String getAssignedRoleObjectTypes(){
        String retString = "";
        if (selectedRoleId != null) {
            /* SEK 09/15/2016 SEK commenting out for now
                because permissions are not inherited

            if (dataverseRolePermissionHelper.hasDataversePermissions(selectedRoleId) && dvObject instanceof Dataverse){
                String dvLabel = BundleUtil.getStringFromBundle("dataverses");
                retString = dvLabel;
            }
            */
            if (dataverseRolePermissionHelper.hasDatasetPermissions(selectedRoleId) && dvObject instanceof Dataverse){
                String dsLabel = BundleUtil.getStringFromBundle("datasets");
                if(!retString.isEmpty()) {
                    retString +=", " +  dsLabel;
                } else {
                    retString = dsLabel;
                }

            }
            if (dataverseRolePermissionHelper.hasFilePermissions(selectedRoleId)){
                String filesLabel = BundleUtil.getStringFromBundle("files");
                if(!retString.isEmpty()) {
                    retString +=", " + filesLabel;
                } else {
                    retString = filesLabel;
                }
            }
            return retString;
        }
        return null;
    }

    public String getDefinitionLevelString(){
        if (dvObject != null){
            if (dvObject instanceof Dataverse) return BundleUtil.getStringFromBundle("dataverse");
            if (dvObject instanceof Dataset) return BundleUtil.getStringFromBundle("dataset");
        }
        return null;
    }

    public void assignRole(ActionEvent evt) {
        logger.info("Got to assignRole");
        List<RoleAssignee> selectedRoleAssigneesList = getRoleAssignSelectedRoleAssignees();
        if ( selectedRoleAssigneesList == null ) {
            logger.info("** SELECTED role asignees is null");
            selectedRoleAssigneesList = new LinkedList<>();
        }
        for (RoleAssignee roleAssignee : selectedRoleAssigneesList) {
            assignRole(roleAssignee, roleService.find(selectedRoleId));
        }
        roleAssignments = initRoleAssignments();
    }

    /**
     * Notify a {@code RoleAssignee} that a role was either assigned or revoked.
     * Will notify all members of a group.
     * @param ra The {@code RoleAssignee} to be notified.
     * @param type The type of notification.
     */
    private void notifyRoleChange(RoleAssignee ra, UserNotification.Type type) {
        if (ra instanceof AuthenticatedUser) {
            userNotificationService.sendNotification((AuthenticatedUser) ra, new Timestamp(new Date().getTime()), type, dvObject.getId());
        } else if (ra instanceof ExplicitGroup) {
            ExplicitGroup eg = (ExplicitGroup) ra;
            Set<String> explicitGroupMembers = eg.getContainedRoleAssgineeIdentifiers();
            for (String id : explicitGroupMembers) {
                RoleAssignee explicitGroupMember = roleAssigneeService.getRoleAssignee(id);
                if (explicitGroupMember instanceof AuthenticatedUser) {
                    userNotificationService.sendNotification((AuthenticatedUser) explicitGroupMember, new Timestamp(new Date().getTime()), type, dvObject.getId());
                }
            }
        }
    }

    private void assignRole(RoleAssignee ra, DataverseRole r) {
        try {
            String privateUrlToken = null;
            commandEngine.submit(new AssignRoleCommand(ra, r, dvObject, dvRequestService.getDataverseRequest(), privateUrlToken));
            List<String> args = Arrays.asList(
                    r.getName(),
                    ra.getDisplayInfo().getTitle(),
                    StringEscapeUtils.escapeHtml4(dvObject.getDisplayName())
            );
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.roleAssignedToFor", args));
            // don't notify if role = file downloader and object is not released
            if (!(r.getAlias().equals(DataverseRole.FILE_DOWNLOADER) && !dvObject.isReleased()) ){
                notifyRoleChange(ra, UserNotification.Type.ASSIGNROLE);
            }

        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeAssigned"), BundleUtil.getStringFromBundle("permission.permissionsMissing", Arrays.asList(ex.getRequiredPermissions().toString())));
        } catch (CommandException ex) {
            List<String> args = Arrays.asList(
                    r.getName(),
                    ra.getDisplayInfo().getTitle(),
                    StringEscapeUtils.escapeHtml4(dvObject.getDisplayName())
            );
            String message = BundleUtil.getStringFromBundle("permission.roleNotAssignedFor", args);
            JsfHelper.addErrorMessage(message);
            //JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role was not able to be assigned.");
            logger.log(Level.SEVERE, "Error assiging role: " + ex.getMessage(), ex);
        }

        showAssignmentMessages();
    }

    /*
    ============================================================================
     edit role dialog
    ============================================================================
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
                String roleState = role.getId() != null ? BundleUtil.getStringFromBundle("permission.updated") : BundleUtil.getStringFromBundle("permission.created");
                setRole(commandEngine.submit(new CreateRoleCommand(role, dvRequestService.getDataverseRequest(), (Dataverse) role.getOwner())));
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.roleWas", Arrays.asList(roleState)));
            } catch (PermissionException ex) {
                JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotSaved"), BundleUtil.getStringFromBundle("permission.permissionsMissing", Arrays.asList(ex.getRequiredPermissions().toString())));
            } catch (CommandException ex) {
                JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotSaved").concat(" " + ex.getMessage()) );
                logger.log(Level.SEVERE, "Error saving role: " + ex.getMessage(), ex);
            }
        }
        showRoleMessages();
    }


    public DataverseRolePermissionHelper getDataverseRolePermissionHelper() {
        return dataverseRolePermissionHelper;
    }

    public void setDataverseRolePermissionHelper(DataverseRolePermissionHelper dataverseRolePermissionHelper) {
        this.dataverseRolePermissionHelper = dataverseRolePermissionHelper;
    }

    /*
    ============================================================================
    Internal methods
    ============================================================================
    */

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

    // inner class used for display of role assignments
    public static class RoleAssignmentRow {

        private final RoleAssigneeDisplayInfo assigneeDisplayInfo;
        private final RoleAssignment ra;

        public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf) {
            ra = anRa;
            assigneeDisplayInfo = disInf;
        }

        public RoleAssignment getRoleAssignment() {
            return ra;
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
    
    public static class RoleAssignmentHistoryEntry {
        private String roleName;
        private String assigneeIdentifier;
        private String assignedBy;
        private Date assignedAt;
        private String revokedBy;
        private Date revokedAt;

        public RoleAssignmentHistoryEntry(String assigneeIdentifier, String roleName) {
            this.roleName = roleName;
            this.assigneeIdentifier = assigneeIdentifier;
            ;
        }

        public void setRevokedAt(Date actionTimestamp) {
            revokedAt = actionTimestamp;

        }

        public void setRevokedBy(String actionByIdentifier) {
            revokedBy = actionByIdentifier;

        }

        public void setAssignedAt(Date actionTimestamp) {
            assignedAt = actionTimestamp;

        }

        public void setAssignedBy(String actionByIdentifier) {
            assignedBy = actionByIdentifier;

        }

        public String getRoleName() {
            return roleName;
        }

        public String getAssigneeIdentifier() {
            return assigneeIdentifier;
        }

        public String getAssignedBy() {
            return assignedBy;
        }

        public Date getAssignedAt() {
            return assignedAt;
        }

        public String getRevokedBy() {
            return revokedBy;
        }

        public Date getRevokedAt() {
            return revokedAt;
        }
    }
}
