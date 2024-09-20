package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LocalJvmSettings
@ExtendWith(MockitoExtension.class)
public class IndexServiceBeanTest {
    private static final Logger logger = Logger.getLogger(IndexServiceBeanTest.class.getCanonicalName());

    private IndexServiceBean indexService;
    private Dataverse dataverse;

    @Mock
    private SettingsServiceBean settingsService;
    @InjectMocks
    private SystemConfig systemConfig = new SystemConfig();
    
    @BeforeEach
    public void setUp() {
        dataverse = MocksFactory.makeDataverse();
        dataverse.setDataverseType(DataverseType.UNCATEGORIZED);
        indexService = new IndexServiceBean();
        indexService.systemConfig = systemConfig;
        indexService.settingsService = Mockito.mock(SettingsServiceBean.class);
        indexService.dataverseService = Mockito.mock(DataverseServiceBean.class);
        indexService.datasetFieldService = Mockito.mock(DatasetFieldServiceBean.class);
        indexService.datasetVersionService = Mockito.mock(DatasetVersionServiceBean.class);
        BrandingUtil.injectServices(indexService.dataverseService, indexService.settingsService);

        Mockito.when(indexService.dataverseService.findRootDataverse()).thenReturn(dataverse);
    }
    
    @Test
    public void testInitWithDefaults() {
        // given
        String url = "http://localhost:8983/solr/collection1";
        
        // when
        indexService.init();
        
        // then
        HttpSolrClient client = (HttpSolrClient) indexService.solrServer;
        assertEquals(url, client.getBaseURL());
    }
    
    
    @Test
    @JvmSetting(key = JvmSettings.SOLR_HOST, value = "foobar")
    @JvmSetting(key = JvmSettings.SOLR_PORT, value = "1234")
    @JvmSetting(key = JvmSettings.SOLR_CORE, value = "test")
    void testInitWithConfig() {
        // given
        String url = "http://foobar:1234/solr/test";
        
        // when
        indexService.init();
        
        // then
        HttpSolrClient client = (HttpSolrClient) indexService.solrServer;
        assertEquals(url, client.getBaseURL());
    }

    @Test
    public void TestIndexing() throws SolrServerException, IOException {
        final IndexableDataset indexableDataset = createIndexableDataset();
        final SolrInputDocuments docs = indexService.toSolrDocs(indexableDataset, null);
        Set<String> indexedFields = docs.getDocuments().stream().flatMap(x -> x.getFieldNames().stream()).collect(Collectors.toSet());

        logger.info(docs.getMessage());
        logger.info(String.join(", ", indexedFields));

        assertTrue(!docs.getDocuments().isEmpty());
        assertTrue(indexedFields.contains("language"));
    }

    @Test
    public void testValidateBoundingBox() throws SolrServerException, IOException {
        final IndexableDataset indexableDataset = createIndexableDataset();
        final DatasetVersion datasetVersion = indexableDataset.getDatasetVersion();
        DatasetField dsf = new DatasetField();
        DatasetFieldType dsft = new DatasetFieldType(DatasetFieldConstant.geographicBoundingBox, DatasetFieldType.FieldType.TEXT, true);
        dsf.setDatasetFieldType(dsft);

        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
        val.setParentDatasetField(dsf);
        val.setChildDatasetFields(Arrays.asList(
                constructBoundingBoxValue(DatasetFieldConstant.westLongitude, "34.9"), // bad value. must be less than east
                constructBoundingBoxValue(DatasetFieldConstant.eastLongitude, "34.8"),
                constructBoundingBoxValue(DatasetFieldConstant.northLatitude, "34.2"),
                constructBoundingBoxValue(DatasetFieldConstant.southLatitude, "34.1")
        ));
        vals.add(val);
        dsf.setDatasetFieldCompoundValues(vals);
        datasetVersion.getDatasetFields().add(dsf);

        final SolrInputDocuments docs = indexService.toSolrDocs(indexableDataset, null);
        Optional<SolrInputDocument> doc = docs.getDocuments().stream().findFirst();
        assertTrue(doc.isPresent());
        assertTrue(!doc.get().containsKey("geolocation"));
        assertTrue(!doc.get().containsKey("boundingBox"));
    }
    private DatasetField constructBoundingBoxValue(String datasetFieldTypeName, String value) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType(new DatasetFieldType(datasetFieldTypeName, DatasetFieldType.FieldType.TEXT, false));
        retVal.setDatasetFieldValues(Collections.singletonList(new DatasetFieldValue(retVal, value)));
        return retVal;
    }

    private IndexableDataset createIndexableDataset() {
        final Dataset dataset = MocksFactory.makeDataset();
        dataset.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL,"10.666", "FAKE/fake", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null));
        final DatasetVersion datasetVersion = dataset.getCreateVersion(null);
        DatasetField field = createCVVField("language", "English", false);
        datasetVersion.getDatasetFields().add(field);
        final IndexableDataset indexableDataset = new IndexableDataset(datasetVersion);
        indexableDataset.getDatasetVersion().getDataset().setOwner(dataverse);
        DatasetType datasetType = new DatasetType();
        datasetType.setName(DatasetType.DEFAULT_DATASET_TYPE);
        indexableDataset.getDatasetVersion().getDataset().setDatasetType(datasetType);
        return indexableDataset;
    }

    public static DatasetField createCVVField(String name, String strValue, boolean isAllowMultiples) {
        DatasetFieldType datasetFieldType = new DatasetFieldType(name, DatasetFieldType.FieldType.TEXT, isAllowMultiples);
        ControlledVocabularyValue value = new ControlledVocabularyValue(MocksFactory.nextId(), strValue, datasetFieldType);
        datasetFieldType.setControlledVocabularyValues(Arrays.asList(value));
        datasetFieldType.setId(MocksFactory.nextId());
        datasetFieldType.setMetadataBlock(new MetadataBlock());
        DatasetField field = new DatasetField();
        field.setId(MocksFactory.nextId());
        field.setSingleControlledVocabularyValue(value);
        field.setDatasetFieldType(datasetFieldType);
        return field;
    }
}
