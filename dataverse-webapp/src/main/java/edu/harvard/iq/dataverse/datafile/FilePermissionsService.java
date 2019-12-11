package edu.harvard.iq.dataverse.datafile;

import com.google.api.client.util.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestAccessCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import io.vavr.Tuple2;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Stateless
public class FilePermissionsService {

    private DataFileServiceBean datafileService;

    private EjbDataverseEngine commandEngine;

    private DataverseRoleServiceBean roleService;

    private PermissionServiceBean permissionService;
    
    private RoleAssigneeServiceBean roleAssigneeService;

    private UserNotificationService userNotificationService;

    private DataverseRequestServiceBean dvRequestService;
    
    
    // -------------------- CONSTRUCTORS --------------------
    
    @Deprecated
    public FilePermissionsService() {
        // constructor for JEE
    }
    
    @Inject
    public FilePermissionsService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
            DataverseRoleServiceBean roleService, UserNotificationService userNotificationService,
            DataFileServiceBean datafileService, RoleAssigneeServiceBean roleAssigneeService,
            PermissionServiceBean permissionService) {
        
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.roleService = roleService;
        this.userNotificationService = userNotificationService;
        this.datafileService = datafileService;
        this.roleAssigneeService = roleAssigneeService;
        this.permissionService = permissionService;
    }
    
    
    // -------------------- LOGIC --------------------
    
    /**
     * Adds download permission for users/groups to files passed as argument
     * <p>
     * Note: Files must be from the same dataset
     */
    public List<RoleAssignment> assignFileDownloadRole(List<RoleAssignee> roleAssignees, List<DataFile> files) {
        Preconditions.checkArgument(!files.isEmpty());
        Preconditions.checkArgument(isAllFilesFromSameDataset(files));
        
        Dataset dataset = files.get(0).getOwner();
        
        List<RoleAssignment> roleAssignments = Lists.newArrayList();
        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER);
        
        for (Tuple2<RoleAssignee, DataFile> roleAssigneeAndFile: prepareRoleAssigneeAndFileTuples(roleAssignees, files)) {
            RoleAssignment roleAssignment = assignRole(roleAssigneeAndFile._1, roleAssigneeAndFile._2, fileDownloaderRole);
            roleAssignments.add(roleAssignment);
        }
        
        List<AuthenticatedUser> usersToNotify = collectAutheticatedUsersForRoleAssignees(roleAssignees);
        for (AuthenticatedUser userToNotify: usersToNotify) {
            userNotificationService.sendNotificationWithEmail(userToNotify, new Timestamp(new Date().getTime()), NotificationType.GRANTFILEACCESS,
                    dataset.getId(), NotificationObjectType.DATASET);
        }
        
        return roleAssignments;
    }
    
    /**
     * Removes download permission for users/groups to files passed as argument
     */
    public List<RoleAssignment> revokeFileDownloadRole(List<RoleAssignee> roleAssignees, List<DataFile> files) {
        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER);
        
        Set<RoleAssignment> roleAssignments = Sets.newHashSet();
        
        for (Tuple2<RoleAssignee, DataFile> roleAssigneeAndFile: prepareRoleAssigneeAndFileTuples(roleAssignees, files)) {
            Optional<RoleAssignment> roleAssignment = roleAssigneeService.getAssignmentFor(
                    roleAssigneeAndFile._1.getIdentifier(), roleAssigneeAndFile._2.getId(), fileDownloaderRole.getId());
            
            roleAssignment.ifPresent(roleAssignments::add);
        }
        
        for (RoleAssignment roleAssignment: roleAssignments) {
            RevokeRoleCommand revokeRoleCommand = new RevokeRoleCommand(roleAssignment, dvRequestService.getDataverseRequest());
            commandEngine.submit(revokeRoleCommand);
        }
        return Lists.newArrayList(roleAssignments);
    }
    
    
    public void requestAccessToFiles(List<DataFile> files) {
        Preconditions.checkArgument(!files.isEmpty());
        Preconditions.checkArgument(isAllFilesFromSameDataset(files));
        
        Dataset dataset = files.get(0).getOwner();

        for (DataFile file : files) {
            //Not sending notification via request method so that
            // we can bundle them up into one nofication at dataset level
            requestAccessToFile(file);
        }
        
        sendRequestFileAccessNotification(dataset, files.get(0).getId(),
                dvRequestService.getDataverseRequest().getAuthenticatedUser());
    }
    
    /**
     * Rejects request to access files passed as argument for
     * the given user
     * <p>
     * Note: Files must be from the same dataset
     */
    public void rejectRequestAccessToFiles(AuthenticatedUser au, List<DataFile> files) {
        Preconditions.checkArgument(!files.isEmpty());
        Preconditions.checkArgument(isAllFilesFromSameDataset(files));
        
        Dataset dataset = files.get(0).getOwner();
        
        for (DataFile file : files) {
            file.getFileAccessRequesters().remove(au);
            datafileService.save(file);
        }
        
        userNotificationService.sendNotificationWithEmail(au, new Timestamp(new Date().getTime()),
                NotificationType.REJECTFILEACCESS, dataset.getId(), NotificationObjectType.DATASET);
        
    }

    public void sendRequestFileAccessNotification(Dataset dataset, Long fileId, AuthenticatedUser requestor) {
        Stream<AuthenticatedUser> usersWithManageDsPerm = permissionService.getUsersWithPermissionOn(Permission.ManageDatasetPermissions, dataset).stream();
        Stream<AuthenticatedUser> usersWithManageMinorDsPerm = permissionService.getUsersWithPermissionOn(Permission.ManageMinorDatasetPermissions, dataset).stream();

        Stream.concat(usersWithManageDsPerm, usersWithManageMinorDsPerm).distinct().forEach((au) ->
                                                                                                    userNotificationService.sendNotificationWithEmail(au, new Timestamp(new Date().getTime()), NotificationType.REQUESTFILEACCESS,
                                                                                                                                                      fileId, NotificationObjectType.DATAFILE, requestor));
    }
    
    
    // -------------------- PRIVATE --------------------
    
    private List<Tuple2<RoleAssignee, DataFile>> prepareRoleAssigneeAndFileTuples(List<RoleAssignee> roleAssignees, List<DataFile> files) {
        List<Tuple2<RoleAssignee, DataFile>> roleAssigneeAndFileTuples = Lists.newArrayList();
        for (RoleAssignee roleAssignee: roleAssignees) {
            for (DataFile file: files) {
                roleAssigneeAndFileTuples.add(new Tuple2<>(roleAssignee, file));
            }
        }
        return roleAssigneeAndFileTuples;
    }
    
    private List<AuthenticatedUser> collectAutheticatedUsersForRoleAssignees(List<RoleAssignee> roleAssignees) {
        Set<AuthenticatedUser> authenticatedUsers = Sets.newHashSet();
        
        for (RoleAssignee roleAssignee : roleAssignees) {
            authenticatedUsers.addAll(roleAssigneeService.getExplicitUsers(roleAssignee));
        }
        return Lists.newArrayList(authenticatedUsers);
    }
    
    private RoleAssignment assignRole(RoleAssignee ra, DataFile file, DataverseRole fileDownloaderRole) {
        
        String privateUrlToken = null;
        AssignRoleCommand assignRoleCommand = new AssignRoleCommand(ra, fileDownloaderRole, file, dvRequestService.getDataverseRequest(), privateUrlToken);
        
        RoleAssignment roleAssignment = commandEngine.submit(assignRoleCommand);
        
        
        // remove request, if it exist
        if (file.getFileAccessRequesters().remove(ra)) {
            datafileService.save(file);
        }
        
        return roleAssignment;
    }
    
    private void requestAccessToFile(DataFile file) {
        if (!file.getFileAccessRequesters().contains(dvRequestService.getDataverseRequest().getAuthenticatedUser())) {
            commandEngine.submit(new RequestAccessCommand(dvRequestService.getDataverseRequest(), file));
        }
    }
    
    private boolean isAllFilesFromSameDataset(List<DataFile> files) {
        Dataset firstFileDataset = files.get(0).getOwner();
        return files.stream().allMatch(file -> file.getOwner().equals(firstFileDataset));
    }
    
}
