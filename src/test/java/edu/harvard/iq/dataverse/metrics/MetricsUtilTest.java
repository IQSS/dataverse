package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MetricsUtilTest {

    @Test
    public void testDownloadsToJson() {
        MetricsUtil metricsUtil = new MetricsUtil();
        Object[] obj00 = {null, 633607l};
        Object[] obj01 = {new Timestamp(118, 3, 1, 0, 0, 0, 0), 2864l};
        Object[] obj02 = {new Timestamp(118, 2, 1, 0, 0, 0, 0), 60244l};
        Object[] obj03 = {new Timestamp(118, 1, 1, 0, 0, 0, 0), 84614l};
        Object[] obj04 = {new Timestamp(118, 0, 1, 0, 0, 0, 0), 70225l};
        Object[] obj05 = {new Timestamp(117, 11, 1, 0, 0, 0, 0), 50316l};
        Object[] obj06 = {new Timestamp(117, 10, 1, 0, 0, 0, 0), 87894l};
        Object[] obj07 = {new Timestamp(117, 9, 1, 0, 0, 0, 0), 71341l};
        Object[] obj08 = {new Timestamp(117, 8, 1, 0, 0, 0, 0), 62020l};
        Object[] obj09 = {new Timestamp(117, 7, 1, 0, 0, 0, 0), 48036l};
        Object[] obj10 = {new Timestamp(117, 6, 1, 0, 0, 0, 0), 51425l};
        Object[] obj11 = {new Timestamp(117, 5, 1, 0, 0, 0, 0), 50886l};
        Object[] obj12 = {new Timestamp(117, 4, 1, 0, 0, 0, 0), 114233l};
        List<Object[]> onlyTotalInList = new ArrayList<>();
        JsonArrayBuilder onlyTotalResult = MetricsUtil.downloadsToJson(onlyTotalInList);
        JsonArray onlyTotal = onlyTotalResult.build();
        System.out.println(JsonUtil.prettyPrint(onlyTotal));
        assertEquals(true, onlyTotal.isEmpty());
        List<Object[]> list = new ArrayList<>();
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
        JsonArrayBuilder result = MetricsUtil.downloadsToJson(list);
        JsonArray jsonArray = result.build();
        System.out.println(JsonUtil.prettyPrint(jsonArray));
        JsonObject jsonObject = jsonArray.getJsonObject(11);
        assertEquals(5, jsonObject.getInt("monthNum"));
        assertEquals(9223372036854775807l, jsonObject.getJsonNumber("running_total").longValue());
        assertEquals(114233, jsonObject.getInt("Number of File Downloads"));
        assertEquals("May 2017", jsonObject.getString("Month"));
        assertEquals("Total File Downloads", jsonObject.getString("name"));
        assertEquals("2017-05", jsonObject.getString("month_sort"));
        assertEquals("May 2017: 114,233 downloads / total: 9,223,372,036,854,775,807", jsonObject.getString("display_name"));
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
        assertEquals(5, jsonObject.getInt("monthNum"));
        assertEquals(22777l, jsonObject.getJsonNumber("running_total").longValue());
        assertEquals(215, jsonObject.getInt("Number of Datasets"));
        assertEquals("May 2017", jsonObject.getString("Month"));
        assertEquals("Total Datasets", jsonObject.getString("name"));
        assertEquals("2017-05", jsonObject.getString("month_sort"));
        assertEquals("May 2017: 215 new Datasets; Total of 22,777", jsonObject.getString("display_name"));
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
        assertEquals("Physics", jsonObject.getString("type"));
        assertEquals("Physics (0.3%)", jsonObject.getString("label"));
        assertEquals(98, jsonObject.getInt("value"));
        assertEquals(0.00286599993705749, jsonObject.getJsonNumber("weight").doubleValue(), 1000);
    }
}
