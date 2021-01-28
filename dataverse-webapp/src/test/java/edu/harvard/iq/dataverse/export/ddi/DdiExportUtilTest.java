package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class DdiExportUtilTest {

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
                                            .add(modeB))))))).build();

        DatasetVersion version = new DatasetVersion();
        Dataset dataset = Mockito.mock(Dataset.class);
        Mockito.when(dataset.hasActiveEmbargo()).thenReturn(true);
        version.setDataset(dataset);

        Map<String, String> collectionModeIndex = new HashMap<>();
        collectionModeIndex.put(modeA, "Collection mode A");
        collectionModeIndex.put(modeB, "Collection mode B");

        Map<String, Map<String, String>> index = new HashMap<>();
        index.put("collectionMode", collectionModeIndex);

        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DdiExportUtil.datasetJson2ddi(json, version, outputStream, "url", index);

        // then
        assertThat(outputStream.toString()).contains(
                "<collMode>Collection mode A</collMode>",
                "<collMode>Collection mode B</collMode>"
        );
    }

    @Test
    @DisplayName("Create DDI from JSON when there are no files")
    void datasetDtoAsJson2ddi__noFiles() throws Exception {

        // gievn
        String datasetAsDdi = XmlPrinter.prettyPrintXml(readFile("xml/export/ddi/dataset-finch1.xml"));
        String datasetVersionAsJson = readFile("json/export/ddi/dataset-finch1.json");

        // when
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson, "https://localhost:8080",
                Collections.emptyMap());

        // then
        assertThat(result).isEqualTo(datasetAsDdi);
    }

    /*
    * TODO:
    * before we can reenable it again, we'll need to figure out what to do
    * with the access URLs, that are now included in the fileDscr and otherMat
    * sections. So a) we'll need to add something like URI=http://localhost/api/access/datafile/12 to
    * dataset-spruce1.xml, above; and modify the DDI export util so that
    * it can be instructed to use "localhost" for the API urls (otherwise
    * it will use the real hostname). -- L.A. 4.5
    */
    @Test
    @Disabled
    @DisplayName("Create DDI from JSON when there are files in dataset version")
    void datasetDtoAsJson2ddi__withFiles() throws Exception {

        // given

        // Note that `cat dataset-spruce1.json | jq .datasetVersion.files[0].datafile.description` yields
        // an empty string but datasets created in the GUI sometimes don't have a description field at all.
        String datasetAsDdi = XmlPrinter.prettyPrintXml(readFile("xml/export/ddi/dataset-spruce1.xml"));
        String datasetVersionAsJson = readFile("json/export/ddi/dataset-spruce1.json");

        // when
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson, "https://localhost:8080",
                Collections.emptyMap());

        // then
        assertThat(result).isEqualTo(datasetAsDdi);
    }

    // -------------------- PRIVATE --------------------

    private String readFile(String path) throws IOException, URISyntaxException {
        return new String(
                Files.readAllBytes(
                        Paths.get(getClass().getClassLoader().getResource(path).toURI())));
    }
}
