package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.UnmanagedDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.crossref.CrossRefDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.crossref.CrossRefDOIProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DataCiteDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DataCiteProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.doi.ezid.EZIdDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.ezid.EZIdProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.doi.fake.FakeDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.fake.FakeProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandleProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.handle.UnmanagedHandlePidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.perma.UnmanagedPermaLinkPidProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
//Perma 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma 1", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QE", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "-", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_EXCLUDED_LIST, value = "perma:DANSLINKQE123456, perma:bad, perma:LINKIT123456", varArgs ="perma1")

//Perma 2
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma 2", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "DANSLINK", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "QQ", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_MANAGED_LIST, value = "perma:LINKIT/FK2ABCDEF", varArgs ="perma2")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "/", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PERMALINK_BASE_URL, value = "https://example.org/123/citation?persistentId=perma:", varArgs = "perma2")
// Datacite 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "dataCite 1", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = DataCiteDOIProvider.TYPE, varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.5073", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "FK2", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_EXCLUDED_LIST, value = "doi:10.5073/FK2123456", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_MDS_API_URL, value = "https://mds.test.datacite.org/", varArgs = "dc1")
@JvmSetting(key = JvmSettings.DATACITE_REST_API_URL, value = "https://api.test.datacite.org", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_USERNAME, value = "test", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_PASSWORD, value = "changeme", varArgs ="dc1")
//Datacite 2
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "dataCite 2", varArgs = "dc2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = DataCiteDOIProvider.TYPE, varArgs = "dc2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.5072", varArgs = "dc2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "FK3", varArgs = "dc2")
@JvmSetting(key = JvmSettings.DATACITE_MDS_API_URL, value = "https://mds.test.datacite.org/", varArgs = "dc2")
@JvmSetting(key = JvmSettings.DATACITE_REST_API_URL, value = "https://api.test.datacite.org", varArgs ="dc2")
@JvmSetting(key = JvmSettings.DATACITE_USERNAME, value = "test2", varArgs ="dc2")
@JvmSetting(key = JvmSettings.DATACITE_PASSWORD, value = "changeme2", varArgs ="dc2")
//EZID 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "EZId 1", varArgs = "ez1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = EZIdDOIProvider.TYPE, varArgs = "ez1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.5072", varArgs = "ez1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "FK2", varArgs = "ez1")
@JvmSetting(key = JvmSettings.EZID_API_URL, value = "https://ezid.cdlib.org/", varArgs = "ez1")
@JvmSetting(key = JvmSettings.EZID_USERNAME, value = "apitest", varArgs ="ez1")
@JvmSetting(key = JvmSettings.EZID_PASSWORD, value = "apitest", varArgs ="ez1")
//FAKE 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "FAKE 1", varArgs = "fake1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = FakeDOIProvider.TYPE, varArgs = "fake1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.5074", varArgs = "fake1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "fk", varArgs = "fake1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_MANAGED_LIST, value = "doi:10.5073/FK3ABCDEF", varArgs ="fake1")

//HANDLE 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "HDL 1", varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = HandlePidProvider.TYPE, varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "20.500.1234", varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "", varArgs = "hdl1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_MANAGED_LIST, value = "hdl:20.20.20/FK2ABCDEF", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_AUTH_HANDLE, value = "20.500.1234/ADMIN", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_INDEPENDENT_SERVICE, value = "true", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_INDEX, value = "1", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_KEY_PASSPHRASE, value = "passphrase", varArgs ="hdl1")
@JvmSetting(key = JvmSettings.HANDLENET_KEY_PATH, value = "/tmp/cred", varArgs ="hdl1")

// CrossRef 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "CrossRef 1", varArgs = "crossref1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = CrossRefDOIProvider.TYPE, varArgs = "crossref1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.11111", varArgs = "crossref1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "DVN/", varArgs = "crossref1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_MANAGED_LIST, value = "", varArgs ="crossref1")
@JvmSetting(key = JvmSettings.CROSSREF_URL, value = "https://doi.crossref.org", varArgs ="crossref1")
@JvmSetting(key = JvmSettings.CROSSREF_REST_API_URL, value = "https://test.crossref.org", varArgs ="crossref1")
@JvmSetting(key = JvmSettings.CROSSREF_USERNAME, value = "crusername", varArgs ="crossref1")
@JvmSetting(key = JvmSettings.CROSSREF_PASSWORD, value = "secret", varArgs ="crossref1")
@JvmSetting(key = JvmSettings.CROSSREF_DEPOSITOR, value = "xyz", varArgs ="crossref1")
@JvmSetting(key = JvmSettings.CROSSREF_DEPOSITOR_EMAIL, value = "xyz@example.com", varArgs ="crossref1")
//List to instantiate
@JvmSetting(key = JvmSettings.PID_PROVIDERS, value = "perma1, perma2, dc1, dc2, ez1, fake1, hdl1, crossref1")

public class PidUtilTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;
    
    static PidProviderFactoryBean pidService;

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
        pidProviderFactoryMap.put(CrossRefDOIProvider.TYPE, new CrossRefDOIProviderFactory());
        
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
    
    /**
     * Useful for testing but requires DataCite credentials, etc.
     */
    @Disabled
    @Test
    public void testGetDoi() throws IOException {
        String username = System.getenv("DataCiteUsername");
        String password = System.getenv("DataCitePassword");
        String baseUrl = "https://api.test.datacite.org";
        GlobalId pid = new GlobalId(AbstractDOIProvider.DOI_PROTOCOL,"10.70122","QE5A-XN55", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null);
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
        assertTrue(p.getUrlPrefix().startsWith("https://example.org/123/citation?persistentId="));
        p = PidUtil.getPidProvider("dc2");
        assertEquals("FK3", p.getShoulder());
        
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
        String  pid4String = "perma:DANSLINK/QQ-5A-XN55";
        GlobalId pid5 = PidUtil.parseAsGlobalID(pid4String);
        assertEquals("perma2", pid5.getProviderId());
        assertEquals(pid4String, pid5.asString());
        assertEquals("https://example.org/123/citation?persistentId=" + pid4String, pid5.asURL());

    }
    
    @Test
    public void testPermaLinkGenerationiWithSeparator() throws IOException {
        Dataset ds = new Dataset();
        pidService = Mockito.mock(PidProviderFactoryBean.class);
        Mockito.when(pidService.isGlobalIdLocallyUnique(any(GlobalId.class))).thenReturn(true);
        PidProvider p = PidUtil.getPidProvider("perma1");
        p.setPidProviderServiceBean(pidService);
        p.generatePid(ds);
        System.out.println("DS sep " + ds.getSeparator());
        System.out.println("Generated perma identifier" + ds.getGlobalId().asString());
        System.out.println("Provider prefix for perma identifier" + p.getAuthority() + p.getSeparator() + p.getShoulder());
        assertTrue(ds.getGlobalId().asRawIdentifier().startsWith(p.getAuthority() + p.getSeparator() + p.getShoulder()));
    }
    
    @Test
    public void testDOIParsing() throws IOException {
        
        String pid1String = "doi:10.5073/FK2ABCDEF";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        assertEquals("dc1", pid2.getProviderId());
        assertEquals("https://doi.org/" + pid2.getAuthority() + PidUtil.getPidProvider(pid2.getProviderId()).getSeparator() + pid2.getIdentifier(),pid2.asURL());
        assertEquals("10.5073", pid2.getAuthority());
        assertEquals(AbstractDOIProvider.DOI_PROTOCOL, pid2.getProtocol());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String, pid3.asString());
        assertEquals("dc1", pid3.getProviderId());
        
        //Also test case insensitive
        String pid4String = "doi:10.5072/fk3ABCDEF";
        GlobalId pid4 = PidUtil.parseAsGlobalID(pid4String);
        // Lower case is recognized by converting to upper case internally, so we need to test vs. the upper case identifier
        // I.e. we are verifying that the lower case string is parsed the same as the upper case string, both give an internal upper case PID representation
        assertEquals("doi:10.5072/FK3ABCDEF", pid4.asString());
        assertEquals("dc2", pid4.getProviderId());

        String pid5String = "doi:10.5072/FK2ABCDEF";
        GlobalId pid5 = PidUtil.parseAsGlobalID(pid5String);
        assertEquals(pid5String, pid5.asString());
        assertEquals("ez1", pid5.getProviderId());
        
        String pid6String = "doi:10.5074/FKABCDEF";
        GlobalId pid6 = PidUtil.parseAsGlobalID(pid6String);
        assertEquals(pid6String, pid6.asString());
        assertEquals("fake1", pid6.getProviderId());

        String pid7String = "doi:10.11111/DVN/ABCDEF";
        GlobalId pid7 = PidUtil.parseAsGlobalID(pid7String);
        assertEquals(pid7String, pid7.asString());
        assertEquals("crossref1", pid7.getProviderId());
    }
    
    @Test
    public void testHandleParsing() throws IOException {
        
        String pid1String = "hdl:20.500.1234/10052";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        assertEquals("hdl1", pid2.getProviderId());
        assertEquals("https://hdl.handle.net/" + pid2.getAuthority() + PidUtil.getPidProvider(pid2.getProviderId()).getSeparator() + pid2.getIdentifier(),pid2.asURL());
        assertEquals("20.500.1234", pid2.getAuthority());
        assertEquals(HandlePidProvider.HDL_PROTOCOL, pid2.getProtocol());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String, pid3.asString());
        assertEquals("hdl1", pid3.getProviderId());
    }

    @Test
    public void testUnmanagedParsing() throws IOException {
        // A handle managed not managed in the hdl1 provider
        String pid1String = "hdl:20.500.3456/10052";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        //Only parsed by the unmanaged provider
        assertEquals(UnmanagedHandlePidProvider.ID, pid2.getProviderId());
        assertEquals(HandlePidProvider.HDL_RESOLVER_URL + pid2.getAuthority() + PidUtil.getPidProvider(pid2.getProviderId()).getSeparator() + pid2.getIdentifier(),pid2.asURL());
        assertEquals("20.500.3456", pid2.getAuthority());
        assertEquals(HandlePidProvider.HDL_PROTOCOL, pid2.getProtocol());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String, pid3.asString());
        assertEquals(UnmanagedHandlePidProvider.ID, pid3.getProviderId());
        
        //Same for DOIs
        String pid5String = "doi:10.6083/FK2ABCDEF";
        GlobalId pid5 = PidUtil.parseAsGlobalID(pid5String);
        assertEquals(pid5String, pid5.asString());
        assertEquals(UnmanagedDOIProvider.ID, pid5.getProviderId());

        //And Permalinks
        String pid6String = "perma:NOTDANSQEABCDEF";
        GlobalId pid6 = PidUtil.parseAsGlobalID(pid6String);
        assertEquals(pid6String, pid6.asString());
        assertEquals(UnmanagedPermaLinkPidProvider.ID, pid6.getProviderId());
        
        //Lowercase test for unmanaged DOIs
        String pid7String = "doi:10.5281/zenodo.6381129";
        GlobalId pid7 = PidUtil.parseAsGlobalID(pid7String);
        assertEquals(UnmanagedDOIProvider.ID, pid5.getProviderId());
        assertEquals(pid7String.toUpperCase().replace("DOI", "doi"), pid7.asString());
        

    }
    
    @Test
    public void testExcludedSetParsing() throws IOException {
        
        String pid1String = "doi:10.5073/FK2123456";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        assertEquals(UnmanagedDOIProvider.ID, pid2.getProviderId());
        assertEquals("https://doi.org/" + pid2.getAuthority() + PidUtil.getPidProvider(pid2.getProviderId()).getSeparator() + pid2.getIdentifier(),pid2.asURL());
        assertEquals("10.5073", pid2.getAuthority());
        assertEquals(AbstractDOIProvider.DOI_PROTOCOL, pid2.getProtocol());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String, pid3.asString());
        assertEquals(UnmanagedDOIProvider.ID, pid3.getProviderId());
        
        String pid4String = "perma:bad";
        GlobalId pid4 = PidUtil.parseAsGlobalID(pid4String);
        assertEquals(pid4String, pid4.asString());
        assertEquals(UnmanagedPermaLinkPidProvider.ID, pid4.getProviderId());

        String pid5String = "perma:DANSLINKQE123456";
        GlobalId pid5 = PidUtil.parseAsGlobalID(pid5String);
        assertEquals(pid5String, pid5.asString());
        assertEquals(UnmanagedPermaLinkPidProvider.ID, pid5.getProviderId());
        
        String pid6String = "perma:LINKIT123456";
        GlobalId pid6 = PidUtil.parseAsGlobalID(pid6String);
        assertEquals(pid6String, pid6.asString());
        assertEquals(UnmanagedPermaLinkPidProvider.ID, pid6.getProviderId());


    }
    
    @Test
    public void testManagedSetParsing() throws IOException {
        
        String pid1String = "doi:10.5073/fk3ABCDEF";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String.toUpperCase().replace("DOI", "doi"), pid2.asString());
        assertEquals("fake1", pid2.getProviderId());
        assertEquals("https://doi.org/" + pid2.getAuthority() + PidUtil.getPidProvider(pid2.getProviderId()).getSeparator() + pid2.getIdentifier(),pid2.asURL());
        assertEquals("10.5073", pid2.getAuthority());
        assertEquals(AbstractDOIProvider.DOI_PROTOCOL, pid2.getProtocol());
        GlobalId pid3 = PidUtil.parseAsGlobalID(pid2.asURL());
        assertEquals(pid1String.toUpperCase().replace("DOI", "doi"), pid3.asString());
        assertEquals("fake1", pid3.getProviderId());
        assertFalse(PidUtil.getPidProvider(pid3.getProviderId()).canCreatePidsLike(pid3));
        
        String pid4String = "hdl:20.20.20/FK2ABCDEF";
        GlobalId pid4 = PidUtil.parseAsGlobalID(pid4String);
        assertEquals(pid4String, pid4.asString());
        assertEquals("hdl1", pid4.getProviderId());
        assertFalse(PidUtil.getPidProvider(pid4.getProviderId()).canCreatePidsLike(pid4));

        String pid5String = "perma:LINKIT/FK2ABCDEF";
        GlobalId pid5 = PidUtil.parseAsGlobalID(pid5String);
        assertEquals(pid5String, pid5.asString());
        assertEquals("perma2", pid5.getProviderId());
        assertFalse(PidUtil.getPidProvider(pid5.getProviderId()).canCreatePidsLike(pid5));
    }
    
    @Test
    public void testFindingPidGenerators() throws IOException {
        
        Dataset dataset1 = new Dataset();
        Dataverse dataverse1 = new Dataverse();
        dataset1.setOwner(dataverse1);
        String pidGeneratorSpecs = Json.createObjectBuilder().add("protocol", AbstractDOIProvider.DOI_PROTOCOL).add("authority","10.5072").add("shoulder", "FK2").build().toString();
        //Set a PID generator on the parent
        dataverse1.setPidGeneratorSpecs(pidGeneratorSpecs);
        assertEquals(pidGeneratorSpecs, dataverse1.getPidGeneratorSpecs());
        //Verify that the parent's PID generator is the effective one
        assertEquals("ez1", dataverse1.getEffectivePidGenerator().getId());
        assertEquals("ez1", dataset1.getEffectivePidGenerator().getId());
        //Change dataset to have a provider and verify that it is used instead of any effective one
        dataset1.setAuthority("10.5073");
        dataset1.setProtocol(AbstractDOIProvider.DOI_PROTOCOL);
        dataset1.setIdentifier("FK2ABCDEF");
        //Reset to get rid of cached @transient value
        dataset1.setPidGenerator(null);
        assertEquals("dc1", dataset1.getGlobalId().getProviderId());
        assertEquals("dc1", dataset1.getEffectivePidGenerator().getId());
        assertTrue(PidUtil.getPidProvider(dataset1.getEffectivePidGenerator().getId()).canCreatePidsLike(dataset1.getGlobalId()));
        
        dataset1.setPidGenerator(null);
        //Now set identifier so that the provider has this one in it's managed list (and therefore we can't mint new PIDs in the same auth/shoulder) and therefore we get the effective pid generator
        dataset1.setIdentifier("FK3ABCDEF");
        assertEquals("fake1", dataset1.getGlobalId().getProviderId());
        assertEquals("ez1", dataset1.getEffectivePidGenerator().getId());
        
        //Now test failure case
        dataverse1.setPidGenerator(null);
        dataset1.setPidGenerator(null);
        pidGeneratorSpecs = Json.createObjectBuilder().add("protocol", AbstractDOIProvider.DOI_PROTOCOL).add("authority","10.9999").add("shoulder", "FK2").build().toString();
        //Set a PID generator on the parent
        dataverse1.setPidGeneratorSpecs(pidGeneratorSpecs);
        assertEquals(pidGeneratorSpecs, dataverse1.getPidGeneratorSpecs());
        //Verify that the parent's PID generator is the effective one
        assertNull(dataverse1.getEffectivePidGenerator());
        assertNull(dataset1.getEffectivePidGenerator());
    }
    
    @Test
    @JvmSetting(key = JvmSettings.LEGACY_DATACITE_MDS_API_URL, value = "https://mds.test.datacite.org/")
    @JvmSetting(key = JvmSettings.LEGACY_DATACITE_REST_API_URL, value = "https://api.test.datacite.org")
    @JvmSetting(key = JvmSettings.LEGACY_DATACITE_USERNAME, value = "test2")
    @JvmSetting(key = JvmSettings.LEGACY_DATACITE_PASSWORD, value = "changeme2")
    public void testLegacyConfig() throws IOException {
      MockitoAnnotations.openMocks(this);
      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider)).thenReturn("DataCite");
      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Shoulder)).thenReturn("FK2");

      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol)).thenReturn("doi");
      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority)).thenReturn("10.5075");


      
      String protocol = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol);
      String authority = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority);
      String shoulder = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Shoulder);
      String provider = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider);

      if (protocol != null && authority != null && shoulder != null && provider != null) {
          // This line is different than in PidProviderFactoryBean because here we've
          // already added the unmanaged providers, so we can't look for null
          if (!PidUtil.getPidProvider(protocol, authority, shoulder).canManagePID()) {
              PidProvider legacy = null;
              // Try to add a legacy provider
              String identifierGenerationStyle = settingsServiceBean
                      .getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "random");
              String dataFilePidFormat = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat,
                      "DEPENDENT");
              switch (provider) {
              case "EZID":
                  /*
                   * String baseUrl = JvmSettings.PID_EZID_BASE_URL.lookup(String.class); String
                   * username = JvmSettings.PID_EZID_USERNAME.lookup(String.class); String
                   * password = JvmSettings.PID_EZID_PASSWORD.lookup(String.class);
                   * legacy = new EZIdDOIProvider("legacy", "legacy", authority,
                   * shoulder, identifierGenerationStyle, dataFilePidFormat, "", "", baseUrl,
                   * username, password);
                   */
                  break;
              case "DataCite":
                  String mdsUrl = JvmSettings.LEGACY_DATACITE_MDS_API_URL.lookup(String.class);
                  String restUrl = JvmSettings.LEGACY_DATACITE_REST_API_URL.lookup(String.class);
                  String dcUsername = JvmSettings.LEGACY_DATACITE_USERNAME.lookup(String.class);
                  String dcPassword = JvmSettings.LEGACY_DATACITE_PASSWORD.lookup(String.class);
                  if (mdsUrl != null && restUrl != null && dcUsername != null && dcPassword != null) {
                      legacy = new DataCiteDOIProvider("legacy", "legacy", authority, shoulder,
                              identifierGenerationStyle, dataFilePidFormat, "", "", mdsUrl, restUrl, dcUsername,
                              dcPassword);
                  }
                  break;
              case "FAKE":
                  System.out.println("Legacy FAKE found");
                  legacy = new FakeDOIProvider("legacy", "legacy", authority, shoulder,
                              identifierGenerationStyle, dataFilePidFormat, "", "");
                  break;
              }
              if (legacy != null) {
                  // Not testing parts that require this bean
                  legacy.setPidProviderServiceBean(null);
                  PidUtil.addToProviderList(legacy);
              }
          } else {
              System.out.println("Legacy PID provider settings found - ignored since a provider for the same protocol, authority, shoulder has been registered");
          }

      }
      
        String pid1String = "doi:10.5075/FK2ABCDEF";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);    
        assertEquals(pid1String, pid2.asString());
        assertEquals("legacy", pid2.getProviderId());
    }

    //Tests support for legacy Perma provider - see #10516
    @Test
    @JvmSetting(key = JvmSettings.LEGACY_PERMALINK_BASEURL, value = "http://localhost:8080/")
    public void testLegacyPermaConfig() throws IOException {
      MockitoAnnotations.openMocks(this);
      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Shoulder)).thenReturn("FK2");
      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol)).thenReturn(PermaLinkPidProvider.PERMA_PROTOCOL);
      Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority)).thenReturn("PermaTest");
      
      String protocol = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol);
      String authority = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority);
      String shoulder = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Shoulder);

      //Code mirrors the relevant part of PidProviderFactoryBean
      if (protocol != null && authority != null && shoulder != null) {
          // This line is different than in PidProviderFactoryBean because here we've
          // already added the unmanaged providers, so we can't look for null
          if (!PidUtil.getPidProvider(protocol, authority, shoulder).canManagePID()) {
              PidProvider legacy = null;
              // Try to add a legacy provider
              String identifierGenerationStyle = settingsServiceBean
                      .getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "random");
              String dataFilePidFormat = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat,
                      "DEPENDENT");
              String baseUrl = JvmSettings.LEGACY_PERMALINK_BASEURL.lookupOptional().orElse(SystemConfig.getDataverseSiteUrlStatic());
              legacy = new PermaLinkPidProvider("legacy", "legacy", authority, shoulder,
                      identifierGenerationStyle, dataFilePidFormat, "", "", baseUrl,
                      PermaLinkPidProvider.SEPARATOR);
              if (legacy != null) {
                  // Not testing parts that require this bean
                  legacy.setPidProviderServiceBean(null);
                  PidUtil.addToProviderList(legacy);
              }
          } else {
              System.out.println("Legacy PID provider settings found - ignored since a provider for the same protocol, authority, shoulder has been registered");
          }

      }
        //Is a perma PID with the default "" separator recognized?
        String pid1String = "perma:PermaTestFK2ABCDEF";
        GlobalId pid2 = PidUtil.parseAsGlobalID(pid1String);
        assertEquals(pid1String, pid2.asString());
        assertEquals("legacy", pid2.getProviderId());
    }
}
