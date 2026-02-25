
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.EmbargoServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean.StaticPermissionQuery;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@LocalJvmSettings
public class DatasetsEmbargoAPITest {


    @Mock
    private DataFileServiceBean fileService;

    @Mock
    private DatasetServiceBean datasetService;
    
    @Mock
    private EmbargoServiceBean embargoService;
    
    @Mock
    private PermissionServiceBean permissionService;
    
    @Mock
    private SettingsServiceBean settingsService;
    
    @Mock
    private ContainerRequestContext crc;
    
    @Mock
    private edu.harvard.iq.dataverse.Dataset dataset;
    
    @Mock
    private edu.harvard.iq.dataverse.DataFile file;
    
    @Mock
    private DatasetVersion datasetVersion;
    
    @Mock
    private TermsOfUseAndAccess termsOfUseAndAccess;
    
    @Mock
    private StaticPermissionQuery permissionQuery;
    
    @InjectMocks
    private Datasets datasetsApi;
    
    private AuthenticatedUser testUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new AuthenticatedUser();
        testUser.setId(1L);
        testUser.setSuperuser(false);
        
        // Mock the authentication
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
            .thenReturn(testUser);
        
        // Mock dataset lookup
        when(datasetService.find(1L)).thenReturn(dataset);
        
        // Mock dataset version chain
        when(dataset.getLatestVersion()).thenReturn(datasetVersion);
        when(dataset.getFiles()).thenReturn(List.of(file));
        when(datasetVersion.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccess);
        when(datasetVersion.getVersionState()).thenReturn(DatasetVersion.VersionState.DRAFT);
        when(termsOfUseAndAccess.getDatasetVersion()).thenReturn(datasetVersion);
        
        // Mock file lookup
        when(fileService.find(2L)).thenReturn(file);
        when(fileService.save(any(DataFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

      
        
        // Mock permission check
        when(permissionService.userOn(eq(testUser), eq(dataset)))
            .thenReturn(permissionQuery);
        when(permissionQuery.has(Permission.EditDataset)).thenReturn(true);
        
        // Mock setting
        when(settingsService.getValueForKey(SettingsServiceBean.Key.MaxEmbargoDurationInMonths)).thenReturn("12");
        
        // Mock embargoService
        when(embargoService.merge(any(Embargo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(embargoService.save(any(Embargo.class), any(String.class))).thenReturn(1L);
        
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "require-embargo-reason")
    public void testCreateFileEmbargo_withReasonRequired_shouldRejectNullReason() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        JsonObjectBuilder embargoJson = Json.createObjectBuilder()
                .add("dateAvailable", futureDate.toString())
                .add("fileIds", Json.createArrayBuilder().add(1L));
        
        // Act
        Response response = datasetsApi.createFileEmbargo(crc, "1", embargoJson.build().toString());
        
        // Assert
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        if (response.hasEntity()) {
            Object entity = response.getEntity();
            if (entity instanceof JsonObject) {
                JsonObject jsonResponse = (JsonObject) entity;
                String message = jsonResponse.getString("message", "");
                assertTrue(message.contains("Reason is required") || message.contains("reason"),
                    "Expected error message about required reason, got: " + message);
            }
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "require-embargo-reason")
    public void testCreateFileEmbargo_withReasonRequired_shouldAcceptValidReason() throws CommandException {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        JsonObjectBuilder embargoJson = Json.createObjectBuilder()
                .add("dateAvailable", futureDate.toString())
                .add("reason", "Valid embargo reason for testing")
                .add("fileIds", Json.createArrayBuilder().add(2L));
        
        // Act
        Response response = datasetsApi.createFileEmbargo(crc, "1", embargoJson.build().toString());
        
        // Assert
        assertTrue(response.getStatus() == OK.getStatusCode());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "require-embargo-reason")
    public void testCreateFileEmbargo_withReasonNotRequired_shouldAcceptNullReason() throws CommandException {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        JsonObjectBuilder embargoJson = Json.createObjectBuilder()
                .add("dateAvailable", futureDate.toString())
                .add("fileIds", Json.createArrayBuilder().add(2L));
        
        // Act
        Response response = datasetsApi.createFileEmbargo(crc, "1", embargoJson.build().toString());
        
        // Assert
        // Should not get BAD_REQUEST for missing reason when flag is disabled
        if (response.getStatus() == BAD_REQUEST.getStatusCode() && response.hasEntity()) {
            Object entity = response.getEntity();
            if (entity instanceof JsonObject) {
                JsonObject jsonResponse = (JsonObject) entity;
                String message = jsonResponse.getString("message", "");
                assertTrue(!message.contains("Reason is required"),
                    "Should not require reason when flag is disabled, got: " + message);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n", "  \t\n  "})
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "require-embargo-reason")
    public void testCreateFileEmbargo_shouldRejectBlankReason_regardlessOfFlag(String blankReason) {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        JsonObjectBuilder embargoJson = Json.createObjectBuilder()
                .add("dateAvailable", futureDate.toString())
                .add("reason", blankReason)
                .add("fileIds", Json.createArrayBuilder().add(2L));
        
        // Act
        Response response = datasetsApi.createFileEmbargo(crc, "1", embargoJson.build().toString());
        
        // Assert
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        if (response.hasEntity()) {
            Object entity = response.getEntity();
            if (entity instanceof JsonObject) {
                JsonObject jsonResponse = (JsonObject) entity;
                String message = jsonResponse.getString("message", "");
                assertTrue(message.contains("blank") || message.contains("empty"),
                    "Expected error message about blank reason, got: " + message);
            }
        }
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "require-embargo-reason")
    public void testCreateFileEmbargo_withReasonRequired_shouldRejectEmptyString() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        JsonObjectBuilder embargoJson = Json.createObjectBuilder()
                .add("dateAvailable", futureDate.toString())
                .add("reason", "")
                .add("fileIds", Json.createArrayBuilder().add(2L));
        
        // Act
        Response response = datasetsApi.createFileEmbargo(crc, "1", embargoJson.build().toString());
        
        // Assert
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        if (response.hasEntity()) {
            Object entity = response.getEntity();
            if (entity instanceof JsonObject) {
                JsonObject jsonResponse = (JsonObject) entity;
                String message = jsonResponse.getString("message", "");
                assertTrue(message.contains("blank") || message.contains("empty") || message.contains("reason"),
                    "Expected error message about blank/empty reason, got: " + message);
            }
        }
    }
}