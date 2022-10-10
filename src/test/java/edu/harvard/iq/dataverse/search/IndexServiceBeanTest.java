package edu.harvard.iq.dataverse.search;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

public class IndexServiceBeanTest {
    private static final Logger logger = Logger.getLogger(IndexServiceBeanTest.class.getCanonicalName());

    private IndexServiceBean indexService;
    private Dataverse dataverse;

    @Before
    public void setUp() {
        dataverse = MocksFactory.makeDataverse();
        dataverse.setDataverseType(DataverseType.UNCATEGORIZED);
        indexService = new IndexServiceBean();
        indexService.systemConfig = new SystemConfig();
        indexService.settingsService = Mockito.mock(SettingsServiceBean.class);
        indexService.dataverseService = Mockito.mock(DataverseServiceBean.class);
        indexService.datasetFieldService = Mockito.mock(DatasetFieldServiceBean.class);
        BrandingUtil.injectServices(indexService.dataverseService, indexService.settingsService);

        Mockito.when(indexService.dataverseService.findRootDataverse()).thenReturn(dataverse);
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

    private IndexableDataset createIndexableDataset() {
        final Dataset dataset = MocksFactory.makeDataset();
        String fakeId = "doi:10.666/FAKE/fake";
        dataset.setGlobalId(new GlobalId(fakeId));
        final DatasetVersion datasetVersion = dataset.getCreateVersion(null);
        DatasetField field = createCVVField("language", "English", false);
        datasetVersion.getDatasetFields().add(field);
        final IndexableDataset indexableDataset = new IndexableDataset(datasetVersion);
        indexableDataset.getDatasetVersion().getDataset().setOwner(dataverse);
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
