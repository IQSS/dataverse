package edu.harvard.iq.dataverse.metrics;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MetricsUtilTest {

    private static final long COUNT = 42l;
    private List<DatasetsMetrics> datasetsMetrics;

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
    public void testDatasetsBySubjectToJson() {
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
    public void testDataversesBySubjectToJson() {
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
        JsonArrayBuilder jab = MetricsUtil.dataversesBySubjectToJson(list);
        JsonArray jsonArray = jab.build();
        System.out.println(JsonUtil.prettyPrint(jsonArray));
        JsonObject jsonObject = jsonArray.getJsonObject(13);
        assertEquals("Physics", jsonObject.getString("subject"));
        assertEquals(98, jsonObject.getInt("count"));
    }

    @Test
    public void testSanitizeHappyPath() throws Exception {
        assertEquals("2018-04", MetricsUtil.sanitizeYearMonthUserInput("2018-04"));
    }

    @Test(expected = Exception.class)
    public void testSanitizeJunk() throws Exception {
        MetricsUtil.sanitizeYearMonthUserInput("junk");
    }

    @Test(expected = Exception.class)
    public void testSanitizeFullIso() throws Exception {
        MetricsUtil.sanitizeYearMonthUserInput("2018-01-01");
    }

    //Create JsonArray, turn into string and back into array to confirm data integrity
    @Test
    public void testStringToJsonArrayBuilder() {
        System.out.println("testStringToJsonArrayBuilder");
        List<Object[]> list = new ArrayList<>();
        Object[] obj00 = {"Social Sciences", 24955l};
        list.add(obj00);

        JsonArray jsonArrayBefore = MetricsUtil.datasetsBySubjectToJson(list).build();
        System.out.println(JsonUtil.prettyPrint(jsonArrayBefore));

        JsonArray jsonArrayAfter = MetricsUtil.stringToJsonArrayBuilder(jsonArrayBefore.toString()).build();
        System.out.println(JsonUtil.prettyPrint(jsonArrayAfter));

        assertEquals(
                jsonArrayBefore.getJsonObject(0).getString("subject"),
                jsonArrayAfter.getJsonObject(0).getString("subject")
        );
    }

    //Create JsonObject, turn into string and back into array to confirm data integrity
    @Test
    public void testStringToJsonObjectBuilder() {
        System.out.println("testStringToJsonObjectBuilder");

        JsonObject jsonObjBefore = Json.createObjectBuilder().add("Test", "result").build();
        System.out.println(JsonUtil.prettyPrint(jsonObjBefore));

        JsonObject jsonObjAfter = MetricsUtil.stringToJsonObjectBuilder(jsonObjBefore.toString()).build();
        System.out.println(JsonUtil.prettyPrint(jsonObjAfter));

        assertEquals(
                jsonObjBefore.getString("Test"),
                jsonObjAfter.getString("Test")
        );
    }

    @Test
    public void shouldCountDatasetMetricsForYear() {
        // given
        List<DatasetsMetrics> metrics = allMetrics();

        // when
        List<DatasetsMetrics> result = MetricsUtil.countDatasetsPerYear(metrics);

        // then
        verifyMetricsSize(result, 3);
        verifyCountForYear(result, 2018.0, 8L);
        verifyCountForYear(result, 2019.0, 78L);
        verifyCountForYear(result, 2020.0, 17L);
    }

    @Test
    public void shouldFillDataSetsMetricsWithEmptyMonths() {
        // given
        List<DatasetsMetrics> metrics = allMetrics();

        // when
        List<DatasetsMetrics> result = MetricsUtil.fillMissingDatasetMonths(metrics, 2018);

        // then
        verifyMetricsSize(result, 12);
    }

    @Test
    public void verifyCountForYearMonth() {
        // given
        List<DatasetsMetrics> metrics = allMetrics();

        // when
        List<DatasetsMetrics> result = MetricsUtil.fillMissingDatasetMonths(metrics, 2020);

        // then
        verifyDatasetsCountForYearMonth(result, 11, 9);
        verifyDatasetsCountForYearMonth(result, 12, 8);
    }

    @Test
    public void shouldFillRestOfTheMonthWithZeroCount() {
        // given
        List<DatasetsMetrics> metrics = allMetrics();

        // when
        List<DatasetsMetrics> result = MetricsUtil.fillMissingDatasetMonths(metrics, 2020);

        // then
        verifyMissingYearMonthRangeCountAsZero(result, 2020, 1, 10);
    }

    private void verifyCountForYear(List<DatasetsMetrics> metrics, double year, long count) {
        long sum = metrics.stream().filter(dm -> dm.getYear() == year)
                .mapToLong(DatasetsMetrics::getCount)
                .sum();
        assertEquals(count, sum);
    }

    private void verifyDatasetsCountForYearMonth(List<DatasetsMetrics> result, int month, int datasetCount) {
        assertEquals((long) result.get(month - 1).getCount(), datasetCount);
    }

    private void verifyMissingYearMonthRangeCountAsZero(List<DatasetsMetrics> result, double year,
                                                        int fromMonth, int toMonth) {
        for (int month = fromMonth; month <= toMonth; month++) {
            final int filterByMonth = month;
            DatasetsMetrics metrics = result.stream()
                    .filter(dm -> dm.getYear() == year)
                    .filter(dm -> dm.getMonth() == filterByMonth)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Not found metrics for month: " + filterByMonth + " for year: " + year));
            assertEquals(Long.valueOf(0L), metrics.getCount());
        }
    }

    private void verifyMetricsSize(List<DatasetsMetrics> result, int size) {
        assertEquals(size, result.size());
    }

    private List<DatasetsMetrics> allMetrics() {
        return Lists.newArrayList(
                new DatasetsMetrics(2018.0, (double) 4, 7L),
                new DatasetsMetrics(2018.0, (double) 5, 1L),
                new DatasetsMetrics(2018.0, (double) 6, 0L),
                new DatasetsMetrics(2019.0, (double) 1, 78L),
                new DatasetsMetrics(2020.0, (double) 11, 9L),
                new DatasetsMetrics(2020.0, (double) 12, 8L)
        );
    }
}
