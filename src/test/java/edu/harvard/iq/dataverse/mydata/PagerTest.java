/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rmp553
 */
public class PagerTest {
    private Pager pager1;
    
    public PagerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        this.pager1 = new Pager(100, 10, 1);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getNumResults method, of class Pager.
     */
    @Test
    public void testGetNumResults() {
        System.out.println("getNumResults");
        assertEquals(100, pager1.getNumResults());
    }


}
