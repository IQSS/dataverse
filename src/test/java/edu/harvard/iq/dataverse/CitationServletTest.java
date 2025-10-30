package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.pidproviders.PidProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.UnmanagedDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandleProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.handle.UnmanagedHandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.UnmanagedPermaLinkPidProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
//HANDLE 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "HDL 1", varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = HandlePidProvider.TYPE, varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "1902.1", varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "test", varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_MANAGED_LIST, value = "hdl:1902.1/FK2ABCDEF", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_AUTH_HANDLE, value = "1902.1/ADMIN", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_INDEPENDENT_SERVICE, value = "true", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_INDEX, value = "1", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_KEY_PASSPHRASE, value = "passphrase", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_KEY_PATH, value = "/tmp/cred", varArgs ="hdl1")
//List to instantiate
@JvmSetting(key = JvmSettings.PID_PROVIDERS, value = "hdl1")
public class CitationServletTest {

    @Mock
    DvObjectServiceBean dvObjectService;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    

    static CitationServlet citationServlet = new CitationServlet();

    @BeforeAll
    public static void setUp() {
        Map<String, PidProviderFactory> pidProviderFactoryMap = new HashMap<>();
        pidProviderFactoryMap.put(HandlePidProvider.TYPE, new HandleProviderFactory());
        PidUtil.clearPidProviders();

        //Read list of providers to add
        List<String> providers = Arrays.asList(JvmSettings.PID_PROVIDERS.lookup().split(",\\s"));
        //Iterate through the list of providers and add them using the PidProviderFactory of the appropriate type
        for (String providerId : providers) {
            System.out.println("Loading provider: " + providerId);
            String type = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
            PidProviderFactory factory = pidProviderFactoryMap.get(type);
            PidUtil.addToProviderList(factory.createPidProvider(providerId));
        }
        PidUtil.addAllToUnmanagedProviderList(Arrays.asList(new UnmanagedDOIProvider(),
                new UnmanagedHandlePidProvider(), new UnmanagedPermaLinkPidProvider()));
    }
    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHandleRedirect() throws ServletException, IOException {
        String pidString = "hdl:1902.1/test10052";
        DvObject dvObj = new Dataset();
        citationServlet.dvObjectService = dvObjectService = Mockito.mock(DvObjectServiceBean.class);
        when(dvObjectService.findByGlobalId(any(GlobalId.class))).thenReturn(null);
        when(dvObjectService.findByAltGlobalId(any(GlobalId.class), any())).thenReturn(dvObj);
        when(request.getParameter("persistentId")).thenReturn(pidString);
        ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
        doNothing().when(response).sendRedirect(valueCapture.capture());

        citationServlet.doGet(request, response);
        assertEquals("dataset.xhtml?persistentId=hdl:1902.1/test10052", valueCapture.getValue());
    }
}
