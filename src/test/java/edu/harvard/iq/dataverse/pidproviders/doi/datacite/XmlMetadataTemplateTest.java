package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.DoiMetadata;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.xml.XmlValidator;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.SAXException;

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
        DatasetFieldType dft = new DatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false);
        dft.setDisplayFormat("#VALUE");
        DatasetFieldType dft2 = new DatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false);
        dft2.setDisplayFormat("#VALUE");
        DatasetAuthor alice = new DatasetAuthor();
        DatasetField df1 = new DatasetField();
        df1.setDatasetFieldType(dft);
        df1.setSingleValue("Alice");
        alice.setName(df1);
        DatasetField df2 = new DatasetField();
        df2.setDatasetFieldType(dft2);
        df2.setSingleValue("Harvard University");
        alice.setAffiliation(df2);
        DatasetAuthor bob = new DatasetAuthor();
        DatasetField df3 = new DatasetField();
        df3.setDatasetFieldType(dft);
        df3.setSingleValue("Bob");
        bob.setName(df3);
        DatasetField df4 = new DatasetField();
        df4.setDatasetFieldType(dft2);
        df4.setSingleValue("QDR");
        bob.setAffiliation(df4);
        List<DatasetAuthor> authors = new ArrayList<>();
        authors.add(alice);
        authors.add(bob);
        doiMetadata.setAuthors(authors);
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
        DatasetType dType = new DatasetType();
        dType.setName(DatasetType.DATASET_TYPE_DATASET);
        d.setDatasetType(dType);

        String xml = template.generateXML(d);
        System.out.println("Output is " + xml);
        try {
            StreamSource source = new StreamSource(new StringReader(xml));
            source.setSystemId("DataCite XML for test dataset");
            assertTrue(XmlValidator.validateXmlSchema(source, new URL("https://schema.datacite.org/meta/kernel-4/metadata.xsd")));
        } catch (SAXException e) {
            System.out.println("Invalid schema: " + e.getMessage());
        }
        
    }

}
