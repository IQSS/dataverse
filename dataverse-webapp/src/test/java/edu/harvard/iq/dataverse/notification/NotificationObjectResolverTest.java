package edu.harvard.iq.dataverse.notification;

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
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationObjectResolverTest {

    private final static Long DATAVERSE_ID = 3L;
    private final static Long DATASET_ID = 30L;
    private final static Long DATASET_VERSION_ID = 31L;
    private final static Long DATAFILE_ID = 300L;
    private final static Long FILEMETADATA_ID = 301L;

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
    private AuthenticatedUserRepository authenticatedUserRepository;
    @Mock
    private NotificationObjectVisitor notificationObjectVisitor;

    private Dataverse dataverse;
    private Dataset dataset;
    private DatasetVersion datasetVersion;
    private DataFile dataFile;

    private AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("John", "Doe");

    private NotificationObjectResolver notificationObjectResolver;

    @BeforeEach
    void beforeEach() {
        notificationObjectResolver = new NotificationObjectResolver(dataverseRepository, datasetRepository, dataFileRepository, datasetVersionRepository, fileMetadataRepository);

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
    void resolve_ASSIGNROLE_dataverse() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.ASSIGNROLE);
        notification.setObjectId(DATAVERSE_ID);
        notification.setUser(user);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(dataverseRepository.findById(DATAVERSE_ID)).thenReturn(Optional.of(dataverse));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataverse(dataverse);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_ASSIGNROLE_dataset() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.ASSIGNROLE);
        notification.setObjectId(DATASET_ID);
        notification.setUser(user);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataset(dataset);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_ASSIGNROLE_datafile() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.ASSIGNROLE);
        notification.setObjectId(DATAFILE_ID);
        notification.setUser(user);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(dataFileRepository.findById(DATAFILE_ID)).thenReturn(Optional.of(dataFile));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataFile(dataFile);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_REVOKEROLE_dataset() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.REVOKEROLE);
        notification.setObjectId(DATASET_ID);
        notification.setUser(user);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataset(dataset);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_CREATEDV() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEDV);
        notification.setObjectId(DATAVERSE_ID);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(dataverseRepository.findById(DATAVERSE_ID)).thenReturn(Optional.of(dataverse));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataverse(dataverse);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_CREATEDS() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEDS);
        notification.setObjectId(DATASET_VERSION_ID);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(datasetVersionRepository.findById(DATASET_VERSION_ID)).thenReturn(Optional.of(datasetVersion));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDatasetVersion(datasetVersion);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_CREATEACC() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEACC);
        notification.setUser(user);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleUser(user);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_REQUESTFILEACCESS() {
        // given
        AuthenticatedUser requestor = MocksFactory.makeAuthenticatedUser("Some", "Requestor");
        requestor.setEmail("sr@example.com");
        Map<String, String> parameters = new HashMap<>();
        parameters.put(NotificationParameter.REQUESTOR_ID.key(), requestor.getId().toString());

        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.REQUESTFILEACCESS);
        notification.setUser(user);
        notification.setParameters("{\"requestorId\":\"" + requestor.getId() + "\"}");

        notification.setObjectId(DATAFILE_ID);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(dataFileRepository.findById(DATAFILE_ID)).thenReturn(Optional.of(dataFile));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataset(dataset);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_object_not_in_database() {
        // given
        UserNotification notification = new UserNotification();
        notification.setType(NotificationType.CREATEDV);
        notification.setObjectId(DATAVERSE_ID);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).getNotification();
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @Test
    void resolve_custom_type() {
        // given
        UserNotification notification = new UserNotification();
        notification.setObjectId(DATASET_ID);
        notification.setType("CUSTOM");
        notification.setUser(user);
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataset(dataset);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GRANTFILEACCESSINFO", "REJECTFILEACCESSINFO"})
    void resolve_FILEACCESSINFO(String type) {
        // given
        UserNotification notification = new UserNotification();
        notification.setObjectId(DATASET_ID);
        notification.setType(type);
        notification.setUser(user);
        notification.setParameters("GRANTFILEACCESSINFO".equals(type)
                ? "{\"grantedBy\":\"dataverseAdmin\"}"
                : "{\"rejectedBy\":\"dataverseAdmin\"}");
        when(notificationObjectVisitor.getNotification()).thenReturn(notification);
        when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(dataset));

        // when
        notificationObjectResolver.resolve(notificationObjectVisitor);

        // then
        verify(notificationObjectVisitor, times(1)).handleDataset(dataset);
        verifyNoMoreInteractions(notificationObjectVisitor);
    }
}
