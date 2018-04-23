package edu.harvard.iq.dataverse.metrics;

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

    public static JsonArrayBuilder downloadsToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        if (listOfObjectArrays.size() < 1) {
            return jab;
        }
        Object[] runningTotalArray = listOfObjectArrays.get(0);
        long runningTotal = (long) runningTotalArray[1];
        logger.fine("runningTotal: " + runningTotal);
        String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
        // Skip first item in list, which contains the running total count.
        for (Object[] objectArray : listOfObjectArrays.subList(1, listOfObjectArrays.size())) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numDownloadsPerMonth = (long) objectArray[1];
            logger.fine("numDownloadsPerMonth: " + numDownloadsPerMonth);
            String numDownloadsPerMonthFriendly = NumberFormat.getNumberInstance(LOCALE).format(numDownloadsPerMonth);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " 2017";
            job.add("Month", monthYear);
            int monthNum = localDate.getMonthValue();
            job.add("monthNum", monthNum);
            String name = "Total File Downloads";
            job.add("name", name);
            job.add("Number of File Downloads", numDownloadsPerMonth);
            job.add("running_total", runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add("month_sort", monthSort);
            String displayName = monthYear + ": " + numDownloadsPerMonthFriendly + " downloads / total: " + runningTotalFriendly;
            job.add("display_name", displayName);
            jab.add(job);
        }
        return jab;
    }
}
