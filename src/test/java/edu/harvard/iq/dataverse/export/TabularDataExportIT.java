package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionFilesServiceBean;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetRecipe;
import edu.harvard.iq.dataverse.util.xml.XmlUtil;
import io.gdcc.spi.export.FileExportQuery;
import io.gdcc.spi.export.FileMetadataPredicates;
import io.gdcc.spi.export.PageRequest;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@JpaPerformanceTest
class TabularDataExportIT {

    static JpaEntityManagerService jpa;

    static Dataset tabularFilesDataset;
    static int numberOfFiles = 20;
    static int numberOfVariables = 100;
    static int numberOfBatches = 4;
    static List<Integer> varQuantityMap = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        jpa.start();

        DatasetTypeRecipe datasetType = DatasetTypeRecipe.dataset();

        DatasetRecipe tabularFiles = DatasetRecipe.of(
                datasetType,
                VersionRecipe.of(
                        FileRecipe.tabular(numberOfFiles, VariableSetRecipe.uniform(numberOfVariables))
                )
        );

        // Build the fixture
        var tabularFixture = DatasetFixtureBuilder.builder().recipe(tabularFiles).build();

        // Some save the type entity in the database and let the ORM create the mappings
        jpa.inTransactionVoid(em -> em.persist(tabularFixture.datasetType()));

        // Persist the actual dataset
        tabularFilesDataset = tabularFixture.dataset();
        jpa.inTransactionVoid(em -> {
            // DataFile has no cascade path from Dataset, so each file must be persisted explicitly before
            // the dataset graph is flushed.
            for (DataFile dataFile : tabularFixture.dataFiles()) {
                em.persist(dataFile);
            }
            em.persist(tabularFilesDataset);
        });

        for (int i = 0; i < numberOfFiles; i++) {
            varQuantityMap.add(numberOfVariables);
            // Something to consider in the future - add a more complex recipe, 
            // with varying numbers of variables in each file.
        }
    }

    @Test
    void exportTabularMetadata() {
        Long datasetVersionId = tabularFilesDataset.getVersions().get(0).getId();
        System.out.println("version id: " + datasetVersionId);

        QueryCountHolder.clear();
        Instant start = Instant.now();

        // First, obtain the entire mess of the tabular data in the dataset in 
        // in one go, using the legacy .getDatasetFileDetails() method: 
        String json = jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB. Check fixtures!");

            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            JsonArray details = provider.getDatasetFileDetails();
            // We want to parse this json and make sure that the expected number 
            // of files, datatables and variables has been returned.             

            assertEquals(numberOfFiles, details.size(), "Number of tabular files retrieved does not match the number used in the recipe");

            int total = 0;
            for (int i = 0; i < details.size(); i++) {
                JsonObject fileJson = details.getJsonObject(i);
                JsonObject dataTable = fileJson.getJsonArray("dataTables").getJsonObject(0);
                JsonArray vars = dataTable.getJsonArray("dataVariables");
                System.out.println(vars.size() + " variables retrieved for file " + fileJson.getJsonNumber("id"));
                total += vars.size();
            }
            assertEquals(numberOfFiles * numberOfVariables, total, "Failed to retrieve and parse the expected total number of variables");
            return details.toString();
        });

        assertNotNull(json);

        System.out.println("test json produced: " + json);

        // Calculate the md5 of the complete output, for verification further down
        String md5 = calculateMD5(json);
        assertNotNull(md5);
        System.out.println("MD5 Hash: " + md5);

        Instant end = Instant.now();
        long elapsed = start.until(end, ChronoUnit.MILLIS);

        QueryCount count = QueryCountHolder.getGrandTotal();

        long queriesTotal = count.getTotal();
        long queriesSelect = count.getSelect();

        System.out.println("Elapsed ms: " + elapsed);
        System.out.println("Total queries: " + queriesTotal);
        System.out.println("Select queries: " + queriesSelect);

        // And now acquire the same content using the new, paginated methods; 
        // then compare the checksums, to ensure the 2 methods produce the same
        // exact result.
        start = Instant.now();

        json = jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB. Check fixtures!");

            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            DatasetVersionFilesServiceBean versionFilesService = new DatasetVersionFilesServiceBean();
            versionFilesService.injectEntityManager(em);
            provider.injectVersionFilesService(versionFilesService);

            JsonArrayBuilder jab = Json.createArrayBuilder();
            int filesPerBatch = numberOfFiles / numberOfBatches;

            for (int i = 0; i < numberOfBatches; i++) {

                FileExportQuery exportQuery = FileExportQuery.builder()
                        .addFilePredicate(FileMetadataPredicates.ONLY_PUBLIC_FILES)
                        .addFilePredicate(FileMetadataPredicates.ONLY_TABULAR_FILES)
                        .addFilePredicate(FileMetadataPredicates.INCLUDE_TABULAR_DATA_VARIABLES)
                        .build();
                PageRequest paginationRequest = PageRequest.of(filesPerBatch * i, filesPerBatch);

                Stream<JsonObject> details = provider.getDatasetFileDetails(exportQuery, paginationRequest);

                Iterator<JsonObject> it = details.iterator();
                int filesRetrieved = 0;
                while (it.hasNext()) {
                    JsonObject fileJson = it.next();
                    jab.add(fileJson);
                    filesRetrieved++;
                }
                assertEquals(filesPerBatch, filesRetrieved, "Failed to retrieve the expected number of tabular files in batch number " + i);
            }

            // Parse the combined json, count the variables, confirm: 
            JsonArray jsonCombined = jab.build().asJsonArray();

            int total = 0;
            for (int i = 0; i < jsonCombined.size(); i++) {
                JsonObject fileJson = jsonCombined.getJsonObject(i);
                JsonObject dataTable = fileJson.getJsonArray("dataTables").getJsonObject(0);
                JsonArray vars = dataTable.getJsonArray("dataVariables");
                total += vars.size();
            }
            assertEquals(numberOfFiles * numberOfVariables, total, "Failed to retrieve and parse the expected total number of variables using new, paginated methods");

            return jsonCombined.toString();
        });

        end = Instant.now();
        elapsed = start.until(end, ChronoUnit.MILLIS);

        System.out.println("test json produced, paginated: " + json);

        String md5paginated = calculateMD5(json);
        assertNotNull(md5paginated);
        System.out.println("MD5 Hash: " + md5paginated);

        assertEquals(md5, md5paginated, "MD5 Hash mismatch between json fragments produced by the legacy, and paginated ExportDataProvder methods");

        count = QueryCountHolder.getGrandTotal();
        System.out.println("Elapsed ms using paginated methods: " + elapsed);
        System.out.println("Total queries using paginated methods: " + (count.getTotal() - queriesTotal));
        System.out.println("Select queries using paginated methods: " + (count.getSelect() - queriesSelect));

        // And now try to export the dataVariable-level metadata as the <dataDscr> 
        // fragment of DDI xml. 
        // (we are not interested in exporting a full DDI for this imaginary 
        // dataset since there are dedicated tests for the dataset-level DDI 
        // exports elsewhere).
        String xml = jpa.inTransaction(em -> {
            var datasetVersion = em.find(DatasetVersion.class, datasetVersionId);
            assumeTrue(datasetVersion != null, "No dataset version available in DB. Check fixtures!");

            InternalExportDataProvider provider = new InternalExportDataProvider(datasetVersion);
            DatasetVersionFilesServiceBean versionFilesService = new DatasetVersionFilesServiceBean();
            versionFilesService.injectEntityManager(em);
            provider.injectVersionFilesService(versionFilesService);

            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            XMLStreamWriter xmlw = null;
            try {
                xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(byteOutputStream);
                DdiExportUtil.createDataDscrInBatches(xmlw, varQuantityMap, provider, numberOfFiles * numberOfVariables / numberOfBatches);
                xmlw.flush();
            } catch (XMLStreamException xse) {
                assertTrue(false, "Failed to export the dataDscr DDI section, XMLStreamException: " + xse.getMessage());
            } finally {
                if (xmlw != null) {
                    try {
                        xmlw.close();
                    } catch (XMLStreamException e) {
                        // we don't care at this point
                    }
                }
            }

            return byteOutputStream.toString(StandardCharsets.UTF_8);
        });

        System.out.println("test DDI xml produced: " + xml);

        // Finally, let's parse the resulting XML and confirm that the expected
        // numbers of unique datatables and variables have been exported. 
        // There are obvious ways in which the test can be made more thorough. 
        // For example, we can trace each variable by name and confirm that each 
        // one is properly exported in both the json and ddi xml formats. 
        StringReader reader = new StringReader(xml);
        XMLStreamReader xmlr = null;
        XMLInputFactory xmlFactory = XmlUtil.getSecureXMLInputFactory();

        int dataDscrVariablesTotal = 0;
        boolean dataDscrComplete = false;
        boolean dataDscrVarElementOpen = false;
        Set<String> dataDscrDistinctFiles = new HashSet<>();

        try {
            xmlr = xmlFactory.createXMLStreamReader(reader);
        } catch (XMLStreamException xse) {
            assertTrue(false, "Failed to parse the produced dataDscr fragment as valid xml, XMLStreamException: " + xse.getMessage());
        }
        try {
            xmlr.nextTag();
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "dataDscr");
        } catch (XMLStreamException xse) {
            assertTrue(false, "The produced xml fragment does not start with a dataDscr tag");
        }

        try {
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("var")) {
                        assertFalse(dataDscrVarElementOpen, "Out of order, nested <var> tag encountered");
                        dataDscrVarElementOpen = true;
                    } else if (xmlr.getLocalName().equals("location")) {
                        assertTrue(dataDscrVarElementOpen, "Out of order <location> tag encountered");
                        String fileId = xmlr.getAttributeValue(null, "fileid");
                        assertNotNull(fileId, "<location> element without a valid fileid attribute encountered");
                        dataDscrDistinctFiles.add(fileId);
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (xmlr.getLocalName().equals("var")) {
                        assertTrue(dataDscrVarElementOpen, "Out of order closing </var> tag encountered");
                        dataDscrVariablesTotal++;
                        dataDscrVarElementOpen = false;
                    } else if (xmlr.getLocalName().equals("dataDscr")) {
                        dataDscrComplete = true;
                    }
                }
            }
        } catch (XMLStreamException xse) {
            assertTrue(false, "Unexpected XMLStreamException when attempting to parse the dataDscr section: " + xse.getMessage());
        } finally {
            if (xmlr != null) {
                try {xmlr.close();} catch (XMLStreamException e) {}
            }
        }

        assertTrue(dataDscrComplete, "<dataDscr> section not closed properly");
        assertFalse(dataDscrVarElementOpen, "an extra, unterminated <var> section in the <dataDscr>");
        assertEquals(numberOfFiles * numberOfVariables, dataDscrVariablesTotal, "Failed to parse the expected total number of variables in the generated <dataDscr> section");
        assertEquals(numberOfFiles, dataDscrDistinctFiles.size(), "Invalid number of distinct tabular datafiles in the exported ddi fragment");

        // In all of the tests above, the final result - the entire dataset-worth
        // of exported tabular data - is passed around as a string; in the last 
        // method, this combined string then get re-parsed as xml for validation 
        // purposes. This is working adequately for the numbers of files and 
        // variables used; but if we want to use this code for true stress-testing
        // with gigantic amounts of metadata, it will need to be rewritten to 
        // to stream the data in real time, for writing and reading, to avoid 
        // having to keep the whole mess in memory at once.
    }

    private String calculateMD5(String source) {
        String md5 = null;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes(StandardCharsets.UTF_8));

            byte[] hashBytes = md.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            md5 = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("MD5 algorithm not found!");
            //e.printStackTrace();
        }
        return md5;
    }
}
