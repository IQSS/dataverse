package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.SystemConfig;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.harvard.iq.dataverse.util.json.InAppNotificationsJsonPrinter.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InAppNotificationsJsonPrinterTest {

    @Mock
    private DataverseServiceBean dataverseService;
    @Mock
    private DatasetServiceBean datasetService;
    @Mock
    private DatasetVersionServiceBean datasetVersionService;
    @Mock
    private DataFileServiceBean dataFileService;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private SystemConfig systemConfig;

    @InjectMocks
    private InAppNotificationsJsonPrinter sut;

    @Mock
    private NullSafeJsonBuilder notificationJson;
    @Mock
    private AuthenticatedUser authenticatedUser;
    @Mock
    private AuthenticatedUser requestor;

    @Captor
    private ArgumentCaptor<JsonValue> jsonValueCaptor;

    private UserNotification userNotification;

    private final GlobalId testGlobalId = new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.5072", "FK2/BYM3IW", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null);

    @BeforeEach
    public void setUp() {
        userNotification = new UserNotification();
    }

    @Test
    public void testAddFieldsByType_assignRole_dataverse() {
        userNotification.setType(UserNotification.Type.ASSIGNROLE);
        userNotification.setObjectId(1L);

        Dataverse dataverse = mock(Dataverse.class);
        when(dataverse.getAlias()).thenReturn("testdv");
        when(dataverse.getDisplayName()).thenReturn("Test Dataverse");
        when(dataverseService.find(1L)).thenReturn(dataverse);
        when(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataverse)).thenReturn(Collections.emptyList());

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(eq(KEY_ROLE_ASSIGNMENTS), any(JsonArrayBuilder.class));
        verify(notificationJson).add(KEY_DATAVERSE_ALIAS, "testdv");
        verify(notificationJson).add(KEY_DATAVERSE_DISPLAY_NAME, "Test Dataverse");
        verifyNoInteractions(datasetService, dataFileService);
    }

    @Test
    public void testAddFieldsByType_assignRole_dataset() {
        userNotification.setType(UserNotification.Type.ASSIGNROLE);
        userNotification.setObjectId(1L);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Test Dataset");

        when(dataverseService.find(1L)).thenReturn(null);
        when(datasetService.find(1L)).thenReturn(dataset);
        when(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataset)).thenReturn(Collections.emptyList());

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(eq(KEY_ROLE_ASSIGNMENTS), any(JsonArrayBuilder.class));
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Test Dataset");
        verifyNoInteractions(dataFileService);
    }

    @Test
    public void testAddFieldsByType_revokeRole_dataFile() {
        userNotification.setType(UserNotification.Type.REVOKEROLE);
        userNotification.setObjectId(1L);

        DataFile dataFile = mock(DataFile.class);
        Dataset owner = mock(Dataset.class);

        when(dataFile.getOwner()).thenReturn(owner);
        when(owner.getGlobalId()).thenReturn(testGlobalId);
        when(owner.getDisplayName()).thenReturn("Owner Dataset");

        when(dataverseService.find(1L)).thenReturn(null);
        when(datasetService.find(1L)).thenReturn(null);
        when(dataFileService.find(1L)).thenReturn(dataFile);
        when(permissionService.getEffectiveRoleAssignments(authenticatedUser, dataFile)).thenReturn(Collections.emptyList());

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(eq(KEY_ROLE_ASSIGNMENTS), any(JsonArrayBuilder.class));
        verify(notificationJson).add(KEY_OWNER_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataset");
    }

    @Test
    public void testAddFieldsByType_revokeRole_objectDeleted() {
        userNotification.setType(UserNotification.Type.REVOKEROLE);
        userNotification.setObjectId(1L);

        when(dataverseService.find(1L)).thenReturn(null);
        when(datasetService.find(1L)).thenReturn(null);
        when(dataFileService.find(1L)).thenReturn(null);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_OBJECT_DELETED, true);
    }

    @Test
    public void testAddFieldsByType_createDv_dvHasOwner() {
        userNotification.setType(UserNotification.Type.CREATEDV);
        userNotification.setObjectId(1L);

        Dataverse dataverse = mock(Dataverse.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataverse.getAlias()).thenReturn("childDv");
        when(dataverse.getDisplayName()).thenReturn("Child Dataverse");
        when(dataverse.getOwner()).thenReturn(owner);

        when(owner.getAlias()).thenReturn("parentDv");
        when(owner.getDisplayName()).thenReturn("Parent Dataverse");

        when(dataverseService.find(1L)).thenReturn(dataverse);
        when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
        when(systemConfig.getGuidesVersion()).thenReturn("1.0");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATAVERSE_ALIAS, "childDv");
        verify(notificationJson).add(KEY_DATAVERSE_DISPLAY_NAME, "Child Dataverse");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "parentDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Parent Dataverse");
        verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
        verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
        verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_DATAVERSE_MANAGEMENT_HTML);
    }

    @Test
    public void testAddFieldsByType_createDv_dvHasNoOwner() {
        userNotification.setType(UserNotification.Type.CREATEDV);
        userNotification.setObjectId(1L);

        Dataverse dataverse = mock(Dataverse.class);

        when(dataverse.getAlias()).thenReturn("dv");
        when(dataverse.getDisplayName()).thenReturn("Dataverse");
        when(dataverse.getOwner()).thenReturn(null);

        when(dataverseService.find(1L)).thenReturn(dataverse);
        when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
        when(systemConfig.getGuidesVersion()).thenReturn("1.0");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATAVERSE_ALIAS, "dv");
        verify(notificationJson).add(KEY_DATAVERSE_DISPLAY_NAME, "Dataverse");
        verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
        verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
        verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_DATAVERSE_MANAGEMENT_HTML);
    }

    @Test
    public void testAddFieldsByType_createDv_objectDeleted() {
        userNotification.setType(UserNotification.Type.CREATEDV);
        userNotification.setObjectId(1L);

        when(dataverseService.find(1L)).thenReturn(null);
        when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
        when(systemConfig.getGuidesVersion()).thenReturn("1.0");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
        verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
        verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_DATAVERSE_MANAGEMENT_HTML);
        verify(notificationJson).add(KEY_OBJECT_DELETED, true);
    }

    @Test
    public void testAddFieldsByType_requestFileAccess() {
        userNotification.setType(UserNotification.Type.REQUESTFILEACCESS);
        userNotification.setObjectId(1L);
        userNotification.setRequestor(requestor);

        when(requestor.getFirstName()).thenReturn("John");
        when(requestor.getLastName()).thenReturn("Doe");
        when(requestor.getEmail()).thenReturn("johndoe@example.com");

        Dataset dataset = mock(Dataset.class);
        when(dataset.getDisplayName()).thenReturn("Test Dataset");
        when(dataset.getIdentifier()).thenReturn("Test Identifier");

        DataFile dataFile = mock(DataFile.class);
        when(dataFile.getId()).thenReturn(1L);
        when(dataFile.getDisplayName()).thenReturn("Test File");
        when(dataFile.getOwner()).thenReturn(dataset);
        when(dataFileService.find(1L)).thenReturn(dataFile);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_REQUESTOR_FIRST_NAME, "John");
        verify(notificationJson).add(KEY_REQUESTOR_LAST_NAME, "Doe");
        verify(notificationJson).add(KEY_REQUESTOR_EMAIL, "johndoe@example.com");
        verify(notificationJson).add(KEY_DATAFILE_ID, Long.valueOf("1"));
        verify(notificationJson).add(KEY_DATAFILE_DISPLAY_NAME, "Test File");
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Test Dataset");
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, "Test Identifier");
    }

    @Test
    public void testAddFieldsByType_requestFileAccess_nullRequestor() {
        userNotification.setType(UserNotification.Type.REQUESTFILEACCESS);
        userNotification.setObjectId(1L);
        userNotification.setRequestor(null);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getDisplayName()).thenReturn("Test Dataset");
        when(dataset.getIdentifier()).thenReturn("Test Identifier");

        DataFile dataFile = mock(DataFile.class);
        when(dataFile.getId()).thenReturn(1L);
        when(dataFile.getDisplayName()).thenReturn("Test File");
        when(dataFile.getOwner()).thenReturn(dataset);
        when(dataFileService.find(1L)).thenReturn(dataFile);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson, never()).add(eq(KEY_REQUESTOR_FIRST_NAME), anyString());
        verify(notificationJson, never()).add(eq(KEY_REQUESTOR_LAST_NAME), anyString());
        verify(notificationJson, never()).add(eq(KEY_REQUESTOR_EMAIL), anyString());
        verify(notificationJson).add(KEY_DATAFILE_ID, Long.valueOf("1"));
        verify(notificationJson).add(KEY_DATAFILE_DISPLAY_NAME, "Test File");
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Test Dataset");
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, "Test Identifier");
    }

    @Test
    public void testAddFieldsByType_grantFileAccess() {
        userNotification.setType(UserNotification.Type.GRANTFILEACCESS);
        userNotification.setObjectId(1L);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getDisplayName()).thenReturn("Test Dataset");
        when(dataset.getIdentifier()).thenReturn("Test Identifier");

        DataFile dataFile = mock(DataFile.class);
        when(dataFile.getId()).thenReturn(1L);
        when(dataFile.getDisplayName()).thenReturn("Granted File");
        when(dataFile.getOwner()).thenReturn(dataset);
        when(dataFileService.find(1L)).thenReturn(dataFile);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATAFILE_ID, Long.valueOf("1"));
        verify(notificationJson).add(KEY_DATAFILE_DISPLAY_NAME, "Granted File");
        verify(notificationJson, never()).add(eq(KEY_REQUESTOR_FIRST_NAME), anyString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Test Dataset");
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, "Test Identifier");
    }

    @Test
    public void testAddFieldsByType_grantFileAccess_objectDeleted() {
        userNotification.setType(UserNotification.Type.GRANTFILEACCESS);
        userNotification.setObjectId(1L);

        when(dataFileService.find(1L)).thenReturn(null);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_OBJECT_DELETED, true);
    }

    @Test
    public void testAddFieldsByType_createDs() {
        userNotification.setType(UserNotification.Type.CREATEDS);
        userNotification.setObjectId(1L);

        DatasetVersion datasetVersion = mock(DatasetVersion.class);
        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        when(datasetVersion.getDataset()).thenReturn(dataset);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("My Dataset");
        when(dataset.getOwner()).thenReturn(owner);

        when(datasetVersionService.find(1L)).thenReturn(datasetVersion);

        when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
        when(systemConfig.getGuidesVersion()).thenReturn("1.0");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
        verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
        verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_DATASET_MANAGEMENT_HTML);
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "My Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");
    }

    @Test
    public void testAddFieldsByType_submittedDs() {
        userNotification.setType(UserNotification.Type.SUBMITTEDDS);
        userNotification.setObjectId(1L);
        userNotification.setRequestor(requestor);

        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Submitted Dataset");
        when(dataset.getOwner()).thenReturn(owner);
        when(datasetService.find(1L)).thenReturn(dataset);

        when(owner.getAlias()).thenReturn("reviewDv");
        when(owner.getDisplayName()).thenReturn("Review Dataverse");

        when(requestor.getFirstName()).thenReturn("Jane");
        when(requestor.getLastName()).thenReturn("Submitter");
        when(requestor.getEmail()).thenReturn("j.submitter@example.com");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Submitted Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "reviewDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Review Dataverse");
        verify(notificationJson).add(KEY_REQUESTOR_FIRST_NAME, "Jane");
        verify(notificationJson).add(KEY_REQUESTOR_LAST_NAME, "Submitter");
        verify(notificationJson).add(KEY_REQUESTOR_EMAIL, "j.submitter@example.com");
    }

    @Test
    public void testAddFieldsByType_publishedDs() {
        userNotification.setType(UserNotification.Type.PUBLISHEDDS);
        userNotification.setObjectId(1L);

        DatasetVersion datasetVersion = mock(DatasetVersion.class);
        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        when(datasetVersion.getDataset()).thenReturn(dataset);

        when(dataset.getOwner()).thenReturn(owner);
        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Published Dataset");

        when(datasetVersionService.find(1L)).thenReturn(datasetVersion);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Published Dataset");
        verify(notificationJson, never()).add(eq(KEY_CURATION_STATUS), anyString());
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");
    }

    @Test
    public void testAddFieldsByType_statusUpdated() {
        userNotification.setType(UserNotification.Type.STATUSUPDATED);
        userNotification.setObjectId(1L);

        DatasetVersion datasetVersion = mock(DatasetVersion.class);
        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        CurationStatus curationStatusMock = mock(CurationStatus.class);
        when(curationStatusMock.getLabel()).thenReturn("testStatus");

        when(datasetVersion.getDataset()).thenReturn(dataset);
        when(datasetVersion.getCurrentCurationStatus()).thenReturn(curationStatusMock);
        when(datasetVersionService.find(1L)).thenReturn(datasetVersion);

        when(dataset.getOwner()).thenReturn(owner);
        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Status Update Dataset");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Status Update Dataset");
        verify(notificationJson).add(eq(KEY_CURATION_STATUS), any(String.class));
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");
    }

    @Test
    public void testAddFieldsByType_statusUpdated_objectDeleted() {
        userNotification.setType(UserNotification.Type.STATUSUPDATED);
        userNotification.setObjectId(1L);

        when(datasetVersionService.find(1L)).thenReturn(null);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_OBJECT_DELETED, true);
    }

    @Test
    public void testAddFieldsByType_createAcc() {
        userNotification.setType(UserNotification.Type.CREATEACC);
        try (MockedStatic<BrandingUtil> mockedBrandingUtil = mockStatic(BrandingUtil.class)) {
            mockedBrandingUtil.when(BrandingUtil::getInstallationBrandName).thenReturn("My Test Brand Name");

            when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
            when(systemConfig.getGuidesVersion()).thenReturn("1.0");

            sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

            verify(notificationJson).add(KEY_INSTALLATION_BRAND_NAME, "My Test Brand Name");
            verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
            verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
            verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_USER_HTML);
        }
    }

    @Test
    public void testAddFieldsByType_ingestCompleted() {
        userNotification.setType(UserNotification.Type.INGESTCOMPLETED);
        userNotification.setObjectId(1L);

        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Ingested Dataset");
        when(dataset.getOwner()).thenReturn(owner);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        when(datasetService.find(1L)).thenReturn(dataset);

        when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
        when(systemConfig.getGuidesVersion()).thenReturn("1.0");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Ingested Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");
        verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
        verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
        verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_DATASET_MANAGEMENT_TABULAR_FILES_HTML);
    }

    @Test
    public void testAddFieldsByType_ingestCompleted_objectDeleted() {
        userNotification.setType(UserNotification.Type.INGESTCOMPLETED);
        userNotification.setObjectId(1L);

        when(datasetService.find(1L)).thenReturn(null);

        when(systemConfig.getGuidesBaseUrl(false)).thenReturn("http://guides.dataverse.org");
        when(systemConfig.getGuidesVersion()).thenReturn("1.0");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_OBJECT_DELETED, true);
        verify(notificationJson).add(KEY_GUIDES_BASE_URL, "http://guides.dataverse.org");
        verify(notificationJson).add(KEY_GUIDES_VERSION, "1.0");
        verify(notificationJson).add(KEY_GUIDES_SECTION_PATH, GUIDES_SECTION_PATH_DATASET_MANAGEMENT_TABULAR_FILES_HTML);
    }

    @Test
    public void testAddFieldsByType_datasetMentioned_withJsonString() {
        // Arrange
        userNotification.setType(UserNotification.Type.DATASETMENTIONED);
        userNotification.setObjectId(1L);
        String jsonInfo = "{\"key\":\"value\", \"number\":123}";
        userNotification.setAdditionalInfo(jsonInfo);

        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Mentioned Dataset");
        when(dataset.getOwner()).thenReturn(owner);
        when(datasetService.find(1L)).thenReturn(dataset);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        // Act
        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        // Assert
        // Verify standard fields are still added
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Mentioned Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");

        // Capture the JsonValue passed for the additional info
        verify(notificationJson).add(eq(KEY_ADDITIONAL_INFO), jsonValueCaptor.capture());
        JsonValue capturedValue = jsonValueCaptor.getValue();

        // Assert that the parsed object is a JsonObject with the correct properties
        assertInstanceOf(JsonObject.class, capturedValue, "The parsed value should be a JsonObject.");
        JsonObject capturedObject = (JsonObject) capturedValue;
        assertEquals("value", capturedObject.getString("key"));
        assertEquals(123, capturedObject.getInt("number"));

        // Explicitly verify it was NOT added as a plain string
        verify(notificationJson, never()).add(KEY_ADDITIONAL_INFO, jsonInfo);
    }

    @Test
    public void testAddFieldsByType_datasetMentioned_withRegularString() {
        // Arrange
        userNotification.setType(UserNotification.Type.DATASETMENTIONED);
        userNotification.setObjectId(1L);
        String regularStringInfo = "This is just a regular string, not JSON.";
        userNotification.setAdditionalInfo(regularStringInfo);

        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Mentioned Dataset");
        when(dataset.getOwner()).thenReturn(owner);
        when(datasetService.find(1L)).thenReturn(dataset);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        // Act
        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        // Assert
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Mentioned Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");

        // Verify it falls back to adding a plain String
        verify(notificationJson).add(KEY_ADDITIONAL_INFO, regularStringInfo);
    }

    @Test
    public void testAddFieldsByType_datasetMentioned_withNullInfo() {
        // Arrange
        userNotification.setType(UserNotification.Type.DATASETMENTIONED);
        userNotification.setObjectId(1L);
        userNotification.setAdditionalInfo(null); // Set additionalInfo to null

        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Mentioned Dataset");
        when(dataset.getOwner()).thenReturn(owner);
        when(datasetService.find(1L)).thenReturn(dataset);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        // Act
        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        // Assert
        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Mentioned Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");

        // Verify that the additional info key is never added
        verify(notificationJson, never()).add(eq(KEY_ADDITIONAL_INFO), (JsonValue) any());
        verify(notificationJson, never()).add(eq(KEY_ADDITIONAL_INFO), (String) any());
    }

    @Test
    public void testAddFieldsByType_datasetMentioned() {
        userNotification.setType(UserNotification.Type.DATASETMENTIONED);
        userNotification.setObjectId(1L);
        userNotification.setAdditionalInfo("Mentioned in another dataset.");

        Dataset dataset = mock(Dataset.class);
        Dataverse owner = mock(Dataverse.class);

        when(dataset.getGlobalId()).thenReturn(testGlobalId);
        when(dataset.getDisplayName()).thenReturn("Mentioned Dataset");
        when(dataset.getOwner()).thenReturn(owner);
        when(datasetService.find(1L)).thenReturn(dataset);

        when(owner.getAlias()).thenReturn("ownerDv");
        when(owner.getDisplayName()).thenReturn("Owner Dataverse");

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verify(notificationJson).add(KEY_DATASET_PERSISTENT_ID, testGlobalId.toString());
        verify(notificationJson).add(KEY_DATASET_DISPLAY_NAME, "Mentioned Dataset");
        verify(notificationJson).add(KEY_OWNER_ALIAS, "ownerDv");
        verify(notificationJson).add(KEY_OWNER_DISPLAY_NAME, "Owner Dataverse");
        verify(notificationJson).add(KEY_ADDITIONAL_INFO, "Mentioned in another dataset.");
    }

    @Test
    public void testAddFieldsByType_noOpType() {
        // APIGENERATED is a valid type but is not handled by the switch statement,
        // so no fields should be added.
        userNotification.setType(UserNotification.Type.APIGENERATED);

        sut.addFieldsByType(notificationJson, authenticatedUser, userNotification);

        verifyNoInteractions(notificationJson);
    }
}
