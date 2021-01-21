package edu.harvard.iq.dataverse.export.dublincore;

import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author skraffmi
 */
class DublinCoreExportUtilTest {

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should convert json to DublinCore")
    void datasetJson2dublincore() throws Exception {

        // given
        String datasetVersionAsJson = readFileFromResources("json/export/ddi/dataset-finch1.json");
        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json = jsonReader.readObject();

        // when
        OutputStream output = new ByteArrayOutputStream();
        DublinCoreExportUtil.datasetJson2dublincore(json, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = XmlPrinter.prettyPrintXml(output.toString());

        // then
        String datasetAsDdi = XmlPrinter.prettyPrintXml(readFileFromResources("xml/export/ddi/dataset-finchDC.xml"));
        assertThat(result).isEqualTo(datasetAsDdi);
    }

    @Test
    @DisplayName("Should handle cases when there is a single value instead of expected array")
    void datasetJson2dublincore__singleValueInsteadOfArray() throws Exception {

        // given
        JsonObject json = createObjectBuilder()
                .add("identifier", "PCA2E3")
                .add("protocol", "doi")
                .add("authority", "10.5072/FK2")
                .add("datasetVersion", createObjectBuilder()
                    .add("metadataBlocks", createObjectBuilder()
                        .add("citation", createObjectBuilder()
                            .add("fields", createArrayBuilder()
                                .add(createObjectBuilder()
                                    .add("typeName", "language")
                                    .add("multiple", false)
                                    .add("typeClass", "controlledVocabulary")
                                    .add("value", "Polish")))))).build();

        // when
        OutputStream output = new ByteArrayOutputStream();
        DublinCoreExportUtil.datasetJson2dublincore(json, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = output.toString();

        // then
        assertThat(result).contains("<dcterms:language>Polish</dcterms:language>");
    }

    // -------------------- PRIVATE --------------------

    private String readFileFromResources(String path) throws URISyntaxException, IOException {
        URI uri = getClass().getClassLoader()
                .getResource(path)
                .toURI();
        return String.join("\n", Files.readAllLines(Paths.get(uri)));
    }
}
