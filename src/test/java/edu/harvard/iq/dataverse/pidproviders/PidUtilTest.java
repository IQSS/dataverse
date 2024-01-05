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
import org.junit.jupiter.api.BeforeAll;
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
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma1", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QE", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "-", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma2", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "/", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QE", varArgs = "perma2")
public class PidUtilTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;

    @BeforeAll
    //FWIW @JvmSetting doesn't appear to work with @BeforeAll
    public static void setUpClass() throws Exception {
        //ToDo - permalinks: use one "" separator and update code to allow prioritizing PidProviders
        PidProvider p = new PermaLinkProviderFactory().createPidProvider("perma1");
        PidUtil.clearPidProviders();
        PidUtil.addToProviderList(p);
        p = new PermaLinkProviderFactory().createPidProvider("perma2");
        PidUtil.addToProviderList(p);

    }
    
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
    public void testGetPermaLink() throws IOException {
        PidProvider p = PidUtil.getPidProvider("perma1");
            GlobalId pid = new GlobalId(PermaLinkPidProvider.PERMA_PROTOCOL,"DANSLINK","QE-5A-XN55", "-", p.getUrlPrefix(), "perma1");
        System.out.println(pid.asString());
        System.out.println(pid.asURL());
        
        
        
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid.asString());
        assertEquals(pid.asString(), pid2.asString());
        assertEquals("perma1", pid2.getProviderName());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid.asURL());
        assertEquals(pid.asString(), pid3.asString());
        assertEquals("perma1", pid3.getProviderName());

        p = PidUtil.getPidProvider("perma2");
        GlobalId pid5 = new GlobalId(PermaLinkPidProvider.PERMA_PROTOCOL,"DANSLINK","QE/5A-XN55", "/", p.getUrlPrefix(), "perma2");
        GlobalId pid6 = PidUtil.parseAsGlobalID(pid5.asString());
        assertEquals("perma2", pid6.getProviderName());
        assertEquals(pid5.asString(), pid6.asString());
        

    }

}
