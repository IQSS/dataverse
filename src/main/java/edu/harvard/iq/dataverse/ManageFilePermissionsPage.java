/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.ManagePermissionsPage.RoleAssignmentHistoryEntry;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DateUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.ObjectUtils;

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
    DataFileServiceBean datafileService;
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
    UserNotificationServiceBean userNotificationService;    
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @EJB
    FileAccessRequestServiceBean fileAccessRequestService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataverseSession session;

    Dataset dataset = new Dataset(); 
    private final TreeMap<RoleAssignee,List<RoleAssignmentRow>> roleAssigneeMap = new TreeMap<>();
    private final TreeMap<DataFile,List<RoleAssignmentRow>> fileMap = new TreeMap<>();

    public TreeMap<AuthenticatedUser, List<FileAccessRequest>> getFileAccessRequestMap() {
        return fileAccessRequestMap;
    }
    
    public List<DataFile> getDataFilesForRequestor() {
        List<FileAccessRequest> fars = fileAccessRequestMap.get(getFileRequester());
        if (fars == null) {
            return new ArrayList<>();
        } else {
            return fars.stream().map(FileAccessRequest::getDataFile).collect(Collectors.toList());
        }
    }

    private final TreeMap<AuthenticatedUser,List<FileAccessRequest>> fileAccessRequestMap = new TreeMap<>();
    private boolean showDeleted = true;

    public boolean isShowDeleted() {
        return showDeleted;
    }

    public void setShowDeleted(boolean showDeleted) {
        this.showDeleted = showDeleted;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    
    public TreeMap<RoleAssignee, List<RoleAssignmentRow>> getRoleAssigneeMap() {
        return roleAssigneeMap;
    }

    public TreeMap<DataFile, List<RoleAssignmentRow>> getFileMap() {
        return fileMap;
    }

    private boolean backingShowDeleted = true;

    public void showDeletedCheckboxChange() {

        if (backingShowDeleted != showDeleted) {
            initMaps();
            backingShowDeleted = showDeleted;
        }

    }

    public String init() {
        if (dataset.getId() != null) {
            dataset = datasetService.find(dataset.getId());
        }

        // check if dvObject exists and user has permission
        if (dataset == null) {
            return permissionsWrapper.notFound();
        }

        if (!permissionService.on(dataset).has(Permission.ManageFilePermissions)) {
            return permissionsWrapper.notAuthorized();
        }
        initMaps();
        roleAssignmentHistory = null;
        return "";
    }

    private void initMaps() {
        // initialize files and usergroup list
        roleAssigneeMap.clear();
        fileMap.clear();
        fileAccessRequestMap.clear();

        for (DataFile file : dataset.getFiles()) {

            // only include if the file is restricted (or its draft version is restricted)
            //Added a null check in case there are files that have no metadata records SEK
            //for 6587 make sure that a file is in the current version befor adding to the fileMap SEK 2/11/2020
                if (file.getFileMetadata() != null && (file.isRestricted() || file.getFileMetadata().isRestricted())) {
                    //only test if file is deleted if it's restricted
                    boolean fileIsDeleted = !((dataset.getLatestVersion().isDraft() && file.getFileMetadata().getDatasetVersion().isDraft())
                            || (dataset.getLatestVersion().isReleased() && file.getFileMetadata().getDatasetVersion().equals(dataset.getLatestVersion())));

                    if (!isShowDeleted() && fileIsDeleted) {
                        //if don't show deleted and is deleted go to next file...
                        continue;
                    }
                // we get the direct role assignments assigned to the file
                List<RoleAssignment> ras = roleService.directRoleAssignments(file);
                List<RoleAssignmentRow> raList = new ArrayList<>(ras.size());
                for (RoleAssignment ra : ras) {
                    // for files, only show role assignments which can download
                    if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                        raList.add(new RoleAssignmentRow(ra, roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier(), true).getDisplayInfo(), fileIsDeleted));
                        addFileToRoleAssignee(ra, fileIsDeleted);
                    }
                }

                file.setDeleted(fileIsDeleted);

                fileMap.put(file, raList);

                // populate the file access requests map
                for (FileAccessRequest fileAccessRequest : file.getFileAccessRequests(FileAccessRequest.RequestState.CREATED)) {
                    List<FileAccessRequest> fileAccessRequestList = fileAccessRequestMap.get(fileAccessRequest.getRequester());
                    if (fileAccessRequestList == null) {
                        fileAccessRequestList = new ArrayList<>();
                        AuthenticatedUser withProvider = authenticationService.getAuthenticatedUserWithProvider(fileAccessRequest.getRequester().getUserIdentifier());
                        fileAccessRequestMap.put(withProvider, fileAccessRequestList);
                    }
                    fileAccessRequestList.add(fileAccessRequest);
                }
            }
        }
    }

    public String getAuthProviderFriendlyName(String authProviderId){
        return AuthenticationProvider.getFriendlyName(authProviderId);
    }

    Date getAccessRequestDate(List<FileAccessRequest> fileAccessRequests){
        if (fileAccessRequests == null) {
            return null;
        }

        // find the oldest date in the list of available and return a formatted date, or null if no dates were found
        return fileAccessRequests.stream()
            .filter(fileAccessRequest -> fileAccessRequest.getCreationTime() != null)
            .min((a, b) -> ObjectUtils.compare(a.getCreationTime(), b.getCreationTime(), true))
            .map(FileAccessRequest::getCreationTime)
            .orElse(null);
    }

    public String formatAccessRequestDate(List<FileAccessRequest> fileAccessRequests){
        Date date = getAccessRequestDate(fileAccessRequests);

        if (date == null) {
            return null;
        }

        return DateUtil.formatDate(date);
    }


    public String formatAccessRequestTimestamp(List<FileAccessRequest> fileAccessRequests){
        Date date = getAccessRequestDate(fileAccessRequests);

        if (date == null) {
            return null;
        }

        return Util.getDateTimeFormat().format(date);
    }

    private void addFileToRoleAssignee(RoleAssignment assignment, boolean fileDeleted) {
        RoleAssignee ra = roleAssigneeService.getRoleAssignee(assignment.getAssigneeIdentifier());
        List<RoleAssignmentRow> assignments = roleAssigneeMap.get(ra);
        if (assignments == null) {
            assignments = new ArrayList<>();
            roleAssigneeMap.put(ra, assignments);
        }
        
        assignments.add(new RoleAssignmentRow(assignment, ra.getDisplayInfo(), fileDeleted));
    }

    /* 
     main page
     */
    
    public void removeRoleAssignments(List<RoleAssignmentRow> raRows) {
        for (RoleAssignmentRow raRow : raRows) {
            revokeRole(raRow.getId());
        }
        
        initMaps();
        showUserGroupMessages();
    }    


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
        setSelectedRoleAssignmentRows(new ArrayList<>());
        this.selectedFile = file;
        this.selectedRoleAssignee = null;
        this.roleAssignments = raRows;
        showFileMessages();
    }
    
    public void initViewRemoveDialogByRoleAssignee(RoleAssignee ra, List<RoleAssignmentRow> raRows) {
        setSelectedRoleAssignmentRows(new ArrayList<>());
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
            commandEngine.submit(new RevokeRoleCommand(ra, dvRequestService.getDataverseRequest()));
            JsfHelper.addSuccessMessage( BundleUtil.getStringFromBundle("permission.roleWasRemoved" , Arrays.asList(ra.getRole().getName(), roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo().getTitle())));
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"), BundleUtil.getStringFromBundle("permission.permissionsMissing" , Arrays.asList(ex.getRequiredPermissions().toString())));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"));
            logger.log(Level.SEVERE, "Error removing role assignment: " + ex.getMessage(), ex);
        }
    }    
  
 
    /*
     grant access dialog
     */
    private List<RoleAssignee> selectedRoleAssignees;
    private List<DataFile> selectedFiles = new ArrayList<>();
    private AuthenticatedUser fileRequester;
    
    public List<RoleAssignee> getSelectedRoleAssignees() {
        return selectedRoleAssignees;
    }

    public void setSelectedRoleAssignees(List<RoleAssignee> selectedRoleAssignees) {
        this.selectedRoleAssignees = selectedRoleAssignees;
    }

    public List<DataFile> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<DataFile> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    
    public AuthenticatedUser getFileRequester() {
        return fileRequester;
    }


    public void initAssignDialog(ActionEvent ae) {

        showDeleted = false;
        initMaps();

        fileRequester = null;
        selectedRoleAssignees = null;
        selectedFiles.clear();
        showUserGroupMessages();
    }
    
    public void initAssignDialogByFile(DataFile file) {
        showDeleted = false;
        initMaps();
        fileRequester = null;
        selectedRoleAssignees = null;
        selectedFiles.clear();
        selectedFiles.add(file);
        showFileMessages();
    }
    public void initAssignDialogForFileRequester(AuthenticatedUser au) {
        fileRequester = au;
        selectedRoleAssignees = null;
        selectedFiles.clear();

        for (FileAccessRequest fileAccessRequest : fileAccessRequestMap.get(au)) {
            selectedFiles.add(fileAccessRequest.getDataFile());
        }
        showUserGroupMessages();
    }     
    

    public List<RoleAssignee> completeRoleAssignee(String query) {
        return roleAssigneeService.filterRoleAssignees(query, dataset, selectedRoleAssignees); 
    }
    
    public void grantAccess(ActionEvent evt) {
        // Find the built in file downloader role (currently by alias)
        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER);
        for (RoleAssignee roleAssignee : selectedRoleAssignees) {
            boolean sendNotification = false;
            for (DataFile file : selectedFiles) {
                if (assignRole(roleAssignee, file, fileDownloaderRole)) {
                    if (file.isReleased()) {
                        sendNotification = true;
                    }
                    // set request(s) granted, if they exist
                    for (AuthenticatedUser au : roleAssigneeService.getExplicitUsers(roleAssignee)) {
                        FileAccessRequest far = file.getAccessRequestForAssignee(au);
                        //There may not be a request, so do the null check
                        if (far != null) {
                            far.setStateGranted();
                        }
                    }
                    datafileService.save(file);
                }

            }

            if (sendNotification) {
                for (AuthenticatedUser au : roleAssigneeService.getExplicitUsers(roleAssignee)) {
                    userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.GRANTFILEACCESS, dataset.getId());                
                }
             }
        }

        initMaps();
    }
    
    public void grantAccessToRequests(AuthenticatedUser au) {
        grantAccessToRequests(au, selectedFiles);
    }
    
    public void grantAccessToAllRequests(AuthenticatedUser au) {
        List<DataFile> files = new ArrayList<>();

        for (FileAccessRequest fileAccessRequest : fileAccessRequestMap.get(au)) {
            files.add(fileAccessRequest.getDataFile());
        }

        grantAccessToRequests(au, files);
    }

    private void grantAccessToRequests(AuthenticatedUser au, List<DataFile> files) {
        boolean actionPerformed = false;
        // Find the built in file downloader role (currently by alias) 
        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER);
        for (DataFile file : files) {
            if (assignRole(au, file, fileDownloaderRole)) {
                FileAccessRequest far = file.getAccessRequestForAssignee(au);
                if (far!=null) {
                    far.setStateGranted();
                    datafileService.save(file);
                }
                actionPerformed = true;
            }
        }

        if (actionPerformed) {
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.fileAccessGranted", Arrays.asList(au.getDisplayInfo().getTitle())));
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.GRANTFILEACCESS, dataset.getId());
            initMaps();
        }

    }
    
    public void rejectAccessToRequests(AuthenticatedUser au) {
        rejectAccessToRequests(au, selectedFiles);
    }
    
    public void rejectAccessToAllRequests(AuthenticatedUser au) {
        List<DataFile> files = new ArrayList<>();

        for (FileAccessRequest fileAccessRequest : fileAccessRequestMap.get(au)) {
            files.add(fileAccessRequest.getDataFile());
        }

        rejectAccessToRequests(au, files);
    }

    private void rejectAccessToRequests(AuthenticatedUser au, List<DataFile> files) {
        boolean actionPerformed = false;
        for (DataFile file : files) {
            FileAccessRequest far = file.getAccessRequestForAssignee(au);
            if(far!=null) {
                far.setStateRejected();
                fileAccessRequestService.save(far);
                file.removeFileAccessRequest(far);
                datafileService.save(file);
                actionPerformed = true;
            }
        }

        if (actionPerformed) {
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.fileAccessRejected", Arrays.asList(au.getDisplayInfo().getTitle())));
            userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.REJECTFILEACCESS, dataset.getId());
            initMaps();
        }
    }

    private boolean assignRole(RoleAssignee ra,  DataFile file, DataverseRole r) {
        try {
            String privateUrlToken = null;
            commandEngine.submit(new AssignRoleCommand(ra, r, file, dvRequestService.getDataverseRequest(), privateUrlToken));
            List<String> args = Arrays.asList(
                    r.getName(),
                    ra.getDisplayInfo().getTitle(),
                    file.getDisplayName()
            );
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("permission.roleAssignedToFor", args));
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeAssigned"), BundleUtil.getStringFromBundle("permission.permissionsMissing", Arrays.asList(ex.getRequiredPermissions().toString())));
            return false;
        } catch (CommandException ex) {
            //JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role was not able to be assigned.");
            String message = r.getName() + BundleUtil.getStringFromBundle("permission.roleNotAbleToBeAssigned") + ra.getDisplayInfo().getTitle() + " - " + file.getDisplayName() + ".";
            JsfHelper.addErrorMessage(message);
            logger.log(Level.SEVERE, "Error assiging role: " + ex.getMessage(), ex);
            return false;
        }
        
        return true;
    }

    private List<RoleAssignmentHistoryEntry> roleAssignmentHistory;

    public List<RoleAssignmentHistoryEntry> getRoleAssignmentHistory() {
        if (roleAssignmentHistory == null) {
            roleAssignmentHistory = new ArrayList<>();
            
            List<RoleAssignmentAudit> audits = em.createNamedQuery("RoleAssignmentAudit.findByOwnerId", RoleAssignmentAudit.class)
                    .setParameter("datasetId", dataset.getId())
                    .getResultList();
            
            Map<Long, RoleAssignmentHistoryEntry> historyMap = new HashMap<>();
            
            for (RoleAssignmentAudit audit : audits) {
                Long roleAssignmentId = audit.getRoleAssignmentId();
                RoleAssignmentHistoryEntry entry = historyMap.get(roleAssignmentId);
                
                if (entry == null) {
                    entry = new RoleAssignmentHistoryEntry(audit.getAssigneeIdentifier(), audit.getRoleAlias(), audit.getDefinitionPointId()  );
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
            // Second pass: Combine entries with matching criteria
            Map<String, RoleAssignmentHistoryEntry> finalHistoryMap = new HashMap<>();
            for (RoleAssignmentHistoryEntry entry : historyMap.values()) {
                String key = entry.getAssigneeIdentifier() + "|" + entry.getRoleName() + "|" +
                             entry.getAssignedBy() + "|" + entry.getAssignedAt() + "|" +
                             entry.getRevokedBy() + "|" + entry.getRevokedAt();
                
                RoleAssignmentHistoryEntry existingEntry = finalHistoryMap.get(key);
                if (existingEntry == null) {
                    finalHistoryMap.put(key, entry);
                } else {
                    existingEntry.addDefinitionPointId(entry.getDefinitionPointIds().get(0));
                }
            }
            
            roleAssignmentHistory.addAll(finalHistoryMap.values());
            roleAssignmentHistory.sort(Comparator.comparing(RoleAssignmentHistoryEntry::getAssignedAt).reversed());
        }
        return roleAssignmentHistory;
    }

    boolean renderUserGroupMessages = false;
    boolean renderFileMessages = false;
       
    public void showUserGroupMessages() {
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
            //Used when a file to which there has been a role assignment added is deleted    

        private final boolean deleted;

        public boolean isDeleted() {
            return deleted;
        }

        public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf) {
            this.ra = anRa;
            this.assigneeDisplayInfo = disInf;
            this.deleted = false;
        }

        public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf, boolean deleted) {

            this.ra = anRa;
            this.assigneeDisplayInfo = disInf;
            this.deleted = deleted;

        } 
        

        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return assigneeDisplayInfo;
        }

        public DvObject getDefinitionPoint() {
            return ra.getDefinitionPoint();
        }

        
        public Long getId() {
            return ra.getId();
        }
        
    
    }   
}
