/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator;
import edu.harvard.iq.dataverse.datavariable.DataVariable.VariableInterval;
import edu.harvard.iq.dataverse.datavariable.DataVariable.VariableType;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import org.dataverse.unf.UNFUtil;
import org.dataverse.unf.UnfException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author oscardssmith
 */
public class CSVFileReaderTest {

    private static final Logger logger = Logger.getLogger(CSVFileReaderTest.class.getCanonicalName());

    /**
     * Test CSVFileReader with a hellish CSV containing everything nasty I could
     * think of to throw at it.
     */
    @Test
    public void testRead() {
        String testFile = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/csv/IngestCSV.csv";
        String[] expResult = {"-199	\"hello\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"2017/06/20\"	0.0	1	\"2\"	\"823478788778713\"",
            "2	\"Sdfwer\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"1100/06/20\"	Inf	2	\"NaN\"	\",1,2,3\"",
            "0	\"cjlajfo.\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"3000/06/20\"	-Inf	3	\"inf\"	\"\\casdf\"",
            "-1	\"Mywer\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"06-20-2011\"	3.141592653	4	\"4.8\"	\"　 \\\"  \"",
            "266128	\"Sf\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"06-20-1917\"	0	5	\"Inf+11\"	\"\"",
            "0	\"null\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"03/03/1817\"	123	6.000001	\"11-2\"	\"\\\"adf\\0\\na\\td\\nsf\\\"\"",
            "-2389	\"\"	2013-04-08 13:14:23	2013-04-08 13:14:72	2017-06-20	\"2017-03-12\"	NaN	2	\"nap\"	\"💩⌛👩🏻■\""};
        BufferedReader result = null;
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(testFile))) {
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi(), ',');
            File outFile = instance.read(stream, false, null).getTabDelimitedFile();
            result = new BufferedReader(new FileReader(outFile));
            logger.fine("Final pass: " + outFile.getPath());
        } catch (IOException ex) {
            fail("" + ex);
        }

        String foundLine = null;
        assertNotNull(result);
        int line = 0;
        for (String expLine : expResult) {
            try {
                foundLine = result.readLine();
            } catch (IOException ex) {
                fail();
            }
            assertEquals(expLine, foundLine, "Error on line " + line);
            line++;
        }

    }

    /*
     * This test will read the CSV File From Hell, above, then will inspect
     * the DataTable object produced by the plugin, and verify that the
     * individual DataVariables have been properly typed.
     */
    @Test
    public void testVariables() {
        String testFile = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/csv/IngestCSV.csv";

        String[] expectedVariableNames = {"ints", "Strings", "Times", "Not quite Times", "Dates", "Not quite Dates",
            "Numbers", "Not quite Ints", "Not quite Numbers", "Column that hates you, contains many comas, and is verbose and long enough that it would cause ingest to fail if ingest failed when a header was more than 256 characters long. Really, it's just sadistic.　Also to make matters worse, the space at the begining of this sentance was a special unicode space designed to make you angry."};

        VariableType[] expectedVariableTypes = {VariableType.NUMERIC, VariableType.CHARACTER,
            VariableType.CHARACTER, VariableType.CHARACTER, VariableType.CHARACTER, VariableType.CHARACTER,
            VariableType.NUMERIC, VariableType.NUMERIC, VariableType.CHARACTER, VariableType.CHARACTER};

        VariableInterval[] expectedVariableIntervals = {VariableInterval.DISCRETE, VariableInterval.DISCRETE,
            VariableInterval.DISCRETE, VariableInterval.DISCRETE, VariableInterval.DISCRETE, VariableInterval.DISCRETE,
            VariableInterval.CONTINUOUS, VariableInterval.CONTINUOUS, VariableInterval.DISCRETE, VariableInterval.DISCRETE};

        String[] expectedVariableFormatCategories = {null, null, "time", "time", "date", null, null, null, null, null};

        String[] expectedVariableFormats = {null, null, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", null, null, null, null, null};

        Long expectedNumberOfCases = 7L; // aka the number of lines in the TAB file produced by the ingest plugin

        DataTable result = null;
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(testFile))) {
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi(), ',');
            result = instance.read(stream, false, null).getDataTable();
        } catch (IOException ex) {
            fail("" + ex);
        }

        assertNotNull(result);

        assertNotNull(result.getDataVariables());

        assertEquals(result.getVarQuantity(), new Long(result.getDataVariables().size()));

        assertEquals(result.getVarQuantity(), new Long(expectedVariableTypes.length));

        assertEquals(expectedNumberOfCases, result.getCaseQuantity());

        // OK, let's go through the individual variables:
        for (int i = 0; i < result.getVarQuantity(); i++) {

            assertEquals(expectedVariableNames[i], result.getDataVariables().get(i).getName(), "variable " + i + ":");

            assertEquals(expectedVariableTypes[i], result.getDataVariables().get(i).getType(), "variable " + i + ":");

            assertEquals(expectedVariableIntervals[i], result.getDataVariables().get(i).getInterval(), "variable " + i + ":");

            assertEquals(expectedVariableFormatCategories[i], result.getDataVariables().get(i).getFormatCategory(), "variable " + i + ":");

            assertEquals(expectedVariableFormats[i], result.getDataVariables().get(i).getFormat(), "variable " + i + ":");
        }
    }

    /*
     * This test will read a CSV file, then attempt to subset
     * the resulting tab-delimited file and verify that the individual variable vectors
     * are legit.
     */
    @Test
    public void testSubset() {
        String testFile = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/csv/election_precincts.csv";
        Long expectedNumberOfVariables = 13L;
        Long expectedNumberOfCases = 24L; // aka the number of lines in the TAB file produced by the ingest plugin

        TabularDataIngest ingestResult = null;

        File generatedTabFile = null;
        DataTable generatedDataTable = null;

        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(testFile))) {
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi(), ',');

            ingestResult = instance.read(stream, false, null);

            generatedTabFile = ingestResult.getTabDelimitedFile();
            generatedDataTable = ingestResult.getDataTable();
        } catch (IOException ex) {
            fail("" + ex);
        }

        assertNotNull(generatedDataTable);

        assertNotNull(generatedDataTable.getDataVariables());

        assertEquals(generatedDataTable.getVarQuantity(), new Long(generatedDataTable.getDataVariables().size()));

        assertEquals(generatedDataTable.getVarQuantity(), expectedNumberOfVariables);

        assertEquals(expectedNumberOfCases, generatedDataTable.getCaseQuantity());

        // And now let's try and subset the individual vectors
        // First, the "continuous" vectors (we should be able to read these as Double[]):
        int[] floatColumns = {2};

        Double[][] floatVectors = {
            {1.0, 3.0, 4.0, 6.0, 7.0, 8.0, 11.0, 12.0, 76.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0, 77.0},
};

        int vectorCount = 0;
        for (int i : floatColumns) {
            // We'll be subsetting the column vectors one by one, re-opening the
            // file each time. Inefficient - but we don't care here.

            if (!generatedDataTable.getDataVariables().get(i).isIntervalContinuous()) {
                fail("Column " + i + " was not properly processed as \"continuous\"");
            }
            FileInputStream generatedTabInputStream = null;
            try {
                generatedTabInputStream = new FileInputStream(generatedTabFile);
            } catch (FileNotFoundException ioex) {
                fail("Failed to open generated tab-delimited file for reading" + ioex);
            }

            Double[] columnVector = TabularSubsetGenerator.subsetDoubleVector(generatedTabInputStream, i, generatedDataTable.getCaseQuantity().intValue(), false);

            assertArrayEquals(floatVectors[vectorCount++], columnVector, "column " + i + ":");
        }

        // Discrete Numerics (aka, integers):
        int[] integerColumns = {1, 4, 6, 7, 8, 9, 10, 11, 12};

        Long[][] longVectors = {
            {1L, 3L, 4L, 6L, 7L, 8L, 11L, 12L, 76L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L, 77L},
            {1L, 2L, 3L, 4L, 5L, 11L, 13L, 15L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L},
            {85729227L, 85699791L, 640323976L, 85695847L, 637089796L, 637089973L, 85695001L, 85695077L, 1111111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L},
            {205871733L, 205871735L, 205871283L, 258627915L, 257444575L, 205871930L, 260047422L, 262439738L, 1111111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L},
            {205871673L, 205871730L, 205871733L, 205872857L, 258627915L, 257444584L, 205873413L, 262439738L, 1111111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L},
            {25025000201L, 25025081001L, 25025000701L, 25025050901L, 25025040600L, 25025000502L, 25025040401L, 25025100900L, 1111111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L},
            {250250502002L, 250250502003L, 250250501013L, 250250408011L, 250250503001L, 250250103001L, 250250406002L, 250250406001L, 1111111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L},
            {250251011024001L, 250251011013003L, 250251304041007L, 250251011013006L, 250251010016000L, 250251011024002L, 250251001005004L, 250251002003002L, 1111111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L},
            {2109L, 2110L, 2111L, 2120L, 2121L, 2115L, 2116L, 2122L, 11111L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L, 4444444L}
        };

        vectorCount = 0;

        for (int i : integerColumns) {
            if (!generatedDataTable.getDataVariables().get(i).isIntervalDiscrete()
                    || !generatedDataTable.getDataVariables().get(i).isTypeNumeric()) {
                fail("Column " + i + " was not properly processed as \"discrete numeric\"");
            }
            FileInputStream generatedTabInputStream = null;
            try {
                generatedTabInputStream = new FileInputStream(generatedTabFile);
            } catch (FileNotFoundException ioex) {
                fail("Failed to open generated tab-delimited file for reading" + ioex);
            }

            Long[] columnVector = TabularSubsetGenerator.subsetLongVector(generatedTabInputStream, i, generatedDataTable.getCaseQuantity().intValue(), false);

            assertArrayEquals(longVectors[vectorCount++], columnVector, "column " + i + ":");
        }

        // And finally, Strings:
        int[] stringColumns = {0, 3, 5};

        String[][] stringVectors = {
            {"Dog", "Squirrel", "Antelope", "Zebra", "Lion", "Gazelle", "Cat", "Giraffe", "Cat", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey", "Donkey"},
            {"East Boston", "Charlestown", "South Boston", "Bronx", "Roslindale", "Mission Hill", "Jamaica Plain", "Hyde Park", "Fenway/Kenmore", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens", "Queens"},
            {"2-06", "1-09", "1-1A", "1-1B", "2-04", "3-05", "1-1C", "1-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A", "41-10A",}
        };

        vectorCount = 0;

        for (int i : stringColumns) {
            if (!generatedDataTable.getDataVariables().get(i).isTypeCharacter()) {
                fail("Column " + i + " was not properly processed as a character vector");
            }
            FileInputStream generatedTabInputStream = null;
            try {
                generatedTabInputStream = new FileInputStream(generatedTabFile);
            } catch (FileNotFoundException ioex) {
                fail("Failed to open generated tab-delimited file for reading" + ioex);
            }

            String[] columnVector = TabularSubsetGenerator.subsetStringVector(generatedTabInputStream, i, generatedDataTable.getCaseQuantity().intValue(), false);

            assertArrayEquals(stringVectors[vectorCount++], columnVector, "column " + i + ":");
        }
    }

    /*
     * UNF test;
     * I'd like to use a file with more interesting values - "special" numbers, freaky dates, accents, etc.
     * for this. But checking it in with this simple file, for now.
     * (thinking about it, the "csv file from hell" may be a better test case for the UNF test)
     */
    @Test
    public void testVariableUNFs() {
        String testFile = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/csv/election_precincts.csv";
        Long expectedNumberOfVariables = 13L;
        Long expectedNumberOfCases = 24L; // aka the number of lines in the TAB file produced by the ingest plugin

        String[] expectedUNFs = {
            "UNF:6:wb7OATtNC/leh1sOP5IGDQ==",
            "UNF:6:0V3xQ3ea56rzKwvGt9KBCA==",
            "UNF:6:0V3xQ3ea56rzKwvGt9KBCA==",
            "UNF:6:H9inAvq5eiIHW6lpqjjKhQ==",
            "UNF:6:Bh0M6QvunZwW1VoTyioRCQ==",
            "UNF:6:o5VTaEYz+0Kudf6hQEEupQ==",
            "UNF:6:eJRvbDJkIeDPrfN2dYpRfA==",
            "UNF:6:JD1wrtM12E7evrJJ3bRFGA==",
            "UNF:6:xUKbK9hb5o0nL5/mYiy7Bw==",
            "UNF:6:Mvq3BrdzoNhjndMiVr92Ww==",
            "UNF:6:KkHM6Qlyv3QlUd+BKqqB3Q==",
            "UNF:6:EWUVuyXKSpyllsrjHnheig==",
            "UNF:6:ri9JsRJxM2xpWSIq17oWNw=="};

        TabularDataIngest ingestResult = null;

        File generatedTabFile = null;
        DataTable generatedDataTable = null;

        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(testFile))) {
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi(), ',');

            ingestResult = instance.read(stream, false, null);

            generatedTabFile = ingestResult.getTabDelimitedFile();
            generatedDataTable = ingestResult.getDataTable();
        } catch (IOException ex) {
            fail("" + ex);
        }

        assertNotNull(generatedDataTable);

        assertNotNull(generatedDataTable.getDataVariables());

        assertEquals(generatedDataTable.getVarQuantity(), new Long(generatedDataTable.getDataVariables().size()));

        assertEquals(generatedDataTable.getVarQuantity(), expectedNumberOfVariables);

        assertEquals(expectedNumberOfCases, generatedDataTable.getCaseQuantity());

        for (int i = 0; i < expectedNumberOfVariables; i++) {
            String unf = null;

            if (generatedDataTable.getDataVariables().get(i).isIntervalContinuous()) {
                FileInputStream generatedTabInputStream = null;
                try {
                    generatedTabInputStream = new FileInputStream(generatedTabFile);
                } catch (FileNotFoundException ioex) {
                    fail("Failed to open generated tab-delimited file for reading" + ioex);
                }

                Double[] columnVector = TabularSubsetGenerator.subsetDoubleVector(generatedTabInputStream, i, generatedDataTable.getCaseQuantity().intValue(), false);
                try {
                    unf = UNFUtil.calculateUNF(columnVector);
                } catch (IOException | UnfException ioex) {
                    fail("Failed to generate the UNF for variable number " + i + ", (" + generatedDataTable.getDataVariables().get(i).getName() + ", floating point)");
                }

            }
            if (generatedDataTable.getDataVariables().get(i).isIntervalDiscrete()
                    && generatedDataTable.getDataVariables().get(i).isTypeNumeric()) {

                FileInputStream generatedTabInputStream = null;
                try {
                    generatedTabInputStream = new FileInputStream(generatedTabFile);
                } catch (FileNotFoundException ioex) {
                    fail("Failed to open generated tab-delimited file for reading" + ioex);
                }

                Long[] columnVector = TabularSubsetGenerator.subsetLongVector(generatedTabInputStream, i, generatedDataTable.getCaseQuantity().intValue(), false);

                try {
                    unf = UNFUtil.calculateUNF(columnVector);
                } catch (IOException | UnfException ioex) {
                    fail("Failed to generate the UNF for variable number " + i + ", (" + generatedDataTable.getDataVariables().get(i).getName() + ", integer)");
                }

            }
            if (generatedDataTable.getDataVariables().get(i).isTypeCharacter()) {

                FileInputStream generatedTabInputStream = null;
                try {
                    generatedTabInputStream = new FileInputStream(generatedTabFile);
                } catch (FileNotFoundException ioex) {
                    fail("Failed to open generated tab-delimited file for reading" + ioex);
                }

                String[] columnVector = TabularSubsetGenerator.subsetStringVector(generatedTabInputStream, i, generatedDataTable.getCaseQuantity().intValue(), false);

                String[] dateFormats = null;

                // Special handling for Character strings that encode dates and times:
                if ("time".equals(generatedDataTable.getDataVariables().get(i).getFormatCategory())
                        || "date".equals(generatedDataTable.getDataVariables().get(i).getFormatCategory())) {

                    dateFormats = new String[expectedNumberOfCases.intValue()];
                    for (int j = 0; j < expectedNumberOfCases; j++) {
                        dateFormats[j] = generatedDataTable.getDataVariables().get(i).getFormat();
                    }
                }

                try {
                    if (dateFormats == null) {
                        unf = UNFUtil.calculateUNF(columnVector);
                    } else {
                        unf = UNFUtil.calculateUNF(columnVector, dateFormats);
                    }
                } catch (IOException | UnfException iex) {
                    fail("Failed to generate the UNF for variable number " + i + ", (" + generatedDataTable.getDataVariables().get(i).getName() + ", " + (dateFormats == null ? "String" : "Date/Time value") + ")");
                }
            }

            assertEquals(expectedUNFs[i], unf, "Variable number " + i + ":");
        }

    }

    /**
     * Tests CSVFileReader with a CSV with one more column than header. Tests
     * CSVFileReader with a null CSV.
     */
    @Test
    public void testBrokenCSV() {
        String brokenFile = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/csv/BrokenCSV.csv";
        try {
            new CSVFileReader(new CSVFileReaderSpi(), ',').read(null, false, null);
            fail("IOException not thrown on null csv");
        } catch (NullPointerException ex) {
            String expMessage = null;
            assertEquals(expMessage, ex.getMessage());
        } catch (IOException ex) {
            String expMessage = BundleUtil.getStringFromBundle("ingest.csv.nullStream");
            assertEquals(expMessage, ex.getMessage());
        }
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(brokenFile))) {
            new CSVFileReader(new CSVFileReaderSpi(), ',').read(stream, false, null);
            fail("IOException was not thrown when collumns do not align.");
        } catch (IOException ex) {
            String expMessage = BundleUtil.getStringFromBundle("ingest.csv.recordMismatch",
                                                               Arrays.asList(new String[]{"3", "6", "4"}));
            assertEquals(expMessage, ex.getMessage());
        }
    }
}
