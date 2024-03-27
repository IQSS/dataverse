/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.ingest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rmp553
 */
public class IngestableDataCheckerTest {
   
    public IngestableDataCheckerTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {

    }
    
    @BeforeEach
    public void setUp() {     
    }
    
    @AfterEach
    public void tearDown() {
        
     
    }

    
    private File createTempFile(String filename, String fileContents) throws IOException {

        if (filename == null){
            return null;
        }
        
        Path tmpFile = Files.createTempFile("ingestdatachecker", "");
        
        if (fileContents != null){
            Files.writeString(tmpFile, fileContents, StandardCharsets.UTF_8);
        }
        
        return tmpFile.toFile();
    }
    
    private MappedByteBuffer createTempFileAndGetBuffer(String filename, String fileContents) throws IOException {
        
        File fh = this.createTempFile(filename, fileContents);
        
        FileChannel srcChannel = new FileInputStream(fh).getChannel();

        // create a read-only MappedByteBuffer
        MappedByteBuffer buff = srcChannel.map(FileChannel.MapMode.READ_ONLY, 0, fh.length());

        return buff;
    }

    /**
     * Test of getTestFormatSet method, of class IngestableDataChecker.
     */
    //@Test
    /*
    public void testGetTestFormatSet() {
        System.out.println("getTestFormatSet");
        IngestableDataChecker instance = new IngestableDataChecker();
        String[] expResult = null;
        String[] result = instance.getTestFormatSet();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    */
    
    private void msg(String m){
        System.out.println(m);
    }


    private void msgt(String m){
        msg("---------------------------");
        msg(m);
        msg("---------------------------");
    }
    
    /**
     * Test of testDTAformat method, of class IngestableDataChecker.
     * @throws java.io.IOException
     */
    @Test
    public void testTestDTAformat() throws IOException {
        msgt("(1) testDTAformat");
        
        msgt("(1a) Mock a Legit Stata File (application/x-stata)");
        MappedByteBuffer buff = createTempFileAndGetBuffer("testDTA.txt", "l   ");

        IngestableDataChecker instance = new IngestableDataChecker();
        String result = instance.testDTAformat(buff);
        msg("result 1a: " + result);
        assertEquals(result, "application/x-stata");
   
        
        msgt("(1b) File is empty string (non-DTA)");
        buff = createTempFileAndGetBuffer("notDTA.txt", "");
        instance = new IngestableDataChecker();
        result = instance.testDTAformat(buff);
        msg("result 1b: " + result);
        assertEquals(result, null);
        
        
        msgt("(1c) File is some random text (non-DTA)");
        buff = createTempFileAndGetBuffer("notDTA2.txt", "hello-non-stata-file-how-are-you");
        instance = new IngestableDataChecker();
        result = instance.testDTAformat(buff);
        msg("result 1c: " + result);
        assertEquals(result, null);

        
        msgt("(1d) Mock a Legit Stata File with STATA_13_HEADER");
        buff = createTempFileAndGetBuffer("testDTA2.txt", IngestableDataChecker.STATA_13_HEADER);
        result = instance.testDTAformat(buff);
        msg("result 1d: " + result);
        assertEquals(result, "application/x-stata-13");
        
   
    }

    
    /**
     * Test of testSAVformat method, of class IngestableDataChecker.
     */
    @Test
    public void testTestSAVformat() throws IOException {
        msgt("(2) testSAVformat");

        msgt("(2a) Mock a Legit SPSS-SAV File (application/x-spss-sav)");
        MappedByteBuffer buff = createTempFileAndGetBuffer("testSAV.txt", "$FL2");

        IngestableDataChecker instance = new IngestableDataChecker();
        String result = instance.testSAVformat(buff);
        msg("result 2a: " + result);
        assertEquals(result, "application/x-spss-sav");

        msgt("(2b) File is empty string");
        buff = createTempFileAndGetBuffer("testNotSAV-empty.txt", "");

        instance = new IngestableDataChecker();
        result = instance.testSAVformat(buff);
        msg("result 2b: " + result);
        assertEquals(result, null);

        msgt("(2c) File is non-SAV string");
        buff = createTempFileAndGetBuffer("testNotSAV-string.txt", "i-am-not-a-x-spss-sav-file");
        instance = new IngestableDataChecker();
        result = instance.testSAVformat(buff);
        msg("result 2c: " + result);
        assertEquals(result, null);

    }

    /**
     * Test of testXPTformat method, of class IngestableDataChecker.
     */
    /*  @Test
    public void testTestXPTformat() {
      
        System.out.println("testXPTformat");
        MappedByteBuffer buff = null;
        IngestableDataChecker instance = new IngestableDataChecker();
        String expResult = "";
        String result = instance.testXPTformat(buff);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
        
    }*/

    /**
     * Test of testPORformat method, of class IngestableDataChecker.
     */
        /*@Test
    public void testTestPORformat() {
    
        System.out.println("testPORformat");
        MappedByteBuffer buff = null;
        IngestableDataChecker instance = new IngestableDataChecker();
        String expResult = "";
        String result = instance.testPORformat(buff);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
       
    }
 */
    /**
     * Test of testRDAformat method, of class IngestableDataChecker.
     */
       /*@Test
    public void testTestRDAformat() {
     
        System.out.println("testRDAformat");
        MappedByteBuffer buff = null;
        IngestableDataChecker instance = new IngestableDataChecker();
        String expResult = "";
        String result = instance.testRDAformat(buff);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
        
    }
*/
    /**
     * Test of detectTabularDataFormat method, of class IngestableDataChecker.
     */
    /*
    @Test
    public void testDetectTabularDataFormat() {
        System.out.println("detectTabularDataFormat");
        File fh = null;
        IngestableDataChecker instance = new IngestableDataChecker();
        String expResult = "";
        String result = instance.detectTabularDataFormat(fh);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

}
