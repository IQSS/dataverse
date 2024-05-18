package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.DoiMetadata;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;
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

public class XmlMetadataTemplateTest {

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
     */
    @Test
    public void testDataCiteXMLCreation() throws IOException {
        DoiMetadata doiMetadata = new DoiMetadata();
        doiMetadata.setTitle("A Title");
        List<String> creators = new ArrayList<String>();
        creators.add("Alice");
        creators.add("Bob");
        doiMetadata.setCreators(creators);
        doiMetadata.setPublisher("Dataverse");
        XmlMetadataTemplate template = new XmlMetadataTemplate(doiMetadata);
        
        Dataset d = new Dataset();
        GlobalId doi = new GlobalId("doi", "10.5072", "FK2/ABCDEF", null, null, null);
        d.setGlobalId(doi);
        DatasetVersion dv = new DatasetVersion();
        TermsOfUseAndAccess toa = new TermsOfUseAndAccess();
        toa.setTermsOfUse("Some terms");
        dv.setTermsOfUseAndAccess(toa);
        dv.setDataset(d);
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

        String xml = template.generateXML(d);
        System.out.println("Output is " + xml);
        
    }

}
