package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
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
        assertEquals(633607, jsonObject.getInt("running_total"));
        assertEquals(114233, jsonObject.getInt("Number of File Downloads"));
        assertEquals("May 2017", jsonObject.getString("Month"));
        assertEquals("Total File Downloads", jsonObject.getString("name"));
        assertEquals("2017-05", jsonObject.getString("month_sort"));
        assertEquals("May 2017: 114,233 downloads / total: 633,607", jsonObject.getString("display_name"));
    }

}
