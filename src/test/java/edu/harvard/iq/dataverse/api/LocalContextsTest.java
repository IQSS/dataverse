package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@LocalJvmSettings
@ExtendWith(MockitoExtension.class)
public class LocalContextsTest {

    @InjectMocks
    private LocalContexts localContexts;

    @Mock
    private DatasetServiceBean datasetService;
    @Mock
    private UserServiceBean userService;
    @Mock
    private SystemConfig systemConfig;
    @Mock
    private PermissionsWrapper permissionsWrapper;
    @Mock
    private PermissionServiceBean permissionService;
    @Mock
    private AuthenticatedUser authUser;
    @Mock
    private Dataset dataset;
    @Mock
    private ContainerRequestContext crc;

    private static final String RANDOM_API_KEY = "random_api_key_12345";

    @Test
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://sandbox.localcontextshub.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    public void testSearchLocalContexts_ServiceUnavailable() {
        // given
        when(datasetService.find(1L)).thenReturn(dataset);
        // No mock for http client, so it will fail and return SERVICE_UNAVAILABLE

        // when
        Response response = localContexts.searchLocalContexts("1", "2");

        // then
        assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://sandbox.localcontextshub.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    public void testGetLocalContextsProject_ServiceUnavailable() {
        // given
        when(datasetService.find(1L)).thenReturn(dataset);
        when(dataset.getGlobalId()).thenReturn(new GlobalId("doi", "10.5072", "FK2ABCDEF", "/","https://doi.org/", "test"));
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
        .thenReturn(GuestUser.get());

        // No mock for http client, so it will fail and return SERVICE_UNAVAILABLE

        // when
        Response response = localContexts.getDatasetLocalContexts(crc ,"1");

        // then
        assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://sandbox.localcontextshub.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "add-local-contexts-permission-check")
    public void testGetLocalContextsProject_Forbidden() {
        assertTrue(FeatureFlags.ADD_LOCAL_CONTEXTS_PERMISSION_CHECK.enabled());
        // given
        AuthenticatedUser au = new AuthenticatedUser();
        au.setId(1L);
        when(datasetService.find(1L)).thenReturn(dataset);
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
        .thenReturn(au);
        PermissionServiceBean.StaticPermissionQuery staticPermissionQuery = Mockito.mock(PermissionServiceBean.StaticPermissionQuery.class);
        when(permissionService.userOn(Mockito.any(AuthenticatedUser.class), Mockito.any(Dataset.class))).thenReturn(staticPermissionQuery);
        when(staticPermissionQuery.has(Permission.EditDataset)).thenReturn(false);

        // when
        Response response = localContexts.getDatasetLocalContexts(crc, "1");

        // then
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
    }
    
    @Test
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_URL, value = "https://sandbox.localcontextshub.org/")
    @JvmSetting(key = JvmSettings.LOCALCONTEXTS_API_KEY, value = RANDOM_API_KEY)
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "add-local-contexts-permission-check")
    public void testGetLocalContextsProject_Allowed() {
        assertTrue(FeatureFlags.ADD_LOCAL_CONTEXTS_PERMISSION_CHECK.enabled());
        // given
        AuthenticatedUser au = new AuthenticatedUser();
        au.setId(1L);
        when(datasetService.find(1L)).thenReturn(dataset);
        when(dataset.getGlobalId()).thenReturn(new GlobalId("doi", "10.5072", "FK2ABCDEF", "/","https://doi.org/", "test"));
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
        .thenReturn(au);
        PermissionServiceBean.StaticPermissionQuery staticPermissionQuery = Mockito.mock(PermissionServiceBean.StaticPermissionQuery.class);
        when(permissionService.userOn(Mockito.any(AuthenticatedUser.class), Mockito.any(Dataset.class))).thenReturn(staticPermissionQuery);
        when(staticPermissionQuery.has(Permission.EditDataset)).thenReturn(true);

        // when
        Response response = localContexts.getDatasetLocalContexts(crc, "1");

        // then
        assertEquals(SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testSearchLocalContexts_NoConfig() {

        // when
        Response response = localContexts.searchLocalContexts("1", "2");

        // then
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetLocalContextsProject_NoConfig() {

        when(datasetService.find(1L)).thenReturn(dataset);
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
        .thenReturn(GuestUser.get());
        // when
        Response response = localContexts.getDatasetLocalContexts(crc, "1");

        // then
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }
}