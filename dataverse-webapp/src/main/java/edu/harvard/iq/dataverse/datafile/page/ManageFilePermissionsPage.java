package edu.harvard.iq.dataverse.datafile.page;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.datafile.FilePermissionsService;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import jersey.repackaged.com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import javax.faces.view.ViewScoped;

import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class ManageFilePermissionsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ManageFilePermissionsPage.class.getCanonicalName());

    private DatasetDao datasetDao;
    private RoleAssigneeServiceBean roleAssigneeService;
    private PermissionsWrapper permissionsWrapper;
    private FilePermissionsService filePermissionsService;
    private DataverseRoleServiceBean roleService;

    private Long datasetId;
    private Dataset dataset;
    private final TreeMap<RoleAssignee, List<RoleAssignmentRow>> roleAssigneeMap = new TreeMap<>();
    private final TreeMap<DataFile, List<RoleAssignmentRow>> fileMap = new TreeMap<>();
    private final TreeMap<AuthenticatedUser, List<DataFile>> fileAccessRequestMap = new TreeMap<>();


    @Deprecated
    public ManageFilePermissionsPage() {
        // JEE requirement
    }

    @Inject
    public ManageFilePermissionsPage(DatasetDao datasetDao, RoleAssigneeServiceBean roleAssigneeService,
                                     PermissionsWrapper permissionsWrapper, FilePermissionsService filePermissionsService,
                                     DataverseRoleServiceBean roleService) {
        this.datasetDao = datasetDao;
        this.roleAssigneeService = roleAssigneeService;
        this.permissionsWrapper = permissionsWrapper;
        this.filePermissionsService = filePermissionsService;
        this.roleService = roleService;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public TreeMap<RoleAssignee, List<RoleAssignmentRow>> getRoleAssigneeMap() {
        return roleAssigneeMap;
    }

    public TreeMap<DataFile, List<RoleAssignmentRow>> getFileMap() {
        return fileMap;
    }

    public TreeMap<AuthenticatedUser, List<DataFile>> getFileAccessRequestMap() {
        return fileAccessRequestMap;
    }


    public String init() {
        if (datasetId == null) {
            return permissionsWrapper.notFound();
        }
        dataset = datasetDao.find(datasetId);

        // check if dvObject exists and user has permission
        if (dataset == null) {
            return permissionsWrapper.notFound();
        }

        if (!permissionsWrapper.canManagePermissions(dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        initMaps();
        return "";
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
        
        List<DataFile> files = Lists.newArrayList();
        List<RoleAssignee> roleAssignees = Lists.newArrayList();
        Set<String> roleAssigneeNames = Sets.newTreeSet();
        
        for (RoleAssignmentRow raRow: selectedRoleAssignmentRows) {
            files.add(raRow.getDefinitionPoint());
            roleAssignees.add(raRow.getRoleAssignee());
            roleAssigneeNames.add(raRow.getAssigneeDisplayInfo().getTitle());
        }
        
        Try.of(() -> filePermissionsService.revokeFileDownloadRole(roleAssignees, files))
            .onSuccess((roleAssignments) -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.file.downloadRoleWasRemoved",
                    StringUtils.join(roleAssigneeNames, ", ")
            )))
            .onSuccess((roleAssignments) -> initMaps())
            .onFailure(ex -> handleRemoveRoleException(ex));

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
        fileRequester = null;
        selectedRoleAssignees = null;
        selectedFiles.clear();
        showUserGroupMessages();
    }

    public void initAssignDialogByFile(DataFile file) {
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
        selectedFiles.addAll(fileAccessRequestMap.get(au));
        showUserGroupMessages();
    }


    public List<RoleAssignee> completeRoleAssignee(String query) {
        return roleAssigneeService.filterRoleAssignees(query, dataset, selectedRoleAssignees);
    }

    public void grantAccess(ActionEvent evt) {
        
        String usersString = selectedRoleAssignees.stream().map(ra -> ra.getDisplayInfo().getTitle()).collect(Collectors.joining(", "));
        String filesString = selectedFiles.stream().map(file -> file.getDisplayName()).collect(Collectors.joining(", "));
        
        Try.of(() -> filePermissionsService.assignFileDownloadRole(selectedRoleAssignees, selectedFiles))
                .onSuccess((roleAssignments) -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.file.roleAssignedToFor", usersString, filesString)))
                .onSuccess((roleAssignments) -> initMaps())
                .onFailure((e) -> handleAssignRoleException(e, usersString, filesString));
        
    }

    public void grantAccessToRequests(AuthenticatedUser au) {
        grantAccessToRequests(au, selectedFiles);
    }

    public void grantAccessToAllRequests(AuthenticatedUser au) {
        grantAccessToRequests(au, fileAccessRequestMap.get(au));
    }

    private void grantAccessToRequests(AuthenticatedUser au, List<DataFile> files) {
        
        String userString = au.getDisplayInfo().getTitle();
        String filesString = files.stream().map(file -> file.getDisplayName()).collect(Collectors.joining(", "));
        
        Try.of(() -> filePermissionsService.assignFileDownloadRole(Collections.singletonList(au), files))
            .onSuccess((roleAssigments) -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.fileAccessGranted", userString)))
            .onSuccess((roleAssigments) -> initMaps())
            .onFailure((e) -> handleAssignRoleException(e, userString, filesString));
    }

    public void rejectAccessToRequestsForSelectedFiles(AuthenticatedUser au) {
        rejectAccessToRequests(au, selectedFiles);
    }

    public void rejectAccessToAllRequests(AuthenticatedUser au) {
        rejectAccessToRequests(au, fileAccessRequestMap.get(au));
    }

    private void rejectAccessToRequests(AuthenticatedUser au, List<DataFile> files) {
        filePermissionsService.rejectRequestAccessToFiles(au, files);
        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.fileAccessRejected", Arrays.asList(au.getDisplayInfo().getTitle())));
        initMaps();
    }
    
    
    private void handleAssignRoleException(Throwable ex, String usersString, String filesString) {
        if (ex instanceof PermissionException) {
            handlePermissionException((PermissionException) ex, "permission.roleNotAbleToBeAssigned");
            return;
        }
        
        String message = BundleUtil.getStringFromBundle("permission.roleNotAbleToBeAssigned") + usersString + " - " + filesString + ".";
        JsfHelper.addFlashErrorMessage(message);
        logger.log(Level.SEVERE, "Error assiging role: " + ex.getMessage(), ex);
    }
    
    private void handleRemoveRoleException(Throwable ex) {
        if (ex instanceof PermissionException) {
            handlePermissionException((PermissionException) ex, "permission.roleNotAbleToBeRemoved");
            return;
        }
        JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"));
        logger.log(Level.SEVERE, "Error removing role assignment: " + ex.getMessage(), ex);
    }
    
    private void handlePermissionException(PermissionException ex, String operationErrorSummaryKey) {
        Set<Permission> requiredPermissions = ex.getMissingPermissions();
        JH.addMessage(FacesMessage.SEVERITY_ERROR,
                BundleUtil.getStringFromBundle(operationErrorSummaryKey),
                BundleUtil.getStringFromBundle("permission.permissionsMissing", Arrays.asList(requiredPermissions.toString())));
    }
    
    private void initMaps() {
        // initialize files and usergroup list
        roleAssigneeMap.clear();
        fileMap.clear();
        fileAccessRequestMap.clear();

        for (DataFile file : dataset.getFiles()) {
            
            // only include if the file in any dataset version is restricted
            if (anyFileMetadataRestricted(file)) {
                // we get the direct role assignments assigned to the file
                
                List<RoleAssignment> ras = fetchDownloadRoleAssignments(file);
                List<RoleAssignmentRow> raList = new ArrayList<>();
                for (RoleAssignment ra : ras) {
                    RoleAssignee roleAssignee = roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier());
                    
                    raList.add(new RoleAssignmentRow(ra, roleAssignee));
                    
                    roleAssigneeMap.putIfAbsent(roleAssignee, new ArrayList<>());
                    roleAssigneeMap.get(roleAssignee).add(new RoleAssignmentRow(ra, roleAssignee));
                }

                fileMap.put(file, raList);

                // populate the file access requests map
                for (AuthenticatedUser au : file.getFileAccessRequesters()) {
                    fileAccessRequestMap.putIfAbsent(au, new ArrayList<>());
                    fileAccessRequestMap.get(au).add(file);
                }
            }
        }

    }

    private boolean anyFileMetadataRestricted(DataFile file) {
        
        for (FileMetadata fileMetadata: file.getFileMetadatas()) {
            if (fileMetadata.getTermsOfUse().getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
                return true;
            }
        }
        return false;
    }

    private List<RoleAssignment> fetchDownloadRoleAssignments(DataFile file) {
        
        List<RoleAssignment> ras = roleService.directRoleAssignments(file);
        List<RoleAssignment> downloadFileRoleAssigments = new ArrayList<>();
        for (RoleAssignment ra : ras) {
            if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                downloadFileRoleAssigments.add(ra);
            }
        }
        return downloadFileRoleAssigments;
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

    public boolean isRenderFileMessages() {
        return renderFileMessages;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public Long getDatasetId() {
        return datasetId;
    }


    // inner class used fordisplay of role assignments
    public static class RoleAssignmentRow {

        private final RoleAssignee roleAssignee;
        private final RoleAssignment roleAssignment;

        public RoleAssignmentRow(RoleAssignment roleAssignment, RoleAssignee roleAssignee) {
            Preconditions.checkArgument(roleAssignment.getDefinitionPoint() instanceof DataFile);
            this.roleAssignment = roleAssignment;
            this.roleAssignee = roleAssignee;
        }


        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return roleAssignee.getDisplayInfo();
        }

        public DataFile getDefinitionPoint() {
            return (DataFile)roleAssignment.getDefinitionPoint();
        }


        public Long getId() {
            return roleAssignment.getId();
        }

        public RoleAssignee getRoleAssignee() {
            return roleAssignee;
        }

    }
}
