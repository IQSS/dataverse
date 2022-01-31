package edu.harvard.iq.dataverse.export;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;
import edu.harvard.iq.dataverse.citation.CitationDataExtractor;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.citation.StandardCitationFormatsConverter;
import edu.harvard.iq.dataverse.export.ddi.DdiDatasetExportService;
import edu.harvard.iq.dataverse.persistence.MockMetadataFactory;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DDIExporterTest {

    private DDIExporter ddiExporter;

    @Mock private DdiDatasetExportService ddiDatasetExportService;
    @Mock private SettingsServiceBean settingsService;
    @Mock private VocabularyValuesIndexer vocabularyValuesIndexer;

    @Captor
    private ArgumentCaptor<DatasetDTO> datasetDtoCaptor;

    @BeforeEach
    void setUp() {
        ddiExporter = new DDIExporter(ddiDatasetExportService, settingsService, vocabularyValuesIndexer,
                new CitationFactory(new CitationDataExtractor(), new StandardCitationFormatsConverter()));
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should pass email type field to service responsible for export")
    void exportDataset_without_excluding_emails() throws IOException, ExportException, XMLStreamException {
        //given
        Dataset dataset = createDataset();
        DatasetVersion datasetVersion = dataset.getLatestVersion();

        MetadataBlock citationBlock = MockMetadataFactory.makeCitationMetadataBlock();

        DatasetFieldType titleType = MockMetadataFactory.makeTitleFieldType(citationBlock);
        DatasetField titleField = DatasetField.createNewEmptyDatasetField(titleType, datasetVersion);
        MockMetadataFactory.fillTitle(titleField, "Export test");

        DatasetFieldType emailType = MocksFactory.makeDatasetFieldType("email", FieldType.EMAIL, false, citationBlock);
        DatasetField emailField = DatasetField.createNewEmptyDatasetField(emailType, datasetVersion);
        emailField.setValue("example@domain.com");

        datasetVersion.setDatasetFields(Lists.newArrayList(titleField, emailField));

        when(settingsService.isTrueForKey(Key.ExcludeEmailFromExport)).thenReturn(false);


        //when
        ddiExporter.exportDataset(datasetVersion);

        //then
        verify(ddiDatasetExportService).datasetJson2ddi(datasetDtoCaptor.capture(), same(datasetVersion), any(), any());
        DatasetDTO datasetDTO = datasetDtoCaptor.getValue();

        assertThat(extractDatasetField(datasetDTO, "email")).isPresent();
        DatasetFieldDTO capturedEmailField = extractDatasetField(datasetDTO, "email").get();
        assertThat(capturedEmailField.getSinglePrimitive()).isEqualTo("example@domain.com");

    }

    @Test
    @DisplayName("Should not pass email type field to service responsible for export")
    void exportDataset_with_excluding_emails() throws IOException, ExportException, XMLStreamException {
        //given
        Dataset dataset = createDataset();
        DatasetVersion datasetVersion = dataset.getLatestVersion();

        MetadataBlock citationBlock = MockMetadataFactory.makeCitationMetadataBlock();

        DatasetFieldType titleType = MockMetadataFactory.makeTitleFieldType(citationBlock);
        DatasetField titleField = DatasetField.createNewEmptyDatasetField(titleType, datasetVersion);
        MockMetadataFactory.fillTitle(titleField, "Export test");

        DatasetFieldType emailType = MocksFactory.makeDatasetFieldType("email", FieldType.EMAIL, false, citationBlock);
        DatasetField emailField = DatasetField.createNewEmptyDatasetField(emailType, datasetVersion);
        emailField.setValue("example@domain.com");

        datasetVersion.setDatasetFields(Lists.newArrayList(titleField, emailField));

        when(settingsService.isTrueForKey(Key.ExcludeEmailFromExport)).thenReturn(true);

        //when
        ddiExporter.exportDataset(datasetVersion);

        //then
        verify(ddiDatasetExportService).datasetJson2ddi(datasetDtoCaptor.capture(), same(datasetVersion), any(), any());
        DatasetDTO datasetDTO = datasetDtoCaptor.getValue();

        assertThat(extractDatasetField(datasetDTO, "email")).isNotPresent();

    }

    // -------------------- PRIVATE --------------------

    private Optional<DatasetFieldDTO> extractDatasetField(DatasetDTO dataset, String fieldTypeName) {
        return dataset.getDatasetVersion().getMetadataBlocks().values().stream()
                .flatMap(block -> block.getFields().stream())
                .filter(field -> field.getTypeName().equals(fieldTypeName))
                .findFirst();
    }

    private Dataset createDataset() {
        Dataset dataset = MocksFactory.makeDataset();
        dataset.getFiles().clear();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.1012");
        dataset.setIdentifier("abc");
        dataset.setPublicationDate(Timestamp.from(Instant.now()));
        DatasetVersion datasetVersion = dataset.getLatestVersion();
        datasetVersion.getFileMetadatas().clear();
        return dataset;
    }

}
