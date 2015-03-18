/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.search;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SearchUtilTest {

    public SearchUtilTest() {
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

    @Test
    public void testSanitizeQuery() {
        System.out.println("sanitizeQuery");
        assertEquals(null, SearchUtil.sanitizeQuery(null));
        assertEquals("", SearchUtil.sanitizeQuery(""));
        assertEquals("doi\\:10.5072/FK2/4QEJQV", SearchUtil.sanitizeQuery("doi:10.5072/FK2/4QEJQV"));
        assertEquals("datasetPersistentIdentifier:doi\\:10.5072/FK2/4QEJQV", SearchUtil.sanitizeQuery("datasetPersistentIdentifier:doi:10.5072/FK2/4QEJQV"));
        assertEquals("doi\\:4QEJQV", SearchUtil.sanitizeQuery("doi:4QEJQV"));
        assertEquals("hdl\\:1902.1/21919", SearchUtil.sanitizeQuery("hdl:1902.1/21919"));
        assertEquals("datasetPersistentIdentifier:hdl\\:1902.1/21919", SearchUtil.sanitizeQuery("datasetPersistentIdentifier:hdl:1902.1/21919"));
    }

}
