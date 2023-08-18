/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.NullPointerException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


public class SolrQueryFormatterTest {

    public static class SolrQueryFormatterNoParamTest {
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

    /*
        public static Collection data() {
            // The following list of test cases was compiled using the interface-based approach for input-space partition.
            // Therefor, for every input parameter, the domain of possible values was partitioned into different sets:
            //   - sliceOfIds   (5 sets): null, empty, non-empty with null values only, non-empty with Long values only, non-empty with both null and Long values
            //   - paramName    (3 sets): null, empty, non-empty
            //   - dvObjectType (3 sets): null, empty, non-empty
            // Then, for every set, a representative value was chosen and combined with every other set (3*3*5 = 45 test cases).
            return Arrays.asList(new Object[][] {
                // sliceOfIds                                   paramName    dvObjectType    expectedResult                                     expectedException
                { null,                                         null,        null,           "paramName cannot be null",                        NullPointerException.class },
                { null,                                         null,        "",             "paramName cannot be null",                        NullPointerException.class },
                { null,                                         null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class },
                { null,                                         "",          null,           "sliceOfIds cannot be null",                       NullPointerException.class },
                { null,                                         "",          "",             "sliceOfIds cannot be null",                       NullPointerException.class },
                { null,                                         "",          "dvObjectType", "sliceOfIds cannot be null",                       NullPointerException.class },
                { null,                                         "paramName", null,           "sliceOfIds cannot be null",                       NullPointerException.class },
                { null,                                         "paramName", "",             "sliceOfIds cannot be null",                       NullPointerException.class },
                { null,                                         "paramName", "dvObjectType", "sliceOfIds cannot be null",                       NullPointerException.class },

                { new ArrayList<Long>(),                        null,        null,           "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(),                        null,        "",             "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(),                        null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(),                        "",          null,           "sliceOfIds must have at least 1 value",           IllegalStateException.class },
                { new ArrayList<Long>(),                        "",          "",             "sliceOfIds must have at least 1 value",           IllegalStateException.class },
                { new ArrayList<Long>(),                        "",          "dvObjectType", "sliceOfIds must have at least 1 value",           IllegalStateException.class },
                { new ArrayList<Long>(),                        "paramName", null,           "sliceOfIds must have at least 1 value",           IllegalStateException.class },
                { new ArrayList<Long>(),                        "paramName", "",             "sliceOfIds must have at least 1 value",           IllegalStateException.class },
                { new ArrayList<Long>(),                        "paramName", "dvObjectType", "sliceOfIds must have at least 1 value",           IllegalStateException.class },

                { new ArrayList<Long>() {{ add(null); }},       null,        null,           "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>() {{ add(null); }},       null,        "",             "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>() {{ add(null); }},       null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>() {{ add(null); }},       "",          null,           "(:())",                                           null },
                { new ArrayList<Long>() {{ add(null); }},       "",          "",             "(:() AND dvObjectType:())",                       null },
                { new ArrayList<Long>() {{ add(null); }},       "",          "dvObjectType", "(:() AND dvObjectType:(dvObjectType))",           null },
                { new ArrayList<Long>() {{ add(null); }},       "paramName", null,           "(paramName:())",                                  null },
                { new ArrayList<Long>() {{ add(null); }},       "paramName", "",             "(paramName:() AND dvObjectType:())",              null },
                { new ArrayList<Long>() {{ add(null); }},       "paramName", "dvObjectType", "(paramName:() AND dvObjectType:(dvObjectType))",  null },

                { new ArrayList<Long>(Arrays.asList(1L)),       null,        null,           "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(Arrays.asList(1L)),       null,        "",             "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(Arrays.asList(1L)),       null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(Arrays.asList(1L)),       "",          null,           "(:(1))",                                          null },
                { new ArrayList<Long>(Arrays.asList(1L)),       "",          "",             "(:(1) AND dvObjectType:())",                      null },
                { new ArrayList<Long>(Arrays.asList(1L)),       "",          "dvObjectType", "(:(1) AND dvObjectType:(dvObjectType))",          null },
                { new ArrayList<Long>(Arrays.asList(1L)),       "paramName", null,           "(paramName:(1))",                                 null },
                { new ArrayList<Long>(Arrays.asList(1L)),       "paramName", "",             "(paramName:(1) AND dvObjectType:())",             null },
                { new ArrayList<Long>(Arrays.asList(1L)),       "paramName", "dvObjectType", "(paramName:(1) AND dvObjectType:(dvObjectType))", null },

                { new ArrayList<Long>(Arrays.asList(1L, null)), null,        null,           "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(Arrays.asList(1L, null)), null,        "",             "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(Arrays.asList(1L, null)), null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class },
                { new ArrayList<Long>(Arrays.asList(1L, null)), "",          null,           "(:(1))",                                          null },
                { new ArrayList<Long>(Arrays.asList(1L, null)), "",          "",             "(:(1) AND dvObjectType:())",                      null },
                { new ArrayList<Long>(Arrays.asList(1L, null)), "",          "dvObjectType", "(:(1) AND dvObjectType:(dvObjectType))",          null },
                { new ArrayList<Long>(Arrays.asList(1L, null)), "paramName", null,           "(paramName:(1))",                                 null },
                { new ArrayList<Long>(Arrays.asList(1L, null)), "paramName", "",             "(paramName:(1) AND dvObjectType:())",             null },
                { new ArrayList<Long>(Arrays.asList(1L, null)), "paramName", "dvObjectType", "(paramName:(1) AND dvObjectType:(dvObjectType))", null },
            });
        }
     */
    
    /*
     * The following list of test cases was compiled using the interface-based approach for input-space partition.
     * Therefor, for every input parameter, the domain of possible values was partitioned into different sets:
     *    - sliceOfIds   (5 sets): null, empty, non-empty with null values only, non-empty with Long values only, non-empty with both null and Long values
     *    - paramName    (3 sets): null, empty, non-empty
     *    - dvObjectType (3 sets): null, empty, non-empty
     * Then, for every set, a representative value was chosen and combined with every other set (3*3*5 = 45 test cases).
     */
    static Stream<Arguments> data() {
        return Stream.of(
            // sliceOfIds                   paramName    dvObjectType    expectedResult                                     expectedException
            Arguments.of(null,              null,        null,           "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(null,              null,        "",             "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(null,              null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(null,              "",          null,           "sliceOfIds cannot be null",                       NullPointerException.class),
            Arguments.of(null,              "",          "",             "sliceOfIds cannot be null",                       NullPointerException.class),
            Arguments.of(null,              "",          "dvObjectType", "sliceOfIds cannot be null",                       NullPointerException.class),
            Arguments.of(null,              "paramName", null,           "sliceOfIds cannot be null",                       NullPointerException.class),
            Arguments.of(null,              "paramName", "",             "sliceOfIds cannot be null",                       NullPointerException.class),
            Arguments.of(null,              "paramName", "dvObjectType", "sliceOfIds cannot be null",                       NullPointerException.class),
            
            Arguments.of(list(),            null,        null,           "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(),            null,        "",             "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(),            null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(),            "",          null,           "sliceOfIds must have at least 1 value",           IllegalStateException.class),
            Arguments.of(list(),            "",          "",             "sliceOfIds must have at least 1 value",           IllegalStateException.class),
            Arguments.of(list(),            "",          "dvObjectType", "sliceOfIds must have at least 1 value",           IllegalStateException.class),
            Arguments.of(list(),            "paramName", null,           "sliceOfIds must have at least 1 value",           IllegalStateException.class),
            Arguments.of(list(),            "paramName", "",             "sliceOfIds must have at least 1 value",           IllegalStateException.class),
            Arguments.of(list(),            "paramName", "dvObjectType", "sliceOfIds must have at least 1 value",           IllegalStateException.class),
            
            Arguments.of(list((Long) null), null,        null,           "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list((Long) null), null,        "",             "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list((Long) null), null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list((Long) null), "",          null,           "(:())",                                           null),
            Arguments.of(list((Long) null), "",          "",             "(:() AND dvObjectType:())",                       null),
            Arguments.of(list((Long) null), "",          "dvObjectType", "(:() AND dvObjectType:(dvObjectType))",           null),
            Arguments.of(list((Long) null), "paramName", null,           "(paramName:())",                                  null),
            Arguments.of(list((Long) null), "paramName", "",             "(paramName:() AND dvObjectType:())",              null),
            Arguments.of(list((Long) null), "paramName", "dvObjectType", "(paramName:() AND dvObjectType:(dvObjectType))",  null),
            
            Arguments.of(list(1L),          null,        null,           "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(1L),          null,        "",             "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(1L),          null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(1L),          "",          null,           "(:(1))",                                          null),
            Arguments.of(list(1L),          "",          "",             "(:(1) AND dvObjectType:())",                      null),
            Arguments.of(list(1L),          "",          "dvObjectType", "(:(1) AND dvObjectType:(dvObjectType))",          null),
            Arguments.of(list(1L),          "paramName", null,           "(paramName:(1))",                                 null),
            Arguments.of(list(1L),          "paramName", "",             "(paramName:(1) AND dvObjectType:())",             null),
            Arguments.of(list(1L),          "paramName", "dvObjectType", "(paramName:(1) AND dvObjectType:(dvObjectType))", null),
            
            Arguments.of(list(1L, null),    null,        null,           "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(1L, null),    null,        "",             "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(1L, null),    null,        "dvObjectType", "paramName cannot be null",                        NullPointerException.class),
            Arguments.of(list(1L, null),    "",          null,           "(:(1))",                                          null),
            Arguments.of(list(1L, null),    "",          "",             "(:(1) AND dvObjectType:())",                      null),
            Arguments.of(list(1L, null),    "",          "dvObjectType", "(:(1) AND dvObjectType:(dvObjectType))",          null),
            Arguments.of(list(1L, null),    "paramName", null,           "(paramName:(1))",                                 null),
            Arguments.of(list(1L, null),    "paramName", "",             "(paramName:(1) AND dvObjectType:())",             null),
            Arguments.of(list(1L, null),    "paramName", "dvObjectType", "(paramName:(1) AND dvObjectType:(dvObjectType))", null)
        );
    }
    
    /**
     * @param expectedResult May either be (i) the expected query part or (ii) the expected exception message
     */
    @ParameterizedTest
    @MethodSource("data")
    void testFormatIdsForSolrClause(List<Long> sliceOfIds, String paramName, String dvObjectType,
                                    String expectedResult, Class<Throwable> expectedException) {
        SolrQueryFormatter sqf = new SolrQueryFormatter();
        
        if (expectedException == null) {
            assertEquals(expectedResult, sqf.formatIdsForSolrClause(sliceOfIds, paramName, dvObjectType));
            return;
        }
        
        Throwable e = assertThrows(expectedException, () -> sqf.formatIdsForSolrClause(sliceOfIds, paramName, dvObjectType));
        assertEquals(expectedResult, e.getMessage());
    }
    
    static List<Long> list(Long... args) {
        return Arrays.asList(args);
    }
}
