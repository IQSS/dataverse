/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import java.util.Arrays;
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
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    /**
     * Test of getNumResults method, of class Pager.
     */
    @Test
    public void testBasics() {
        System.out.println("getNumResults");

       
        pager1 = new Pager(102, 10, 1);

        msgt("Test: 102 results, 10 per page, page 1");
        assertEquals(true, pager1.isPagerNecessary());

        assertEquals(102, pager1.getNumResults());
        assertEquals(1, pager1.getPreviousPageNumber());
        assertEquals(2, pager1.getNextPageNumber());
        assertEquals(false, pager1.hasPreviousPageNumber());
        assertEquals(true, pager1.hasNextPageNumber());
        
        msg("page list: " + Arrays.toString(pager1.getPageNumberList()));
        //assertEquals(new int[]{1, 2, 3, 4, 5}, pager1.getPageNumberList());
        assertEquals(1, pager1.getPageNumberList()[0]);
        assertEquals(5, pager1.getPageNumberList()[4]);
    
        assertEquals(1, pager1.getStartCardNumber());
        assertEquals(10, pager1.getEndCardNumber());

        pager1 = new Pager(102, 10, 6);
        msgt("Test: 102 results, 10 per page, page 6");

        msg("page list: " + Arrays.toString(pager1.getPageNumberList()));
        assertEquals(4, pager1.getPageNumberList()[0]);
        assertEquals(8, pager1.getPageNumberList()[4]);

        msgt("Test: 100 results, 10 per page, page 9");
        pager1 = new Pager(100, 10, 9);
        msg("page list: " + Arrays.toString(pager1.getPageNumberList()));
        assertEquals(6, pager1.getPageNumberList()[0]);
        assertEquals(10, pager1.getPageNumberList()[4]);

        msgt("Test: 100 results, 10 per page, page 10");
        pager1 = new Pager(100, 10, 10);
        msg("page list: " + Arrays.toString(pager1.getPageNumberList()));
        assertEquals(6, pager1.getPageNumberList()[0]);
        assertEquals(10, pager1.getPageNumberList()[4]);

        msgt("Test: 102 results, 10 per page, page 9");
        pager1 = new Pager(102, 10, 9);
        msg("page list: " + Arrays.toString(pager1.getPageNumberList()));
        assertEquals(7, pager1.getPageNumberList()[0]);
        assertEquals(11, pager1.getPageNumberList()[4]);

        /*
        pager1 = new Pager(102, 10, 5);
        assertEquals(3, pager1.getPageNumberList()[0]);

        pager1 = new Pager(102, 10, 7);
        msg("page list Pager(102, 10, 7): " + Arrays.toString(pager1.getPageNumberList()));
 //       assertEquals(4, pager1.getPageNumberList()[0]);

        pager1 = new Pager(102, 10, 10);
   //     assertEquals(6, pager1.getPageNumberList()[0]);
*/
    } 

    @Test
    public void testNoResults() {
        
        System.out.println("getNumResults");
        pager1 = new Pager(0, 10, 1);

        assertEquals(false, pager1.isPagerNecessary());
        
        assertEquals(0, pager1.getNumResults());
        assertEquals(0, pager1.getPreviousPageNumber());
        assertEquals(0, pager1.getNextPageNumber());
        assertEquals(false, pager1.hasPreviousPageNumber());
        assertEquals(false, pager1.hasNextPageNumber());
        
        msgt("page list: " + Arrays.toString(pager1.getPageNumberList()));
        //assertEquals(null, pager1.getPageNumberList());
        //assertEquals(1, pager1.getPageNumberList()[pager1.getPageCount()-1]);

        assertEquals(0, pager1.getStartCardNumber());
        assertEquals(0, pager1.getEndCardNumber());
       
    } 


}
