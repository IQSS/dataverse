package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class MetricsUtilTest {

    public static class MetricsUtilNoParamTest {

        private static final long COUNT = 42l;

        @Test
        public void testCountToJson() {
            // This constructor is just here for code coverage. :)
            MetricsUtil metricsUtil = new MetricsUtil();
            JsonObject jsonObject = MetricsUtil.countToJson(COUNT).build();
            System.out.println(JsonUtil.prettyPrint(jsonObject));
            assertEquals(COUNT, jsonObject.getJsonNumber("count").longValue());
        }

        @Test
        public void testDataversesByCategoryToJson() {
            List<Object[]> list = new ArrayList<>();
            Object[] obj00 = { "RESEARCH_PROJECTS", 791l };
            Object[] obj01 = { "RESEARCHERS", 745l };
            Object[] obj02 = { "UNCATEGORIZED", 565l };
            Object[] obj03 = { "ORGANIZATIONS_INSTITUTIONS", 250l };
            Object[] obj04 = { "JOURNALS", 106l };
            Object[] obj05 = { "RESEARCH_GROUP", 106l };
            Object[] obj06 = { "TEACHING_COURSES", 20l };
            Object[] obj07 = { "LABORATORY", 17l };
            Object[] obj08 = { "DEPARTMENT", 7l };
            list.add(obj00);
            list.add(obj01);
            list.add(obj02);
            list.add(obj03);
            list.add(obj04);
            list.add(obj05);
            list.add(obj06);
            list.add(obj07);
            list.add(obj08);
            JsonArrayBuilder jab = MetricsUtil.dataversesByCategoryToJson(list);
            JsonArray jsonArray = jab.build();
            System.out.println(JsonUtil.prettyPrint(jsonArray));
            JsonObject jsonObject = jsonArray.getJsonObject(8);
            assertEquals("Department", jsonObject.getString("category"));
            assertEquals(7, jsonObject.getInt("count"));
        }

        @Test
        public void testDatasetsBySubjectToJson() {
            List<Object[]> list = new ArrayList<>();
            Object[] obj00 = { "Social Sciences", 24955l };
            Object[] obj01 = { "Medicine, Health and Life Sciences", 2262l };
            Object[] obj02 = { "Earth and Environmental Sciences", 1631l };
            Object[] obj03 = { "Agricultural Sciences", 1187l };
            Object[] obj04 = { "Other", 980l };
            Object[] obj05 = { "Computer and Information Science", 888l };
            Object[] obj06 = { "Arts and Humanities", 832l };
            Object[] obj07 = { "Astronomy and Astrophysics", 353l };
            Object[] obj08 = { "Business and Management", 346l };
            Object[] obj09 = { "Law", 220l };
            Object[] obj10 = { "Engineering", 203l };
            Object[] obj11 = { "Mathematical Sciences", 123l };
            Object[] obj12 = { "Chemistry", 116l };
            Object[] obj13 = { "Physics", 98l };
            list.add(obj00);
            list.add(obj01);
            list.add(obj02);
            list.add(obj03);
            list.add(obj04);
            list.add(obj05);
            list.add(obj06);
            list.add(obj07);
            list.add(obj08);
            list.add(obj09);
            list.add(obj10);
            list.add(obj11);
            list.add(obj12);
            list.add(obj13);
            JsonArrayBuilder jab = MetricsUtil.datasetsBySubjectToJson(list);
            JsonArray jsonArray = jab.build();
            System.out.println(JsonUtil.prettyPrint(jsonArray));
            JsonObject jsonObject = jsonArray.getJsonObject(13);
            assertEquals("Physics", jsonObject.getString("subject"));
            assertEquals(98, jsonObject.getInt("count"));
        }

        @Test
        public void testDataversesBySubjectToJson() {
            List<Object[]> list = new ArrayList<>();
            Object[] obj00 = { "Social Sciences", 24955l };
            Object[] obj01 = { "Medicine, Health and Life Sciences", 2262l };
            Object[] obj02 = { "Earth and Environmental Sciences", 1631l };
            Object[] obj03 = { "Agricultural Sciences", 1187l };
            Object[] obj04 = { "Other", 980l };
            Object[] obj05 = { "Computer and Information Science", 888l };
            Object[] obj06 = { "Arts and Humanities", 832l };
            Object[] obj07 = { "Astronomy and Astrophysics", 353l };
            Object[] obj08 = { "Business and Management", 346l };
            Object[] obj09 = { "Law", 220l };
            Object[] obj10 = { "Engineering", 203l };
            Object[] obj11 = { "Mathematical Sciences", 123l };
            Object[] obj12 = { "Chemistry", 116l };
            Object[] obj13 = { "Physics", 98l };
            list.add(obj00);
            list.add(obj01);
            list.add(obj02);
            list.add(obj03);
            list.add(obj04);
            list.add(obj05);
            list.add(obj06);
            list.add(obj07);
            list.add(obj08);
            list.add(obj09);
            list.add(obj10);
            list.add(obj11);
            list.add(obj12);
            list.add(obj13);
            JsonArrayBuilder jab = MetricsUtil.dataversesBySubjectToJson(list);
            JsonArray jsonArray = jab.build();
            System.out.println(JsonUtil.prettyPrint(jsonArray));
            JsonObject jsonObject = jsonArray.getJsonObject(13);
            assertEquals("Physics", jsonObject.getString("subject"));
            assertEquals(98, jsonObject.getInt("count"));
        }

        @Test
        void testSanitizeHappyPath() {
            assertEquals("2018-04", MetricsUtil.sanitizeYearMonthUserInput("2018-04"));
        }

        @Test
        void testSanitizeJunk() {
            assertThrows(Exception.class, () -> MetricsUtil.sanitizeYearMonthUserInput("junk"));
        }

        @Test
        void testSanitizeFullIso() {
            assertThrows(Exception.class, () -> MetricsUtil.sanitizeYearMonthUserInput("2018-01-01"));
        }

        @Test
        void testSanitizeYearMonthUserInputIsAfterCurrentDate() {
            assertThrows(Exception.class, () -> MetricsUtil.sanitizeYearMonthUserInput("2099-01"));
        }

        @Test
        public void testGetCurrentMonth() {
            String expectedMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String currentMonth = MetricsUtil.getCurrentMonth();
            assertEquals(expectedMonth, currentMonth);
        }

        // Create JsonArray, turn into string and back into array to confirm data
        // integrity
        @Test
        public void testStringToJsonArrayBuilder() {
            System.out.println("testStringToJsonArrayBuilder");
            List<Object[]> list = new ArrayList<>();
            Object[] obj00 = { "Social Sciences", 24955l };
            list.add(obj00);

            JsonArray jsonArrayBefore = MetricsUtil.datasetsBySubjectToJson(list).build();
            System.out.println(JsonUtil.prettyPrint(jsonArrayBefore));

            JsonArray jsonArrayAfter = MetricsUtil.stringToJsonArray(jsonArrayBefore.toString());
            System.out.println(JsonUtil.prettyPrint(jsonArrayAfter));

            assertEquals(jsonArrayBefore.getJsonObject(0).getString("subject"),
                    jsonArrayAfter.getJsonObject(0).getString("subject"));
        }

        // Create JsonObject, turn into string and back into array to confirm data
        // integrity
        @Test
        public void testStringToJsonObjectBuilder() {
            System.out.println("testStringToJsonObjectBuilder");

            JsonObject jsonObjBefore = Json.createObjectBuilder().add("Test", "result").build();
            System.out.println(JsonUtil.prettyPrint(jsonObjBefore));

            JsonObject jsonObjAfter = MetricsUtil.stringToJsonObject(jsonObjBefore.toString());
            System.out.println(JsonUtil.prettyPrint(jsonObjAfter));

            assertEquals(jsonObjBefore.getString("Test"), jsonObjAfter.getString("Test"));
        }

    }
    
    @ParameterizedTest
    @CsvSource(value = {
        "local,false,local",
        "remote,false,remote",
        "all,false,all",
        "NULL,false,local",
        "'',false,local",
        "abcd,true,NULL"
    }, nullValues = "NULL")
    void testValidateDataLocationStringType(String dataLocation, boolean isExceptionExpected, String expectedOutput) {
        if (isExceptionExpected)
            assertThrows(Exception.class, () -> MetricsUtil.validateDataLocationStringType(dataLocation));
        else
            assertEquals(expectedOutput, MetricsUtil.validateDataLocationStringType(dataLocation));
    }
}
