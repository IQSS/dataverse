package edu.harvard.iq.dataverse.notification.dto;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadataRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserNotificationMapperTest {

    private final static Long DATAVERSE_ID = 3L;
    private final static Long DATASET_ID = 30L;
    private final static Long DATASET_VERSION_ID = 31L;
    private final static Long DATAFILE_ID = 300L;
    private final static Long FILEMETADATA_ID = 301L;
    
    @InjectMocks
    private UserNotificationMapper notificationMapper;

    @Mock
    private DataverseRepository dataverseRepository;
    @Mock
    private DatasetRepository datasetRepository;
    @Mock
    private DatasetVersionRepository datasetVersionRepository;
    @Mock
    private DataFileRepository dataFileRepository;
    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private PermissionServiceBean permissionService;

    private Dataverse dataverse;
    private Dataset dataset;
    private DatasetVersion datasetVersion;
    private DataFile dataFile;
    
    private AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("John", "Doe");
    
    @BeforeEach
    void beforeEach() {
        dataverse = new Dataverse();
        dataverse.setId(DATAVERSE_ID);
        
        dataset = new Dataset();
        dataset.setId(DATASET_ID);
        dataset.setOwner(dataverse);
        
        datasetVersion = dataset.getLatestVersion();
        datasetVersion.setId(DATASET_VERSION_ID);
        
        dataFile = new DataFile();
        dataFile.setId(DATAFILE_ID);
        dataFile.setOwner(dataset);
        
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(FILEMETADATA_ID);
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setDatasetVersion(datasetVersion);
    }

    // -------------------- TESTS --------------------

    @Test
    void toDTO_basic_info() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType("type");
        notification.setId(1L);
        notification.setSendDate(Timestamp.from(Instant.parse("2007-12-03T10:15:30.00Z")));
        notification.setAdditionalMessage("additional message");
        notification.setReadNotification(true);

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getId, UserNotificationDTO::getSendDate,
                    UserNotificationDTO::getAdditionalMessage, UserNotificationDTO::isDisplayAsRead)
            .containsExactly(
                    1L, Timestamp.from(Instant.parse("2007-12-03T10:15:30.00Z")),
                    "additional message", true);
    }
    
    @Test
    void toDTO_ASSIGNROLE_dataverse() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.ASSIGNROLE);
        notification.setObjectId(DATAVERSE_ID);
        notification.setUser(user);
        when(dataverseRepository.findById(DATAVERSE_ID)).thenReturn(Optional.of(dataverse));
        when(permissionService.getRolesOfUser(user, dataverse)).thenReturn(
                Sets.newHashSet(
                        createRoleAssignment(1L, DataverseRole.BuiltInRole.ADMIN.getAlias()),
                        createRoleAssignment(2L, DataverseRole.BuiltInRole.DEPOSITOR.getAlias())));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.ASSIGNROLE, dataverse, NotificationObjectType.DATAVERSE, "Admin/Depositor");
    }

    @Test
    void toDTO_ASSIGNROLE_dataset() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.ASSIGNROLE);
        notification.setObjectId(DATASET_ID);
        notification.setUser(user);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));
        when(permissionService.getRolesOfUser(user, dataset)).thenReturn(
                Sets.newHashSet(
                        createRoleAssignment(1L, DataverseRole.BuiltInRole.DS_CONTRIBUTOR.getAlias()),
                        createRoleAssignment(2L, DataverseRole.BuiltInRole.FILE_DOWNLOADER.getAlias())));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.ASSIGNROLE, dataset, NotificationObjectType.DATASET, "Dataset Creator/File Downloader");
        
    }

    @Test
    void toDTO_ASSIGNROLE_datafile() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.ASSIGNROLE);
        notification.setObjectId(DATAFILE_ID);
        notification.setUser(user);
        when(dataFileRepository.findById(DATAFILE_ID)).thenReturn(Optional.of(dataFile));
        when(permissionService.getRolesOfUser(user, dataFile)).thenReturn(
                Sets.newHashSet(
                        createRoleAssignment(1L, DataverseRole.BuiltInRole.FILE_DOWNLOADER.getAlias())));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.ASSIGNROLE, dataFile, NotificationObjectType.DATAFILE, "File Downloader");
        
    }

    @Test
    void toDTO_REVOKEROLE_dataset() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.REVOKEROLE);
        notification.setObjectId(DATASET_ID);
        notification.setUser(user);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));
        when(permissionService.getRolesOfUser(user, dataset)).thenReturn(
                Sets.newHashSet(
                        createRoleAssignment(1L, DataverseRole.BuiltInRole.DS_CONTRIBUTOR.getAlias()),
                        createRoleAssignment(2L, DataverseRole.BuiltInRole.FILE_DOWNLOADER.getAlias())));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.REVOKEROLE, dataset, NotificationObjectType.DATASET, "Dataset Creator/File Downloader");
        
    }

    @Test
    void toDTO_CREATEDV() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEDV);
        notification.setObjectId(DATAVERSE_ID);
        when(dataverseRepository.findById(DATAVERSE_ID)).thenReturn(Optional.of(dataverse));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.CREATEDV, dataverse, NotificationObjectType.DATAVERSE, null);
        
    }

    @Test
    void toDTO_CREATEDS() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEDS);
        notification.setObjectId(DATASET_VERSION_ID);
        when(datasetVersionRepository.findById(DATASET_VERSION_ID)).thenReturn(Optional.of(datasetVersion));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.CREATEDS, datasetVersion, NotificationObjectType.DATASET_VERSION, null);
    }

    @Test
    void toDTO_CREATEACC() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEACC);
        notification.setUser(user);

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType, UserNotificationDTO::getRoleString)
            .containsExactly(NotificationType.CREATEACC, user, NotificationObjectType.AUTHENTICATED_USER, null);
    }

    @Test
    void toDTO_REQUESTFILEACCESS() {
        // given
        AuthenticatedUser requestor = MocksFactory.makeAuthenticatedUser("Some", "Requestor");
        requestor.setEmail("sr@example.com");
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.REQUESTFILEACCESS);
        notification.setUser(user);
        notification.setRequestor(requestor);
        notification.setObjectId(DATAFILE_ID);
        when(dataFileRepository.findById(DATAFILE_ID)).thenReturn(Optional.of(dataFile));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType,
                    UserNotificationDTO::getRequestorName, UserNotificationDTO::getRequestorEmail)
            .containsExactly(
                    NotificationType.REQUESTFILEACCESS, dataset, NotificationObjectType.DATASET,
                    "Some Requestor", "sr@example.com");
    }

    @Test
    void toDTO_object_not_in_database() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEDV);
        notification.setObjectId(DATAVERSE_ID);

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType)
            .containsExactly(
                    NotificationType.CREATEDV, null, null);
    }

    @Test
    void toDTO_custom_type() {
        // given
        UserNotification notification = new UserNotification();
        notification.setObjectId(DATASET_ID);
        notification.setType("CUSTOM");
        notification.setUser(user);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));

        // when
        UserNotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        // then
        assertThat(notificationDTO)
            .extracting(
                    UserNotificationDTO::getType, UserNotificationDTO::getTheObject,
                    UserNotificationDTO::getTheObjectType)
            .containsExactly(
                    "CUSTOM", dataset, NotificationObjectType.DATASET);
    }

    // -------------------- PRIVATE --------------------

    private RoleAssignment createRoleAssignment(Long roleId, String roleAlias) {
        RoleAssignment roleAssignment = new RoleAssignment();
        DataverseRole dataverseRole = new DataverseRole();
        dataverseRole.setId(roleId);
        dataverseRole.setAlias(roleAlias);
        roleAssignment.setRole(dataverseRole);
        return roleAssignment;
    }

}
