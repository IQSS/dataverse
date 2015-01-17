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
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class ManageFilePermissionsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ManageFilePermissionsPage.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;
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

    Dataset dataset = new Dataset(); 
    private Map<RoleAssignee,List<RoleAssignmentRow>> roleAssigneeMap = new HashMap();
    private Map<FileMetadata,List<RoleAssignmentRow>> fileMap = new HashMap();

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    
    public Map<RoleAssignee, List<RoleAssignmentRow>> getRoleAssigneeMap() {
        return roleAssigneeMap;
    }

    public void setRoleAssigneeMap(Map<RoleAssignee, List<RoleAssignmentRow>> roleAssigneeMap) {
        this.roleAssigneeMap = roleAssigneeMap;
    }

    public Map<FileMetadata, List<RoleAssignmentRow>> getFileMap() {
        return fileMap;
    }

    public void setFileMap(Map<FileMetadata, List<RoleAssignmentRow>> fileMap) {
        this.fileMap = fileMap;
    }    


    public String init() {
        if (dataset.getId() != null) {
            dataset = datasetService.find(dataset.getId());
        }

        // check if dvObject exists and user has permission
        if (dataset == null) {
            return "/404.xhtml";
        }

        if (!permissionService.on(dataset).has(Permission.ManageDatasetPermissions)) {
            return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
        }
        
        initMaps();
        
        return "";
    }
    
    private void initMaps() {
        // initialize files and usergroup list
        roleAssigneeMap.clear();
        fileMap.clear();
               
        for (FileMetadata fmd : dataset.getLatestVersion().getFileMetadatas()) {
            Set<RoleAssignment> ras = roleService.rolesAssignments(fmd.getDataFile());
            List raList = new ArrayList<>(ras.size());
            for (RoleAssignment ra : ras) {
                // for files, only show role assignments which can download
                if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                    raList.add(new RoleAssignmentRow(ra, roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo(), fmd.getDataFile()));                   
                    addFileToRoleAssignee(ra, fmd);                    
                }
            fileMap.put(fmd, raList);    
            }            
        }        
    }
    private void addFileToRoleAssignee(RoleAssignment assignment, FileMetadata fmd) {
        RoleAssignee ra = roleAssigneeService.getRoleAssignee(assignment.getAssigneeIdentifier());
        List<RoleAssignmentRow> assignments = roleAssigneeMap.get(ra);
        if (assignments == null) {
            assignments = new ArrayList();
            roleAssigneeMap.put(ra, assignments);
        }
        assignments.add(new RoleAssignmentRow(assignment, ra.getDisplayInfo(), fmd.getDataFile()));
    }

    /* 
     main page
     */


    /*
     view / remove roles dialog
     */  
    private DataFile selectedFile;
    private RoleAssignee selectedRoleAssignee;
    private List<RoleAssignmentRow> roleAssignments;
    private List<RoleAssignmentRow> selectedRoleAssignmentRows;

    public DataFile getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(DataFile selectedFile) {
        this.selectedFile = selectedFile;
    }

    public RoleAssignee getSelectedRoleAssignee() {
        return selectedRoleAssignee;
    }

    public void setSelectedRoleAssignee(RoleAssignee selectedRoleAssignee) {
        this.selectedRoleAssignee = selectedRoleAssignee;
    }
    
    public List<RoleAssignmentRow> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(List<RoleAssignmentRow> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<RoleAssignmentRow> getSelectedRoleAssignmentRows() {
        return selectedRoleAssignmentRows;
    }

    public void setSelectedRoleAssignmentRows(List<RoleAssignmentRow> selectedRoleAssignmentRows) {
        this.selectedRoleAssignmentRows = selectedRoleAssignmentRows;
    }
    
    public void initViewRemoveDialogByFile(DataFile file, List<RoleAssignmentRow> raRows) {
        this.selectedFile = file;
        this.selectedRoleAssignee = null;
        this.roleAssignments = raRows;
        showFileMessages();
    }
    
    public void initViewRemoveDialogByRoleAssignee(RoleAssignee ra, List<RoleAssignmentRow> raRows) {
        this.selectedFile = null;
        this.selectedRoleAssignee = ra;
        this.roleAssignments = raRows;
        showUserGroupMessages();
    }    
    
    public void removeRoleAssignments() {
        for (RoleAssignmentRow raRow : selectedRoleAssignmentRows) {
            revokeRole(raRow.getId());
        }
        
        initMaps();        
    }
    
    // internal method used by removeRoleAssignments
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
     assign roles dialog
     */
    private List<RoleAssignee> selectedRoleAssignees;
    private List<FileMetadata> selectedFiles;
    private List<RoleAssignee> roleAssigneeList = new ArrayList();

    public List<RoleAssignee> getSelectedRoleAssignees() {
        return selectedRoleAssignees;
    }

    public void setSelectedRoleAssignees(List<RoleAssignee> selectedRoleAssignees) {
        this.selectedRoleAssignees = selectedRoleAssignees;
    }

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }


    public void initAssignDialog(ActionEvent ae) {
        selectedRoleAssignees = null;
        selectedFiles.clear();
        showUserGroupMessages();
    }
    
    public void initPreselectedAssignDialog(FileMetadata fmd) {
        selectedRoleAssignees = null;
        selectedFiles.clear();
        selectedFiles.add(fmd);
        showFileMessages();
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
    
    public void grantAccess(ActionEvent evt) {
        // Find the built in file downloader role (currently by alias) 
        for (RoleAssignee roleAssignee : selectedRoleAssignees) {
            for (FileMetadata fmd : selectedFiles) {
                assignRole(roleAssignee, fmd.getDataFile(), roleService.findBuiltinRoleByAlias("filedownloader"));                
            }
        }
        
        initMaps();
    }

    private void assignRole(RoleAssignee ra,  DataFile file, DataverseRole r) {
        try {
            commandEngine.submit(new AssignRoleCommand(ra, r, file, session.getUser()));
            JsfHelper.addSuccessMessage(r.getName() + " role assigned to " + ra.getDisplayInfo().getTitle() + " for " + file.getDisplayName() + ".");
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "The role was not able to be assigned.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role was not able to be assigned.");
            logger.log(Level.SEVERE, "Error assiging role: " + ex.getMessage(), ex);
        }
    }


    boolean renderUserGroupMessages = false;
    boolean renderFileMessages = false;
       
    private void showUserGroupMessages() {
        renderUserGroupMessages = true;
        renderFileMessages = false;
    }

    private void showFileMessages() {
        renderUserGroupMessages = false;
        renderFileMessages = true;
    }    
    
    public boolean isRenderUserGroupMessages() {
        return renderUserGroupMessages;
    }

    public void setRenderUserGroupMessages(boolean renderUserGroupMessages) {
        this.renderUserGroupMessages = renderUserGroupMessages;
    }

    public boolean isRenderFileMessages() {
        return renderFileMessages;
    }

    public void setRenderFileMessages(boolean renderFileMessages) {
        this.renderFileMessages = renderFileMessages;
    }





    // inner class used fordisplay of role assignments
    public static class RoleAssignmentRow {

        private final RoleAssigneeDisplayInfo assigneeDisplayInfo;
        private final RoleAssignment ra;
        private final DvObject assignmentPoint;

        public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf, DvObject assignmentPoint) {
            this.ra = anRa;
            this.assigneeDisplayInfo = disInf;
            this.assignmentPoint = assignmentPoint;
        }        
        

        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return assigneeDisplayInfo;
        }

        public DvObject getDefinitionPoint() {
            return ra.getDefinitionPoint();
        }

        public DvObject getAssignmentPoint() {
            return assignmentPoint;
        }
        
        public Long getId() {
            return ra.getId();
        }

    }   
}
