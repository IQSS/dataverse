package edu.harvard.iq.dataverse.export;

import static org.junit.jupiter.api.Assertions.*;

import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.FileExportQuery;
import io.gdcc.spi.export.PageRequest;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.w3c.dom.Document;

public class CroissantExporterTest {

    static CroissantExporter exporter;
    static OutputStream outputStreamMinimal;
    static ExportDataProvider dataProviderMinimal;
    static OutputStream outputStreamMax;
    static ExportDataProvider dataProviderMax;
    static OutputStream outputStreamCars;
    static ExportDataProvider dataProviderCars;
    static OutputStream outputStreamRestricted;
    static ExportDataProvider dataProviderRestricted;
    static OutputStream outputStreamJunk;
    static ExportDataProvider dataProviderJunk;
    static OutputStream outputStreamDraft;
    static ExportDataProvider dataProviderDraft;

    @BeforeAll
    public static void setUp() {
        exporter = new CroissantExporter();

        outputStreamMinimal = new ByteArrayOutputStream();
        dataProviderMinimal =
                new ExportDataProvider() {
                    @Override
                    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetJson() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/minimal/in/datasetJson.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetORE() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/minimal/in/datasetORE.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public JsonArray getDatasetFileDetails() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/minimal/in/datasetFileDetails.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readArray();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/minimal/in/datasetSchemaDotOrg.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public String getDataCiteXml() {
                        try {
                            return Files.readString(
                                    Paths.get(
                                            "src/test/resources/croissant/minimal/in/dataCiteXml.xml"),
                                    StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            return null;
                        }
                    }
                };

        outputStreamMax = new ByteArrayOutputStream();
        dataProviderMax =
                new ExportDataProvider() {
                    @Override
                    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetJson() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/max/in/datasetJson.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetORE() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/max/in/datasetORE.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public JsonArray getDatasetFileDetails() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/max/in/datasetFileDetails.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readArray();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/max/in/datasetSchemaDotOrg.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public String getDataCiteXml() {
                        try {
                            return Files.readString(
                                    Paths.get(
                                            "src/test/resources/croissant/max/in/dataCiteXml.xml"),
                                    StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            return null;
                        }
                    }
                };

        outputStreamCars = new ByteArrayOutputStream();
        dataProviderCars =
                new ExportDataProvider() {
                    @Override
                    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetJson() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/cars/in/datasetJson.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetORE() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/cars/in/datasetORE.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public JsonArray getDatasetFileDetails() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/cars/in/datasetFileDetails.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readArray();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/cars/in/datasetSchemaDotOrg.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public String getDataCiteXml() {
                        try {
                            return Files.readString(
                                    Paths.get(
                                            "src/test/resources/croissant/cars/in/dataCiteXml.xml"),
                                    StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            return null;
                        }
                    }
                };

        outputStreamRestricted = new ByteArrayOutputStream();
        dataProviderRestricted =
                new ExportDataProvider() {
                    @Override
                    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetJson() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/restricted/in/datasetJson.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetORE() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/restricted/in/datasetORE.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public JsonArray getDatasetFileDetails() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/restricted/in/datasetFileDetails.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readArray();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/restricted/in/datasetSchemaDotOrg.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public String getDataCiteXml() {
                        try {
                            return Files.readString(
                                    Paths.get(
                                            "src/test/resources/croissant/restricted/in/dataCiteXml.xml"),
                                    StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            return null;
                        }
                    }
                };

        outputStreamJunk = new ByteArrayOutputStream();
        dataProviderJunk =
                new ExportDataProvider() {
                    @Override
                    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetJson() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/junk/in/datasetJson.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetORE() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/junk/in/datasetORE.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public JsonArray getDatasetFileDetails() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/junk/in/datasetFileDetails.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readArray();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/junk/in/datasetSchemaDotOrg.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public String getDataCiteXml() {
                        try {
                            return Files.readString(
                                    Paths.get(
                                            "src/test/resources/croissant/junk/in/dataCiteXml.xml"),
                                    StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            return null;
                        }
                    }
                };

        outputStreamDraft = new ByteArrayOutputStream();
        dataProviderDraft =
                new ExportDataProvider() {
                    @Override
                    public JsonObject getDatasetJson(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetJson() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/draft/in/datasetJson.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetORE(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetORE() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/draft/in/datasetORE.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery, PageRequest pageRequest) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public Stream<JsonObject> getDatasetFileDetails(FileExportQuery fileExportQuery) {
                        return Stream.empty();
                    }
                    
                    @Override
                    public JsonArray getDatasetFileDetails() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/draft/in/datasetFileDetails.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readArray();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public JsonObject getDatasetSchemaDotOrg() {
                        String pathToJsonFile =
                                "src/test/resources/croissant/draft/in/datasetSchemaDotOrg.json";
                        try (JsonReader jsonReader =
                                Json.createReader(new FileReader(pathToJsonFile))) {
                            return jsonReader.readObject();
                        } catch (FileNotFoundException ex) {
                            return null;
                        }
                    }
                    
                    @Override
                    public Document getDataCiteXml(DatasetExportQuery datasetExportQuery) {
                        return null;
                    }
                    
                    @Override
                    public String getDataCiteXml() {
                        try {
                            return Files.readString(
                                    Paths.get(
                                            "src/test/resources/croissant/draft/in/dataCiteXml.xml"),
                                    StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            return null;
                        }
                    }
                };
    }

    @Test
    public void testGetFormatName() {
        CroissantExporter instance = new CroissantExporter();
        String expResult = "";
        String result = instance.getFormatName();
        assertEquals("croissant", result);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Croissant", exporter.getDisplayName(null));
    }

    @Test
    public void testIsHarvestable() {
        assertEquals(false, exporter.isHarvestable());
    }

    @Test
    public void testIsAvailableToUsers() {
        assertEquals(true, exporter.isAvailableToUsers());
    }

    @Test
    public void testGetMediaType() {
        assertEquals("application/json", exporter.getMediaType());
    }

    @Test
    public void testExportDatasetMinimal() throws Exception {
        exporter.exportDataset(dataProviderMinimal, outputStreamMinimal);
        String actual = outputStreamMinimal.toString();
        writeCroissantFile(actual, "minimal");
        String expected =
                Files.readString(
                        Paths.get(
                                "src/test/resources/croissant/minimal/expected/minimal-croissant.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        assertEquals(prettyPrint(expected), prettyPrint(outputStreamMinimal.toString()));
    }

    @Test
    public void testExportDatasetMax() throws Exception {
        exporter.exportDataset(dataProviderMax, outputStreamMax);
        String actual = outputStreamMax.toString();
        writeCroissantFile(actual, "max");
        /*
        First, install pyDataverse from Dans-labs, the "croissant" branch:
        pip3 install --upgrade --no-cache-dir  git+https://github.com/Dans-labs/pyDataverse@croissant#egg=pyDataverse
        You can use this script to export Croissant from a dataset:
        ---
        from pyDataverse.Croissant import Croissant
        #from pyDataverse.Croissant import Croissant
        import json
        #host = "https://dataverse.nl"
        #PID = "doi:10.34894/KMRAYH"
        host = "https://beta.dataverse.org"
        PID = "doi:10.5072/FK2/VQTYHD"
        croissant = Croissant(host, PID)
        print(json.dumps(croissant.get_record(), indent=4, default=str))
        ---
        Finally, uncomment the lines below to check for differences.
         */
        //        String pyDataverse = Files.readString(Paths.get("/tmp/pyDataverse.json"),
        // StandardCharsets.UTF_8);
        //        JSONAssert.assertEquals(actual, pyDataverse, true);
        String expected =
                Files.readString(
                        Paths.get("src/test/resources/croissant/max/expected/max-croissant.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        assertEquals(prettyPrint(expected), prettyPrint(outputStreamMax.toString()));
    }

    /*
        The data in stata13-auto.dta looks something like this:
        make          price mpg rep78 headroom trunk weight length turn displacement gear_ratio foreign
        "AMC Concord" 4099  22  3     2.5      11    2930   186    40   121          3.58       0
        "AMC Pacer"   4749  17  3     3.0      11    3350   173    40   258          2.53       0
        "AMC Spirit"  3799  22        3.0      12    2640   168    35   121          3.08       0
    */
    @Test
    public void testExportDatasetCars() throws Exception {
        exporter.exportDataset(dataProviderCars, outputStreamCars);
        String actual = outputStreamCars.toString();
        writeCroissantFile(actual, "cars");
        String expected =
                Files.readString(
                        Paths.get("src/test/resources/croissant/cars/expected/cars-croissant.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        assertEquals(prettyPrint(expected), prettyPrint(outputStreamCars.toString()));
    }

    /** Same as the cars data but the stata13-auto.dta file is restricted. */
    @Test
    public void testExportDatasetRestricted() throws Exception {
        exporter.exportDataset(dataProviderRestricted, outputStreamRestricted);
        String actual = outputStreamRestricted.toString();
        writeCroissantFile(actual, "restricted");
        String expected =
                Files.readString(
                        Paths.get(
                                "src/test/resources/croissant/restricted/expected/restricted-croissant.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        assertEquals(prettyPrint(expected), prettyPrint(outputStreamRestricted.toString()));
    }

    @Test
    public void testExportDatasetJunk() throws Exception {
        exporter.exportDataset(dataProviderJunk, outputStreamJunk);
        String actual = outputStreamJunk.toString();
        writeCroissantFile(actual, "junk");
        String expected =
                Files.readString(
                        Paths.get("src/test/resources/croissant/junk/expected/junk-croissant.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        assertEquals(prettyPrint(expected), prettyPrint(outputStreamJunk.toString()));
    }

    @Test
    public void testExportDatasetDraft() throws Exception {
        exporter.exportDataset(dataProviderDraft, outputStreamDraft);
        String actual = outputStreamDraft.toString();
        writeCroissantFile(actual, "draft");
        String expected =
                Files.readString(
                        Paths.get(
                                "src/test/resources/croissant/draft/expected/draft-croissant.json"),
                        StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
        assertEquals(prettyPrint(expected), prettyPrint(outputStreamDraft.toString()));
    }

    private void writeCroissantFile(String actual, String name) throws IOException {
        Path dir =
                Files.createDirectories(Paths.get("src/test/resources/croissant/" + name + "/out"));
        Path out = Paths.get(dir + "/croissant.json");
        Files.writeString(out, prettyPrint(actual), StandardCharsets.UTF_8);
    }

    public static String prettyPrint(String jsonObject) {
        try {
            return prettyPrint(getJsonObject(jsonObject));
        } catch (Exception ex) {
            return jsonObject;
        }
    }

    public static String prettyPrint(JsonObject jsonObject) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

    public static JsonObject getJsonObject(String serializedJson) {
        try (StringReader rdr = new StringReader(serializedJson)) {
            try (JsonReader jsonReader = Json.createReader(rdr)) {
                return jsonReader.readObject();
            }
        }
    }
}
