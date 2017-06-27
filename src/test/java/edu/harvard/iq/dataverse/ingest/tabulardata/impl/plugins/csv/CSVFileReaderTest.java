/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author oscardssmith
 */
public class CSVFileReaderTest {

    private static final Logger logger = Logger.getLogger(CSVFileReaderTest.class.getCanonicalName());

    /**
     * Test CSVFileReader with a hellish CSV containing everything nasty I could
     * think of to throw at it.
     *
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
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi());
            File outFile = instance.read(stream, null).getTabDelimitedFile();
            result = new BufferedReader(new FileReader(outFile));
            logger.fine("Final pass: " + outFile.getPath());
        } catch (IOException ex) {
            fail("" + ex);
        }

        String foundLine = null;
        assertNotNull(result);
        for (String expLine : expResult) {
            try {
                foundLine = result.readLine();
            } catch (IOException ex) {
                fail();
            }
            if (!expLine.equals(foundLine)) {
                logger.info("expected: " + expLine);
                logger.info("found : " + foundLine);
            }
            assertEquals(expLine, foundLine);
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

        String[] expectedVariableNames = {"ints", "Strings", "Times", "Not quite Times", "Dates", "Not quite Dates", "Numbers", "Not quite Ints", "Not quite Numbers", "Column that hates you, contains many comas, and is verbose and long enough that it would cause ingest to fail if ingest failed when a header was more than 256 characters long. Really, it's just sadistic.　Also to make matters worse, the space at the begining of this sentance was a special unicode space designed to make you angry."};

        DataVariable.VariableType[] expectedVariableTypes = {DataVariable.VariableType.NUMERIC, DataVariable.VariableType.CHARACTER, DataVariable.VariableType.CHARACTER, DataVariable.VariableType.CHARACTER, DataVariable.VariableType.CHARACTER,
            DataVariable.VariableType.CHARACTER, DataVariable.VariableType.NUMERIC, DataVariable.VariableType.NUMERIC, DataVariable.VariableType.CHARACTER, DataVariable.VariableType.CHARACTER};

        DataVariable.VariableInterval[] expectedVariableIntervals = {DataVariable.VariableInterval.DISCRETE, DataVariable.VariableInterval.DISCRETE, DataVariable.VariableInterval.DISCRETE, DataVariable.VariableInterval.DISCRETE, DataVariable.VariableInterval.DISCRETE,
            DataVariable.VariableInterval.DISCRETE, DataVariable.VariableInterval.CONTINUOUS, DataVariable.VariableInterval.CONTINUOUS, DataVariable.VariableInterval.DISCRETE, DataVariable.VariableInterval.DISCRETE};

        String[] expectedVariableFormatCategories = {null, null, "time", "time", "date", null, null, null, null, null};

        String[] expectedVariableFormats = {null, null, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", null, null, null, null, null};

        Long expectedNumberOfCases = 7L; // aka the number of lines in the TAB file produced by the ingest plugin

        DataTable result = null;
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(testFile))) {
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi());
            result = instance.read(stream, null).getDataTable();
        } catch (IOException ex) {
            fail("" + ex);
        }

        assertNotNull(result);

        assertNotNull(result.getDataVariables());

        assertEquals(result.getVarQuantity(), new Long(result.getDataVariables().size()));

        if (result.getVarQuantity() != expectedVariableTypes.length) {
            logger.info("number of variables expected: " + expectedVariableTypes.length);
            logger.info("number of variables produced: " + result.getVarQuantity());
        }

        assertEquals(result.getVarQuantity(), new Long(expectedVariableTypes.length));

        if (!expectedNumberOfCases.equals(result.getCaseQuantity())) {
            logger.info("number of observations expected: " + expectedNumberOfCases);
            logger.info("number of observations produced: " + result.getCaseQuantity());
        }

        assertEquals(expectedNumberOfCases, result.getCaseQuantity());

        // OK, let's go through the individual variables:
        for (int i = 0; i < result.getVarQuantity(); i++) {

            if (!expectedVariableNames[i].equals(result.getDataVariables().get(i).getName())) {
                logger.info("variable " + i + ", name expected: " + expectedVariableNames[i]);
                logger.info("variable " + i + ", name produced: " + result.getDataVariables().get(i).getName());
            }

            assertEquals(expectedVariableNames[i], result.getDataVariables().get(i).getName());

            if (!expectedVariableTypes[i].equals(result.getDataVariables().get(i).getType())) {
                logger.info("variable " + i + ", type expected: " + expectedVariableTypes[i].toString());
                logger.info("variable " + i + ", type produced: " + result.getDataVariables().get(i).getType().toString());
            }

            assertEquals(expectedVariableTypes[i], result.getDataVariables().get(i).getType());

            if (!expectedVariableIntervals[i].equals(result.getDataVariables().get(i).getInterval())) {
                logger.info("variable " + i + ", interval expected: " + expectedVariableIntervals[i].toString());
                logger.info("variable " + i + ", interval produced: " + result.getDataVariables().get(i).getInterval().toString());
            }

            assertEquals(expectedVariableIntervals[i], result.getDataVariables().get(i).getInterval());

            if ((expectedVariableFormatCategories[i] != null && !expectedVariableFormatCategories[i].equals(result.getDataVariables().get(i).getFormatCategory()))
                    || (expectedVariableFormatCategories[i] == null && result.getDataVariables().get(i).getFormatCategory() != null)) {
                logger.info("variable " + i + ", format category expected: " + expectedVariableFormatCategories[i]);
                logger.info("variable " + i + ", format category produced: " + result.getDataVariables().get(i).getFormatCategory());
            }

            assertEquals(expectedVariableFormatCategories[i], result.getDataVariables().get(i).getFormatCategory());

            if ((expectedVariableFormats[i] != null && !expectedVariableFormats[i].equals(result.getDataVariables().get(i).getFormat()))
                    || (expectedVariableFormats[i] == null && result.getDataVariables().get(i).getFormat() != null)) {
                logger.info("variable " + i + ", format expected: " + expectedVariableFormats[i]);
                logger.info("variable " + i + ", format produced: " + result.getDataVariables().get(i).getFormat());
            }

            assertEquals(expectedVariableFormats[i], result.getDataVariables().get(i).getFormat());

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
            new CSVFileReader(new CSVFileReaderSpi()).read(null, null);
            fail("IOException not thrown on null csv");
        } catch (NullPointerException ex) {
            String expMessage = null;
            assertEquals(expMessage, ex.getMessage());
        } catch (IOException ex) {
            String expMessage = "Stream can't be null.";
            assertEquals(expMessage, ex.getMessage());
        }
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(brokenFile))) {
            new CSVFileReader(new CSVFileReaderSpi()).read(stream, null);
            fail("IOException was not thrown when collumns do not align.");
        } catch (IOException ex) {
            String expMessage = "Reading mismatch, line 3 of the Data file: 6 delimited values expected, 4 found.";
            assertEquals(expMessage, ex.getMessage());
        }
    }
}
