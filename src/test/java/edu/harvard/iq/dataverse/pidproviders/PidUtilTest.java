package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Useful for testing but requires DataCite credentials, etc.
 */
@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
public class PidUtilTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
//        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol)).thenReturn("perma");
//        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority)).thenReturn("DANSLINK");
    }
    
    @Disabled
    @Test
    public void testGetDoi() throws IOException {
        String username = System.getenv("DataCiteUsername");
        String password = System.getenv("DataCitePassword");
        String baseUrl = "https://api.test.datacite.org";
        GlobalId pid = new GlobalId(DOIProvider.DOI_PROTOCOL,"10.70122","QE5A-XN55", "/", DOIProvider.DOI_RESOLVER_URL, null);
        try {
            JsonObjectBuilder result = PidUtil.queryDoi(pid, baseUrl, username, password);
            String out = JsonUtil.prettyPrint(result.build());
            System.out.println("out: " + out);
        } catch (NotFoundException ex) {
            System.out.println("ex: " + ex);
        }
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_PROVIDER_NAME, value = "perma1", varArgs = "api-bearer-auth")
    @JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma1")
    @JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma1")
    @JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QE", varArgs = "perma1")
    @JvmSetting(key = JvmSettings.PID_PROVIDER_IDENTIFIER_GENERATION_STYLE, value = PermaLinkPidProvider.TYPE, varArgs = "perma1")
    public void testGetPermaLink() throws IOException {
        
        PidProvider p = new PermaLinkProviderFactory().createPidProvider("perma1");
        PidUtil.clearPidProviders();
        PidUtil.addToProviderList(p);
        GlobalId pid = new GlobalId(PermaLinkPidProvider.PERMA_PROTOCOL,"DANSLINK","QE5A-XN55", "", p.getUrlPrefix(), "perma1");
        System.out.println(pid.asString());
        System.out.println(pid.asURL());
        
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid.asString());
        assertEquals(pid.asString(), pid2.asString());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid.asURL());
        assertEquals(pid.asString(), pid3.asString());
        
    }

}
