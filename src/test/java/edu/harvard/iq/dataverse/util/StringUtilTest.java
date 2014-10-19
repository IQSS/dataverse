/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.util.StringUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class StringUtilTest {
    
    public StringUtilTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of isEmpty method, of class StringUtil.
     */
    @Test
    public void testIsEmpty() {
        assertTrue( StringUtil.isEmpty(null) );
        assertTrue( StringUtil.isEmpty("") );
        assertTrue( StringUtil.isEmpty(" ") );
        assertTrue( StringUtil.isEmpty("\t") );
        assertTrue( StringUtil.isEmpty("\t \t \n") );
        assertFalse( StringUtil.isEmpty("a") );
    }

    /**
     * Test of isAlphaNumeric method, of class StringUtil.
     */
    @Test
    public void testIsAlphaNumeric() {
        assertTrue( StringUtil.isAlphaNumeric("abc") );
        assertTrue( StringUtil.isAlphaNumeric("1230") );
        assertTrue( StringUtil.isAlphaNumeric("1230abc") );
        assertTrue( StringUtil.isAlphaNumeric("1230abcABC") );
        assertFalse( StringUtil.isAlphaNumeric("1230abcABC#") );
    }

    /**
     * Test of isAlphaNumericChar method, of class StringUtil.
     */
    @Test
    public void testIsAlphaNumericChar() {
        assertTrue( StringUtil.isAlphaNumericChar('a') );
        assertTrue( StringUtil.isAlphaNumericChar('f') );
        assertTrue( StringUtil.isAlphaNumericChar('z') );
        assertTrue( StringUtil.isAlphaNumericChar('0') );
        assertTrue( StringUtil.isAlphaNumericChar('1') );
        assertTrue( StringUtil.isAlphaNumericChar('9') );
        assertTrue( StringUtil.isAlphaNumericChar('A') );
        assertTrue( StringUtil.isAlphaNumericChar('G') );
        assertTrue( StringUtil.isAlphaNumericChar('Z') );
        assertFalse( StringUtil.isAlphaNumericChar('@') );
    }
    
}
