package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.citation.Citation;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.export.ddi.DdiDatasetExportService;
import edu.harvard.iq.dataverse.export.ddi.DdiToHtmlTransformer;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.stream.XMLStreamException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DdiHtmlExporterTest {

    @Mock private CitationFactory citationFactory;
    @Mock private SettingsServiceBean settingsService;
    @Mock private DdiDatasetExportService ddiDatasetExportService;
    @Mock private VocabularyValuesIndexer vocabularyValuesIndexer;
    @Mock private DdiToHtmlTransformer ddiToHtmlTransformer;

    @InjectMocks
    private DdiHtmlExporter exporter;

    @Mock private Citation citation;

    // -------------------- TESTS --------------------

    @Test
    void exportDataset() throws ExportException, XMLStreamException {
        // given
        DatasetVersion version = prepareDatasetVersion();

        // when
        exporter.exportDataset(version);

        // then
        verify(ddiDatasetExportService, times(1))
                .datasetJson2ddi(any(), eq(version), any(), anyMap());
        verify(ddiToHtmlTransformer, times(1))
                .transform(any(), any());
    }

    // -------------------- PRIVATE --------------------

    private DatasetVersion prepareDatasetVersion() {
        Dataset dataset = MocksFactory.makeDataset();
        dataset.setGlobalId(new GlobalId("doi", "123", "4567"));
        DatasetVersion version = dataset.getLatestVersion();
        version.getFileMetadatas().forEach(m -> m.setDatasetVersion(version));
        version.getDatasetFields().clear();
        when(citationFactory.create(version)).thenReturn(citation);
        when(citation.toString(anyBoolean())).thenReturn("");
        return version;
    }
}