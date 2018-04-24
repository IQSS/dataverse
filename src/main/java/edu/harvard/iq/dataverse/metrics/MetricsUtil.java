package edu.harvard.iq.dataverse.metrics;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class MetricsUtil {

    private static final Logger logger = Logger.getLogger(MetricsUtil.class.getCanonicalName());

    // TODO: Should this really be hard-coded to "Locale.US"?
    private final static Locale LOCALE = Locale.US;
    private final static String MONTH = "Month";
    private final static String MONTH_NUM = "monthNum";
    private final static String NAME = "name";
    private final static String RUNNING_TOTAL = "running_total";
    private final static String MONTH_SORT = "month_sort";
    private final static String DISPLAY_NAME = "display_name";
    private final static String VALUE = "value";
    private final static String WEIGHT = "weight";
    private final static String TYPE = "type";
    private final static String LABEL = "label";

    static JsonArrayBuilder dataversesByCategoryToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        float totalDataversesLong = 0;
        for (Object[] objectArray : listOfObjectArrays) {
            long value = (long) objectArray[1];
            totalDataversesLong = totalDataversesLong + value;
        }
        for (Object[] arrayOfObjects : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            String categoryName = (String) arrayOfObjects[0];
            long categoryCount = (long) arrayOfObjects[1];
            String percentage = String.format("%.1f", categoryCount * 100f / totalDataversesLong) + "%";
            // TODO: Spaces in JSON keys is weird but it's what miniverse does.
            job.add("dataverse count", categoryCount);
            job.add(NAME, categoryName + " (" + percentage + ")");
            job.add("percent_label", percentage);
            jab.add(job);
        }
        return jab;
    }

    public static JsonArrayBuilder downloadsToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        if (listOfObjectArrays.size() < 1) {
            return jab;
        }
        // Skip first item in list, which contains a total;
        for (Object[] objectArray : listOfObjectArrays.subList(1, listOfObjectArrays.size())) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numDownloadsPerMonth = (long) objectArray[1];
            logger.fine("numDownloadsPerMonth: " + numDownloadsPerMonth);
            String numDownloadsPerMonthFriendly = NumberFormat.getNumberInstance(LOCALE).format(numDownloadsPerMonth);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " " + localDate.getYear();
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total File Downloads";
            job.add(NAME, name);
            job.add("Number of File Downloads", numDownloadsPerMonth);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            job.add(RUNNING_TOTAL, runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String displayName = monthYear + ": " + numDownloadsPerMonthFriendly + " downloads / total: " + runningTotalFriendly;
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder filesByMonthToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numNewFiles = (long) objectArray[1];
            logger.fine("numNewFiles: " + numNewFiles);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            String numNewFilesFriendly = NumberFormat.getNumberInstance(LOCALE).format(numNewFiles);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " " + localDate.getYear();
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total Files Added";
            job.add(NAME, name);
            job.add("Number of Files", numNewFiles);
            job.add("running_total", runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            String displayName = monthYear + ": " + numNewFilesFriendly + " added / total: " + runningTotalFriendly;
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder dataversesByMonthToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numNewDataverses = (long) objectArray[1];
            logger.fine("numNewDataverses: " + numNewDataverses);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            String numNewDataversesFriendly = NumberFormat.getNumberInstance(LOCALE).format(numNewDataverses);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " " + localDate.getYear();
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total Dataverses";
            job.add(NAME, name);
            job.add("Number of Dataverses", numNewDataverses);
            job.add("running_total", runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            // TODO: For consistency shouldn't runningTotalFriendly be included in display_name?
            // TODO: For consistency shouldn't it be "new" instead of "New" in display_name?
            String displayName = monthYear + ": " + numNewDataversesFriendly + " New Dataverses";
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder datasetsByMonthToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numNewDatasets = (long) objectArray[1];
            logger.fine("numNewDatasets: " + numNewDatasets);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            String numNewDatasetsFriendly = NumberFormat.getNumberInstance(LOCALE).format(numNewDatasets);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " " + localDate.getYear();
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total Datasets";
            job.add(NAME, name);
            job.add("Number of Datasets", numNewDatasets);
            job.add("running_total", runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            String displayName = monthYear + ": " + numNewDatasetsFriendly + " new Datasets; Total of " + runningTotalFriendly;
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder datasetsBySubjectToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        float totalDatasets = 0;
        for (Object[] objectArray : listOfObjectArrays) {
            long value = (long) objectArray[1];
            totalDatasets = totalDatasets + value;
        }
        for (Object[] objectArray : listOfObjectArrays) {
            String type = (String) objectArray[0];
            long value = (long) objectArray[1];
            // FIXME: Too much precision. Should be "0.006", for example.
            double weight = value / totalDatasets;
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add(VALUE, value);
            job.add(WEIGHT, weight);
            job.add("totalDatasets", totalDatasets);
            job.add(TYPE, type);
            String percentage = String.format("%.1f", value * 100f / totalDatasets) + "%";
            job.add(LABEL, type + " (" + percentage + ")");
            jab.add(job);
        }
        return jab;
    }

}
