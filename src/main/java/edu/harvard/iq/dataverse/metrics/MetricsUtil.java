package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.Dataverse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class MetricsUtil {

    private static final Logger logger = Logger.getLogger(MetricsUtil.class.getCanonicalName());

    private final static String COUNT = "count";
    private final static String CATEGORY = "category";
    private final static String SUBJECT = "subject";
    public static String YEAR_AND_MONTH_PATTERN = "yyyy-MM";

    public static JsonObjectBuilder countToJson(long count) {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add(COUNT, count);
        return job;
    }

    public static JsonArrayBuilder dataversesByCategoryToJson(List<Object[]> listOfObjectArrays) {
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

    public static JsonArrayBuilder datasetsBySubjectToJson(List<Object[]> listOfObjectArrays) {
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

    /**
     *
     * @param userInput A year and month in YYYY-MM format.
     * @return A year and month in YYYY-MM format.
     */
    static String sanitizeYearMonthUserInput(String userInput) throws Exception {
        logger.fine("string from user to sanitize (hopefully YYYY-MM format): " + userInput);
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM")
                // To make the parser happy, we set the day of the month to the first of the month.
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter();
        LocalDate localDate = null;
        try {
            localDate = LocalDate.parse(userInput, dateTimeFormatter);
        } catch (DateTimeParseException ex) {
            throw new Exception("The expected format is YYYY-MM but an exception was thrown: " + ex.getLocalizedMessage());
        }
        String sanitized = localDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN));
        return sanitized;
    }

}
