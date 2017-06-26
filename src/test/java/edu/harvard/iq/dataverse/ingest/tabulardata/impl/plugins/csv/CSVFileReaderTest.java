/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
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
        String testFile = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/csv/InjestCSV.csv";
        String[] expResult = {"ints	Strings	Times	Not quite Times	Dates	Not quite Dates	Numbers	Not quite Ints	Not quite Numbers	Column that hates you and is so long that things might start breaking because we previously had a 255 character limit on length for things even when that might not be completely justified. Wow this is an increadibly long header name. Who made this? Oh, that's right, I did.",
            "-199	\"hello\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"2017/06/20\"	0.0	1	\"2\"	\"823478788778713\"",
            "2	\"Sdfwer\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"1100/06/20\"	Inf	2	\"NaN\"	\",1,2,3\"",
            "0	\"cjlajfo.\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"3000/06/20\"	-Inf	3	\"inf\"	\"\\casdf\"",
            "-1	\"Mywer\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"06-20-2011\"	3.141592653	4	\"4.8\"	\" \\\"  \"",
            "266128	\"Sf\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"06-20-1917\"	0	5	\"Inf+11\"	\"\"",
            "0	\"werxc\"	2013-04-08 13:14:23	2013-04-08 13:14:23	2017-06-20	\"03/03/1817\"	123	6.000001	\"11-2\"	\"adf\\0\\na\\td",
            "sf\"",
            "-2389	\"Dfjl\"	2013-04-08 13:14:23	2013-04-08 13:14:72	2017-06-20	\"2017-03-12\"	NaN	2	\"nap\"	\"💩⌛👩🏻■\""};
        BufferedReader result = null;
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(testFile))) {
            CSVFileReader instance = new CSVFileReader(new CSVFileReaderSpi());
            result = new BufferedReader(new FileReader(instance.read(stream, null).getTabDelimitedFile()));
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
        } catch (IOException ex) {
            String expMessage = "Stream can't be null.";
            assertEquals(expMessage, ex.getMessage());
        }
        try (BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(brokenFile))) {
            new CSVFileReader(new CSVFileReaderSpi()).read(stream, null);
            fail("IOException was not thrown when collumns do not align.");
        } catch (IOException ex) {
            String expMessage = "Reading mismatch, line 5 of the Data file: 6 delimited values expected, 4 found.";
            assertEquals(expMessage, ex.getMessage());
        }
    }
}
