package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.Dataverse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    /**
     * 2018-04, for example
     */
    private final static String YEAR_AND_MONTH_NUM = "yearMonth";
    private final static String RUNNING_TOTAL = "runningTotal";
    private final static String COUNT = "count";
    private final static String CATEGORY = "category";
    private final static String SUBJECT = "subject";
    private final static String DOWNLOADS = "downloads";
    private final static String NEW_DATAVERSES = "newDataverses";
    private final static String NEW_DATASETS = "newDatasets";
    private final static String NEW_FILES = "newFiles";
    private final static String YEAR_AND_MONTH_PATTERN = "yyyy-MM";

    static JsonArrayBuilder dataversesByCategoryToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] arrayOfObjects : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            String categoryNameUppercase = (String) arrayOfObjects[0];
            Dataverse dataverse = new Dataverse();
            dataverse.setDataverseType(Dataverse.DataverseType.valueOf(categoryNameUppercase));
            String categoryNameFriendly = dataverse.getFriendlyCategoryName();
            long categoryCount = (long) arrayOfObjects[1];
            job.add(CATEGORY, categoryNameFriendly);
            job.add(COUNT, categoryCount);
            jab.add(job);
        }
        return jab;
    }

    public static JsonArrayBuilder downloadsToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        if (listOfObjectArrays.size() < 1) {
            return jab;
        }
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long downloads = (long) objectArray[1];
            logger.fine("downloads: " + downloads);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            job.add(YEAR_AND_MONTH_NUM, localDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN)));
            job.add(DOWNLOADS, downloads);
            job.add(RUNNING_TOTAL, runningTotal);
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
            long newFiles = (long) objectArray[1];
            logger.fine("numNewFiles: " + newFiles);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            job.add(YEAR_AND_MONTH_NUM, localDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN)));
            job.add(NEW_FILES, newFiles);
            job.add(RUNNING_TOTAL, runningTotal);
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
            long newDataverses = (long) objectArray[1];
            logger.fine("numNewDataverses: " + newDataverses);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            job.add(YEAR_AND_MONTH_NUM, localDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN)));
            job.add(NEW_DATAVERSES, newDataverses);
            job.add(RUNNING_TOTAL, runningTotal);
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
            long newDatasets = (long) objectArray[1];
            logger.fine("numNewDatasets: " + newDatasets);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            job.add(YEAR_AND_MONTH_NUM, localDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN)));
            job.add(NEW_DATASETS, newDatasets);
            job.add(RUNNING_TOTAL, runningTotal);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder datasetsBySubjectToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            String subject = (String) objectArray[0];
            long count = (long) objectArray[1];
            job.add(SUBJECT, subject);
            job.add(COUNT, count);
            jab.add(job);
        }
        return jab;
    }

}
