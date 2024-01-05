package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
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
//Perma 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma 1", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QE", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "-", varArgs = "perma1")
//Perma 2
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma 2", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QE", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "/", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PERMALINK_BASE_URL, value = "https://example.org/123", varArgs = "perma2")
// Datacite 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "dataCite 1", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = DataCiteDOIProvider.TYPE, varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.5072", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "FK2", varArgs = "dc1")
@JvmSetting(key = JvmSettings.DATACITE_MDS_API_URL, value = "https://mds.test.api.org/", varArgs = "dc1")
@JvmSetting(key = JvmSettings.DATACITE_REST_API_URL, value = "https://api.test.datacite.org", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_USERNAME, value = "test", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_PASSWORD, value = "changeme", varArgs ="dc1")
//List to instantiate
@JvmSetting(key = JvmSettings.PID_PROVIDERS, value = "perma1, perma2, dc1")

public class PidUtilTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;

    @BeforeAll
    //FWIW @JvmSetting doesn't appear to work with @BeforeAll
    public static void setUpClass() throws Exception {

        //This mimics the initial config in the PidProviderFactoryBean.loadProviderFactories method - could potentially be used to mock that bean at some point
        Map<String, PidProviderFactory> pidProviderFactoryMap = new HashMap<>();
        pidProviderFactoryMap.put(PermaLinkPidProvider.TYPE, new PermaLinkProviderFactory());
        pidProviderFactoryMap.put(DataCiteDOIProvider.TYPE, new DataCiteProviderFactory());
        pidProviderFactoryMap.put(HandlePidProvider.TYPE, new HandleProviderFactory());
        pidProviderFactoryMap.put(FakeDOIProvider.TYPE, new FakeProviderFactory());
        pidProviderFactoryMap.put(EZIdDOIProvider.TYPE, new EZIdProviderFactory());
        
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
    }
    
    @AfterAll
    public static void tearDownClass() throws Exception {
        PidUtil.clearPidProviders();
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
    public void testFactories() throws IOException {
        PidProvider p = PidUtil.getPidProvider("perma1");
        assertEquals("perma 1", p.getLabel());
        assertEquals(PermaLinkPidProvider.PERMA_PROTOCOL, p.getProtocol());
        assertEquals("DANSLINK", p.getAuthority());
        assertEquals("QE", p.getShoulder());
        assertEquals("-", p.getSeparator());
        assertTrue(p.getUrlPrefix().startsWith(SystemConfig.getDataverseSiteUrlStatic()));
        p = PidUtil.getPidProvider("perma2");
        assertTrue(p.getUrlPrefix().startsWith("https://example.org/123"));
        
    }
    
    @Test
    public void testPermaLinkParsing() throws IOException {
        //Verify that we can parse a valid perma link associated with perma1
        String pid1String = "perma:DANSLINK-QE-5A-XN55";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        //Check that it was parsed by perma1 and that the URL is correct, etc
        assertEquals("perma1", pid2.getProviderId());
        assertEquals(SystemConfig.getDataverseSiteUrlStatic() + "/citation?persistentId=" + pid1String, pid2.asURL());
        assertEquals("DANSLINK", pid2.getAuthority());
        assertEquals(PermaLinkPidProvider.PERMA_PROTOCOL, pid2.getProtocol());
        
        //Verify that parsing the URL form works
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String, pid3.asString());
        assertEquals("perma1", pid3.getProviderId());

        //Repeat the basics with a permalink associated with perma2
        String  pid4String = "perma:DANSLINK/QE-5A-XN55";
        GlobalId pid5 = PidUtil.parseAsGlobalID(pid4String);
        assertEquals("perma2", pid5.getProviderId());
        assertEquals(pid4String, pid5.asString());
        assertEquals("https://example.org/123/citation?persistentId=" + pid4String, pid5.asURL());

    }
    
    @Test
    public void testDOIParsing() throws IOException {
        
        String pid1String = "doi:10.5072/FK2ABCDEF";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        assertEquals("dc1", pid2.getProviderId());
        assertEquals("https://doi.org/" + pid2.getAuthority() + PidUtil.getPidProvider(pid2.getProviderId()).getSeparator() + pid2.getIdentifier(),pid2.asURL());
        assertEquals("10.5072", pid2.getAuthority());
        assertEquals(DOIProvider.DOI_PROTOCOL, pid2.getProtocol());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String, pid3.asString());
        assertEquals("dc1", pid3.getProviderId());
    }

}
