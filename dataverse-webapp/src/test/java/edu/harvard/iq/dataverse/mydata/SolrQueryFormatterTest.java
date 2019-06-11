/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
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
public class SolrQueryFormatterTest {
    
    public SolrQueryFormatterTest() {
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

    private Long[] getRandomListOfLongs(int listCount, int topNumber){
       
        Random r = new Random();
        
        Long[] array = new Long[listCount];
        long last = 0;
        for (int idx = 0; idx < listCount; idx++) {
            last = r.nextInt(topNumber);// + 1;
            array[idx] = last;
        }
        return array;
    }
    
    private Long[] getListOfLongs(int listCount){
        Long[] array = new Long[listCount];
        for (long a = 0; a < array.length; a++) {
            array[(int)a] = a+1;
        }
        return array;
    }
    
    /**
     * Test of buildIdQuery method, of class SolrQueryFormatter.
     */
    @Test
    public void testBasics() {
        msgt("Set group size to 10--it's usually, 1,000");
        SolrQueryFormatter sqf = new SolrQueryFormatter();
        sqf.setSolrIdGroupSize(10);

        String paramName = "entityId";
        // -----------------------------------------
        msgt("List of 10 ids from 1, 10");        
        // -----------------------------------------
        //makeQueryTest(sqf, 10, paramName, "(entityId:(1 2 3 4 5 6 7 8 9 10))");
        makeQueryTest2(sqf, 10, paramName, 1);

        // -----------------------------------------
        msgt("List of 11 ids from 1, 11");        
        // -----------------------------------------
        //makeQueryTest(sqf, 11, paramName, "(entityId:(1 2 3 4 5 6 7 8 9 10)) OR (entityId:(11))");
        makeQueryTest2(sqf, 11, paramName, 2);

        // -----------------------------------------
        msgt("List of 21 ids from 1, 21");        
        // -----------------------------------------
        //makeQueryTest(sqf, 21, paramName, "(entityId:(1 2 3 4 5 6 7 8 9 10)) OR (entityId:(11 12 13 14 15 17 16 19 18 21)) OR (entityId:(20))");
        makeQueryTest2(sqf, 21, paramName, 3);

        // -----------------------------------------
        msgt("List of ids is empty");        
        // -----------------------------------------
        makeQueryTest(sqf, 0, paramName, null);
        //makeQueryTest2(sqf, 0, paramName, null);

        // -----------------------------------------
        msgt("List of ids from 1 to 11");        
        // -----------------------------------------
        msgt("Set to groups of 3");
        sqf.setSolrIdGroupSize(3);
        String expectedResult = "(parentId:(1 2 3)) OR (parentId:(4 5 6)) OR (parentId:(7 8 9)) OR (parentId:(10 11))";//([parentId:(1 2 3)) OR (parentId:(4 5 6)) OR (parentId:(7 8 9)) OR (parentId:(10 11 12)) OR (parentId:(13 14 15)) OR (parentId:(17 16 19)) OR (parentId:(18 21 20)) OR (parentId:(23 22 25)) OR (parentId:(24 27 26)) OR (parentId:(29 28 31)) OR (parentId:(30 34 35)) OR (parentId:(32 33 38)) OR (parentId:(39 36 37)) OR (parentId:(42 43 40)) OR (parentId:(41 46 47)) OR (parentId:(44 45 51)) OR (parentId:(50 49 48)) OR (parentId:(55 54 53)) OR (parentId:(52 59 58)) OR (parentId:(57 56 63)) OR (parentId:(62 61 60)) OR (parentId:(68 69 70)) OR (parentId:(71 64 65)) OR (parentId:(66 67]))> but was:<([entityId:(1 2 3 4 5 6 7 8 9 10)) OR (entityId:(11 12 13 14 15 17 16 19 18 21)) OR (entityId:(20]))";
        //makeQueryTest(sqf, 11, "parentId", expectedResult);
        makeQueryTest2(sqf, 11, "parentId", 4);
        
    }
 
    private void makeQueryTest2(SolrQueryFormatter sqf, int numIds, String paramName, int numParamOccurrences){
        
        Long[] idList = this.getListOfLongs(numIds);
        Set<Long> idListSet = new HashSet<>(Arrays.asList(idList));                 
                
        String queryClause = sqf.buildIdQuery(idListSet, paramName, null);
        msgt("query clause: " + queryClause);
        assertEquals(StringUtils.countMatches(queryClause, paramName), numParamOccurrences);
    }
       
    private void makeQueryTest(SolrQueryFormatter sqf, int numIds, String paramName, String expectedQuery){
        
        Long[] idList = this.getListOfLongs(numIds);
        Set<Long> idListSet = new HashSet<>(Arrays.asList(idList));                 
                
        String queryClause = sqf.buildIdQuery(idListSet, paramName, null);
        msgt("query clause: " + queryClause);
        assertEquals(queryClause, expectedQuery);
    }
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
}
