package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
@JvmSetting(key = JvmSettings.SITE_URL, value = "https://example.com")

public class DataCiteProviderTest {

    static DataverseServiceBean dataverseSvc;
    static SettingsServiceBean settingsSvc;
    static PidProviderFactoryBean pidService;
    static final String DEFAULT_NAME = "LibraScholar";

    @BeforeAll
    public static void setupMocks() {
        dataverseSvc = Mockito.mock(DataverseServiceBean.class);
        settingsSvc = Mockito.mock(SettingsServiceBean.class);
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);

        // initial values (needed here for other tests where this method is reused!)
        Mockito.when(settingsSvc.getValueForKey(SettingsServiceBean.Key.InstallationName)).thenReturn(DEFAULT_NAME);
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn(DEFAULT_NAME);

        pidService = Mockito.mock(PidProviderFactoryBean.class);
        Mockito.when(pidService.isGlobalIdLocallyUnique(any(GlobalId.class))).thenReturn(true);
        Mockito.when(pidService.getProducer()).thenReturn("RootDataverse");

    }

    /**
     * Useful for testing but requires DataCite credentials, etc.
     * 
     * To run the test:
     * export DataCiteUsername=test2
     * export DataCitePassword=changeme2
     * export DataCiteAuthority=10.5072
     * export DataCiteShoulder=FK2
     * 
     * then run mvn test -Dtest=DataCiteProviderTest
     * 
     * For each run of the test, one test DOI will be created and will remain in the registered state, as visible on Fabrica at doi.test.datacite.org
     * (two DOIs are created, but one is deleted after being created in the draft state and never made findable.)
     */
    @Test
    @Disabled
    public void testDoiLifecycle() throws IOException {
        String username = System.getenv("DataCiteUsername");
        String password = System.getenv("DataCitePassword");
        String authority = System.getenv("DataCiteAuthority");
        String shoulder = System.getenv("DataCiteShoulder");
        DataCiteDOIProvider provider = new DataCiteDOIProvider("test", "test", authority, shoulder, "randomString",
                SystemConfig.DataFilePIDFormat.DEPENDENT.toString(), "", "", "https://mds.test.datacite.org",
                "https://api.test.datacite.org", username, password);

        provider.setPidProviderServiceBean(pidService);

        PidUtil.addToProviderList(provider);

        Dataset d = new Dataset();
        DatasetVersion dv = new DatasetVersion();
        DatasetFieldType primitiveDSFType = new DatasetFieldType(DatasetFieldConstant.title,
                DatasetFieldType.FieldType.TEXT, false);
        DatasetField testDatasetField = new DatasetField();

        dv.setVersionState(VersionState.DRAFT);

        testDatasetField.setDatasetVersion(dv);
        testDatasetField.setDatasetFieldType(primitiveDSFType);
        testDatasetField.setSingleValue("First Title");
        List<DatasetField> fields = new ArrayList<>();
        fields.add(testDatasetField);
        dv.setDatasetFields(fields);
        ArrayList<DatasetVersion> dsvs = new ArrayList<>();
        dsvs.add(0, dv);
        d.setVersions(dsvs);

        assertEquals(d.getCurrentName(), "First Title");

        provider.generatePid(d);
        assertEquals(d.getProtocol(), "doi");
        assertEquals(d.getAuthority(), authority);
        assertTrue(d.getIdentifier().startsWith(shoulder));
        d.getGlobalId();

        try {
            provider.createIdentifier(d);
            d.setIdentifierRegistered(true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(DataCiteDOIProvider.DRAFT, provider.getPidStatus(d));
        Map<String, String> mdMap = provider.getIdentifierMetadata(d);
        assertEquals("First Title", mdMap.get("datacite.title"));

        testDatasetField.setSingleValue("Second Title");

        //Modify called for a draft dataset shouldn't update DataCite (given current code)
        try {
            provider.modifyIdentifierTargetURL(d);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Verify the title hasn't changed
        mdMap = provider.getIdentifierMetadata(d);
        assertEquals("First Title", mdMap.get("datacite.title"));
      //Check our local status
        assertEquals(DataCiteDOIProvider.DRAFT, provider.getPidStatus(d));
        //Now delete the identifier
        provider.deleteIdentifier(d);
        //Causes a 404 and a caught exception that prints a stack trace.
        mdMap = provider.getIdentifierMetadata(d);
        // And verify the record is gone (no title, should be no entries at all)
        assertEquals(null, mdMap.get("datacite.title"));
        
        //Now recreate and publicize in one step
        assertTrue(provider.publicizeIdentifier(d));
        d.getLatestVersion().setVersionState(VersionState.RELEASED);

        //Verify the title hasn't changed
        mdMap = provider.getIdentifierMetadata(d);
        assertEquals("Second Title", mdMap.get("datacite.title"));
      //Check our local status
        assertEquals(DataCiteDOIProvider.FINDABLE, provider.getPidStatus(d));
        
        //Verify that modify does update a published/findable record
        testDatasetField.setSingleValue("Third Title");

        try {
            provider.modifyIdentifierTargetURL(d);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mdMap = provider.getIdentifierMetadata(d);
        assertEquals("Third Title", mdMap.get("datacite.title"));
        
        //Now delete the identifier . Once it's been findable, this should just flip the record to registered
        //Not sure that can be easily verified in the test, but it will be visible in Fabrica
        provider.deleteIdentifier(d);
        d.getLatestVersion().setVersionState(VersionState.DEACCESSIONED);
        
        mdMap = provider.getIdentifierMetadata(d);
        assertEquals("This item has been removed from publication", mdMap.get("datacite.title"));
        
        //Check our local status - just uses the version state
        assertEquals(DataCiteDOIProvider.REGISTERED, provider.getPidStatus(d));

        // provider.registerWhenPublished()
    }

}
