package edu.harvard.iq.dataverse.export.ddi;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO.DataFileDTO;
import edu.harvard.iq.dataverse.export.DeserializartionHelper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DdiDatasetExportServiceTest {

    @InjectMocks
    private DdiDatasetExportService ddiDatasetExportService;
    @Mock
    private DdiDataAccessWriter ddiDataAccessWriter;
    @Mock
    private DdiFileWriter ddiFileWriter;
    @Mock
    private DdiVariableWriter ddiVariableWriter;


    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should handle multiple values of collection mode using localized values")
    void datasetJson2ddi__multipleCollectionMode() throws Exception {

        // given
        String modeA = "mode A";
        String modeB = "mode B";

        JsonObject json = createObjectBuilder()
                .add("identifier", "PCA2E3")
                .add("protocol", "doi")
                .add("authority", "10.5072/FK2")
                .add("datasetVersion", createObjectBuilder()
                    .add("versionState", "RELEASED")
                    .add("releaseTime", "2021-01-13T01:01:01Z")
                    .add("versionNumber", 1)
                    .add("citation", "citation")
                    .add("metadataBlocks", createObjectBuilder()
                            .add("socialscience", createObjectBuilder()
                                .add("fields", createArrayBuilder()
                                    .add(createObjectBuilder()
                                        .add("typeName", "collectionMode")
                                        .add("multiple", true)
                                        .add("typeClass", "controlledVocabulary")
                                        .add("value", createArrayBuilder()
                                            .add(modeA)
                                            .add(modeB))))))
                    .add("files", createArrayBuilder())).build();

        DatasetDTO datasetDTO = new Gson().fromJson(json.toString(), DatasetDTO.class);
        datasetDTO.setEmbargoActive(false);
        Map<String, String> collectionModeIndex = new HashMap<>();
        collectionModeIndex.put(modeA, "Collection mode A");
        collectionModeIndex.put(modeB, "Collection mode B");

        Map<String, Map<String, String>> index = new HashMap<>();
        index.put("collectionMode", collectionModeIndex);

        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ddiDatasetExportService.datasetJson2ddi(datasetDTO, outputStream, index);

        // then
        assertThat(outputStream.toString()).contains(
                "<collMode>Collection mode A</collMode>",
                "<collMode>Collection mode B</collMode>"
        );
    }

    @Test
    @DisplayName("Create DDI from JSON when there are no files")
    void datasetJson2ddi__noFiles() throws Exception {

        // given
        String datasetAsDdi = XmlPrinter.prettyPrintXml(readFile("xml/export/ddi/dataset-finch1.xml"));
        DatasetDTO datasetDTO = readDtoFromFile("json/export/ddi/dataset-finch1.json");

        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ddiDatasetExportService.datasetJson2ddi(datasetDTO, outputStream, Collections.emptyMap());

        // then
        assertThat(XmlPrinter.prettyPrintXml(outputStream.toString())).isEqualTo(datasetAsDdi);
        verify(ddiDataAccessWriter).writeDataAccess(any(), same(datasetDTO));
    }

    @Test
    void datasetJson2ddi__all_metadata_fields() throws IOException, URISyntaxException, XMLStreamException {

        // given
        String datasetAsDdi = XmlPrinter.prettyPrintXml(readFile("xml/export/ddi/dataset-all-ddi-metadata-fields.xml"));
        DatasetDTO datasetDTO = readDtoFromFile("xml/export/ddi/dataset-all-ddi-metadata-fields.json");

        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ddiDatasetExportService.datasetJson2ddi(datasetDTO, outputStream, Collections.emptyMap());

        // then
        assertThat(XmlPrinter.prettyPrintXml(outputStream.toString())).isEqualTo(datasetAsDdi);
        verify(ddiDataAccessWriter).writeDataAccess(any(), same(datasetDTO));

    }

    @Test
    void datasetJson2ddi__withFiles() throws IOException, URISyntaxException, XMLStreamException {

        // given
        DatasetDTO datasetDTO = readDtoFromFile("json/export/ddi/dataset-finch1.json");

        FileMetadataDTO file1 = new FileMetadataDTO();
        DataFileDTO tabularDataFileDto = new DataFileDTO();
        tabularDataFileDto.setFilename("file1.tab");
        file1.setDataFile(tabularDataFileDto);

        FileMetadataDTO file2 = new FileMetadataDTO();
        DataFileDTO nonTabularDataFileDto = new DataFileDTO();
        nonTabularDataFileDto.setFilename("file2.txt");
        file2.setDataFile(nonTabularDataFileDto);

        datasetDTO.getDatasetVersion().setFiles(Lists.newArrayList(file1, file2));

        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ddiDatasetExportService.datasetJson2ddi(datasetDTO, outputStream, Collections.emptyMap());

        // then
        verify(ddiFileWriter).writeOtherMatFromFileDto(any(), same(file2));
        verify(ddiFileWriter).writeOtherMatFromFileDto(any(), same(file1));
        verifyNoMoreInteractions(ddiFileWriter);

    }

    @Test
    void datasetJson2ddi_full__withFiles() throws IOException, URISyntaxException, XMLStreamException {

        // given
        DatasetDTO datasetDTO = readDtoFromFile("json/export/ddi/dataset-finch1.json");
        DatasetVersion version = new DatasetVersion();

        FileMetadata nonTabularFile = createFileMetadata();
        DataVariable variable1 = new DataVariable();
        DataVariable variable2 = new DataVariable();
        FileMetadata tabularFile1 = createFileMetadataForTabularFile(variable1, variable2);
        DataVariable variable3 = new DataVariable();
        DataVariable variable4 = new DataVariable();
        FileMetadata tabularFile2 = createFileMetadataForTabularFile(variable3, variable4);

        version.setFileMetadatas(Lists.newArrayList(nonTabularFile, tabularFile1, tabularFile2));

        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ddiDatasetExportService.datasetJson2ddi(datasetDTO, version, outputStream, Collections.emptyMap());

        // then
        verify(ddiFileWriter).writeOtherMatFromFileMetadata(any(), same(nonTabularFile));
        verify(ddiFileWriter).writeFileDscr(any(), same(tabularFile1));
        verify(ddiFileWriter).writeFileDscr(any(), same(tabularFile2));

        verify(ddiVariableWriter).createVarDDI(any(), same(variable1), same(tabularFile1));
        verify(ddiVariableWriter).createVarDDI(any(), same(variable2), same(tabularFile1));
        verify(ddiVariableWriter).createVarDDI(any(), same(variable3), same(tabularFile2));
        verify(ddiVariableWriter).createVarDDI(any(), same(variable4), same(tabularFile2));
    }

    // -------------------- PRIVATE --------------------

    private DatasetDTO readDtoFromFile(String path) throws IOException {
        String datasetDtoJson = UnitTestUtils.readFileToString(path);
        Gson gson = new Gson();
        DatasetDTO dto = gson.fromJson(datasetDtoJson, DatasetDTO.class);
        dto.setEmbargoActive(false);
        DeserializartionHelper.repairNestedDatasetFields(dto);
        return dto;
    }

    private String readFile(String path) throws IOException, URISyntaxException {
        return UnitTestUtils.readFileToString(path);
    }

    private FileMetadata createFileMetadata() {
        FileMetadata fileMetadata = new FileMetadata();
        DataFile dataFile = new DataFile();
        fileMetadata.setDataFile(dataFile);
        return fileMetadata;
    }

    private FileMetadata createFileMetadataForTabularFile(DataVariable...dataVariables) {
        FileMetadata fileMetadata = createFileMetadata();
        DataFile dataFile = fileMetadata.getDataFile();
        DataTable dataTable = new DataTable();
        dataTable.setDataVariables(Lists.newArrayList(dataVariables));
        dataFile.setDataTable(dataTable);

        return fileMetadata;
    }
}
