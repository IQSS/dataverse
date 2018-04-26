package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MetricsUtilTest {

    @Test
    public void testDataversesByCategoryToJson() {
        List<Object[]> list = new ArrayList<>();
        Object[] obj00 = {"RESEARCH_PROJECTS", 791l};
        Object[] obj01 = {"RESEARCHERS", 745l};
        Object[] obj02 = {"UNCATEGORIZED", 565l};
        Object[] obj03 = {"ORGANIZATIONS_INSTITUTIONS", 250l};
        Object[] obj04 = {"JOURNALS", 106l};
        Object[] obj05 = {"RESEARCH_GROUP", 106l};
        Object[] obj06 = {"TEACHING_COURSES", 20l};
        Object[] obj07 = {"LABORATORY", 17l};
        Object[] obj08 = {"DEPARTMENT", 7l};
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
    public void testDownloadsToJson() {
        MetricsUtil metricsUtil = new MetricsUtil();
        Object[] obj01 = {new Timestamp(118, 3, 1, 0, 0, 0, 0), 1l, new BigDecimal("2607")};
        Object[] obj02 = {new Timestamp(118, 2, 1, 0, 0, 0, 0), 56l, new BigDecimal("2606")};
        Object[] obj03 = {new Timestamp(118, 1, 1, 0, 0, 0, 0), 34l, new BigDecimal("2550")};
        Object[] obj04 = {new Timestamp(118, 0, 1, 0, 0, 0, 0), 35l, new BigDecimal("2516")};
        Object[] obj05 = {new Timestamp(117, 11, 1, 0, 0, 0, 0), 32l, new BigDecimal("2481")};
        Object[] obj06 = {new Timestamp(117, 10, 1, 0, 0, 0, 0), 43l, new BigDecimal("2449")};
        Object[] obj07 = {new Timestamp(117, 9, 1, 0, 0, 0, 0), 37l, new BigDecimal("2406")};
        Object[] obj08 = {new Timestamp(117, 8, 1, 0, 0, 0, 0), 13l, new BigDecimal("2369")};
        Object[] obj09 = {new Timestamp(117, 7, 1, 0, 0, 0, 0), 58l, new BigDecimal("2356")};
        Object[] obj10 = {new Timestamp(117, 6, 1, 0, 0, 0, 0), 54l, new BigDecimal("2298")};
        Object[] obj11 = {new Timestamp(117, 5, 1, 0, 0, 0, 0), 42l, new BigDecimal("2244")};
        Object[] obj12 = {new Timestamp(117, 4, 1, 0, 0, 0, 0), 58l, new BigDecimal("2202")};
        List<Object[]> list = new ArrayList<>();
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
        JsonArrayBuilder result = MetricsUtil.downloadsToJson(list);
        JsonArray jsonArray = result.build();
        assertEquals(12, jsonArray.size());
        System.out.println(JsonUtil.prettyPrint(jsonArray));
        JsonObject jsonObject = jsonArray.getJsonObject(11);
        assertEquals(2202l, jsonObject.getJsonNumber("runningTotal").longValue());
        assertEquals(58, jsonObject.getInt("downloads"));
    }

    @Test
    public void testDataversesByMonthToJson() {
        Object[] obj01 = {new Timestamp(118, 3, 1, 0, 0, 0, 0), 1l, new BigDecimal("2607")};
        Object[] obj02 = {new Timestamp(118, 2, 1, 0, 0, 0, 0), 56l, new BigDecimal("2606")};
        Object[] obj03 = {new Timestamp(118, 1, 1, 0, 0, 0, 0), 34l, new BigDecimal("2550")};
        Object[] obj04 = {new Timestamp(118, 0, 1, 0, 0, 0, 0), 35l, new BigDecimal("2516")};
        Object[] obj05 = {new Timestamp(117, 11, 1, 0, 0, 0, 0), 32l, new BigDecimal("2481")};
        Object[] obj06 = {new Timestamp(117, 10, 1, 0, 0, 0, 0), 43l, new BigDecimal("2449")};
        Object[] obj07 = {new Timestamp(117, 9, 1, 0, 0, 0, 0), 37l, new BigDecimal("2406")};
        Object[] obj08 = {new Timestamp(117, 8, 1, 0, 0, 0, 0), 13l, new BigDecimal("2369")};
        Object[] obj09 = {new Timestamp(117, 7, 1, 0, 0, 0, 0), 58l, new BigDecimal("2356")};
        Object[] obj10 = {new Timestamp(117, 6, 1, 0, 0, 0, 0), 54l, new BigDecimal("2298")};
        Object[] obj11 = {new Timestamp(117, 5, 1, 0, 0, 0, 0), 42l, new BigDecimal("2244")};
        Object[] obj12 = {new Timestamp(117, 4, 1, 0, 0, 0, 0), 58l, new BigDecimal("2202")};
        List<Object[]> list = new ArrayList<>();
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
        JsonArrayBuilder result = MetricsUtil.dataversesByMonthToJson(list);
        JsonArray jsonArray = result.build();
        System.out.println(JsonUtil.prettyPrint(jsonArray));
        JsonObject jsonObject = jsonArray.getJsonObject(11);
        assertEquals("2017-05", jsonObject.getString("yearMonth"));
        assertEquals(2202l, jsonObject.getJsonNumber("runningTotal").longValue());
        assertEquals(58, jsonObject.getInt("newDataverses"));
    }

    @Test
    public void testDatasetsByMonthToJson() {
        Object[] obj01 = {new Timestamp(118, 3, 1, 0, 0, 0, 0), 10l, new BigDecimal("25219")};
        Object[] obj02 = {new Timestamp(118, 2, 1, 0, 0, 0, 0), 240l, new BigDecimal("25209")};
        Object[] obj03 = {new Timestamp(118, 1, 1, 0, 0, 0, 0), 302l, new BigDecimal("24969")};
        Object[] obj04 = {new Timestamp(118, 0, 1, 0, 0, 0, 0), 311l, new BigDecimal("24667")};
        Object[] obj05 = {new Timestamp(117, 11, 1, 0, 0, 0, 0), 188l, new BigDecimal("24356")};
        Object[] obj06 = {new Timestamp(117, 10, 1, 0, 0, 0, 0), 157l, new BigDecimal("24168")};
        Object[] obj07 = {new Timestamp(117, 9, 1, 0, 0, 0, 0), 219l, new BigDecimal("24011")};
        Object[] obj08 = {new Timestamp(117, 8, 1, 0, 0, 0, 0), 160l, new BigDecimal("23792")};
        Object[] obj09 = {new Timestamp(117, 7, 1, 0, 0, 0, 0), 318l, new BigDecimal("23632")};
        Object[] obj10 = {new Timestamp(117, 6, 1, 0, 0, 0, 0), 269l, new BigDecimal("23314")};
        Object[] obj11 = {new Timestamp(117, 5, 1, 0, 0, 0, 0), 268l, new BigDecimal("23045")};
        Object[] obj12 = {new Timestamp(117, 4, 1, 0, 0, 0, 0), 215l, new BigDecimal("22777")};
        List<Object[]> list = new ArrayList<>();
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
        JsonArrayBuilder result = MetricsUtil.datasetsByMonthToJson(list);
        JsonArray jsonArray = result.build();
        System.out.println(JsonUtil.prettyPrint(jsonArray));
        JsonObject jsonObject = jsonArray.getJsonObject(11);
        assertEquals(22777l, jsonObject.getJsonNumber("runningTotal").longValue());
        assertEquals(215, jsonObject.getInt("newDatasets"));
        assertEquals("2017-05", jsonObject.getString("yearMonth"));
    }

    @Test
    public void testdatasetsBySubjectToJson() {
        List<Object[]> list = new ArrayList<>();
        Object[] obj00 = {"Social Sciences", 24955l};
        Object[] obj01 = {"Medicine, Health and Life Sciences", 2262l};
        Object[] obj02 = {"Earth and Environmental Sciences", 1631l};
        Object[] obj03 = {"Agricultural Sciences", 1187l};
        Object[] obj04 = {"Other", 980l};
        Object[] obj05 = {"Computer and Information Science", 888l};
        Object[] obj06 = {"Arts and Humanities", 832l};
        Object[] obj07 = {"Astronomy and Astrophysics", 353l};
        Object[] obj08 = {"Business and Management", 346l};
        Object[] obj09 = {"Law", 220l};
        Object[] obj10 = {"Engineering", 203l};
        Object[] obj11 = {"Mathematical Sciences", 123l};
        Object[] obj12 = {"Chemistry", 116l};
        Object[] obj13 = {"Physics", 98l};
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
    public void testFilesByMonthToJson() {
        Object[] obj01 = {new Timestamp(118, 3, 1, 0, 0, 0, 0), 10l, new BigDecimal("25219")};
        Object[] obj02 = {new Timestamp(118, 2, 1, 0, 0, 0, 0), 240l, new BigDecimal("25209")};
        Object[] obj03 = {new Timestamp(118, 1, 1, 0, 0, 0, 0), 302l, new BigDecimal("24969")};
        Object[] obj04 = {new Timestamp(118, 0, 1, 0, 0, 0, 0), 311l, new BigDecimal("24667")};
        Object[] obj05 = {new Timestamp(117, 11, 1, 0, 0, 0, 0), 188l, new BigDecimal("24356")};
        Object[] obj06 = {new Timestamp(117, 10, 1, 0, 0, 0, 0), 157l, new BigDecimal("24168")};
        Object[] obj07 = {new Timestamp(117, 9, 1, 0, 0, 0, 0), 219l, new BigDecimal("24011")};
        Object[] obj08 = {new Timestamp(117, 8, 1, 0, 0, 0, 0), 160l, new BigDecimal("23792")};
        Object[] obj09 = {new Timestamp(117, 7, 1, 0, 0, 0, 0), 318l, new BigDecimal("23632")};
        Object[] obj10 = {new Timestamp(117, 6, 1, 0, 0, 0, 0), 269l, new BigDecimal("23314")};
        Object[] obj11 = {new Timestamp(117, 5, 1, 0, 0, 0, 0), 268l, new BigDecimal("23045")};
        Object[] obj12 = {new Timestamp(117, 4, 1, 0, 0, 0, 0), 215l, new BigDecimal("22777")};
        List<Object[]> list = new ArrayList<>();
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
        JsonArrayBuilder result = MetricsUtil.filesByMonthToJson(list);
        JsonArray jsonArray = result.build();
        System.out.println(JsonUtil.prettyPrint(jsonArray));
        JsonObject jsonObject = jsonArray.getJsonObject(11);
        assertEquals(22777l, jsonObject.getJsonNumber("runningTotal").longValue());
        assertEquals(215, jsonObject.getInt("newFiles"));
        assertEquals("2017-05", jsonObject.getString("yearMonth"));
    }

    @Test
    public void testFilesNowToJson() {
        long count = 42l;
        JsonObjectBuilder result = MetricsUtil.filesNowToJson(count);
        JsonObject jsonObject = result.build();
        System.out.println(JsonUtil.prettyPrint(jsonObject));
        assertEquals(42l, jsonObject.getInt("count"));
    }

}
