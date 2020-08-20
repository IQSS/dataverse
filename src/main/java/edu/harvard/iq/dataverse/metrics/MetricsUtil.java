package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.Dataverse;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.BadRequestException;

public class MetricsUtil {

    private static final Logger logger = Logger.getLogger(MetricsUtil.class.getCanonicalName());

    public final static String CONTENTTYPE = "contenttype";
    public final static String COUNT = "count";
    public final static String CATEGORY = "category";
    public final static String PID = "pid";
    public final static String SUBJECT = "subject";
    public final static String DATE = "date";
    public final static String SIZE = "size";

    public static String YEAR_AND_MONTH_PATTERN = "YYYY-MM";

    public static final String DATA_LOCATION_LOCAL = "local";
    public static final String DATA_LOCATION_REMOTE = "remote";
    public static final String DATA_LOCATION_ALL = "all";

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

    public static JsonArrayBuilder dataversesBySubjectToJson(List<Object[]> listOfObjectArrays) {
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
    
    public static JsonArray timeSeriesToJson(List<Object[]> results) {
        return timeSeriesToJson(results, false);
    }
    
    public static JsonArray timeSeriesToJson(List<Object[]> results, boolean isBigDecimal) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        long total = 0;
        String curDate = (String) results.get(0)[0];
        // Get a list of all the monthly dates from the start until now
        List<String> dates = getDatesFrom(curDate);
        int i = 0;
        // Create an entry for each date
        for (String date : dates) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add(MetricsUtil.DATE, date);
            // If there's a result for this date, add it's count to the total
            // and find the date of the next entry
            if (date.equals(curDate)) {
                if(isBigDecimal) {
                    total +=((BigDecimal) results.get(i)[1]).longValue();
                } else {
                    total += (long) results.get(i)[1];
                }
                i += 1;
                if (i < results.size()) {
                    curDate = (String) results.get(i)[0];
                }
            }
            // Then add the aggregate count
            job.add(MetricsUtil.COUNT, total);
            jab.add(job);
        }
        return jab.build();
    }
    
    public static JsonArray timeSeriesByTypeToJson(List<Object[]> results) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        Map<String, Long> totals = new HashMap<String, Long>();
        Map<String, Long> sizes =  new HashMap<String, Long>();
        String curDate = (String) results.get(0)[0];
        // Get a list of all the monthly dates from the start until now
        List<String> dates = getDatesFrom(curDate);
        int i = 0;
        // Create an entry for each date
        for (String date : dates) {

            // If there's are results for this date, add their counts to the totals
            // and find the date of the next entry(ies)
            while (date.equals(curDate) && i < results.size()) {
                String type = (String) results.get(i)[1];
                totals.put(type,  (totals.containsKey(type) ? totals.get(type) : 0) + (long) results.get(i)[2]);
                sizes.put(type,  (sizes.containsKey(type) ? sizes.get(type) : 0) + ((BigDecimal) results.get(i)[3]).longValue());
                i += 1;
                if (i < results.size()) {
                    curDate = (String) results.get(i)[0];
                }
            }
            // Then add the aggregate count and size for all types
            for(String type: totals.keySet()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add(MetricsUtil.DATE, date);
                job.add(CONTENTTYPE, type);
                job.add(COUNT, totals.get(type));
                job.add(SIZE, sizes.get(type));
                jab.add(job);
            }
        }
        return jab.build();
    }
    
    public static JsonArray timeSeriesByPIDToJson(List<Object[]> results) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        Map<String, Long> totals = new HashMap<String, Long>();
        String curDate = (String) results.get(0)[0];
        // Get a list of all the monthly dates from the start until now
        List<String> dates = getDatesFrom(curDate);
        int i = 0;
        // Create an entry for each date
        for (String date : dates) {
            // If there's are results for this date, add their counts to the totals
            // and find the date of the next entry(ies)
            while (date.equals(curDate) && i < results.size()) {
                String pid = (String) results.get(i)[1];
                totals.put(pid,  (totals.containsKey(pid) ? totals.get(pid) : 0) + (long) results.get(i)[2]);
                i += 1;
                if (i < results.size()) {
                    curDate = (String) results.get(i)[0];
                }
            }
            // Then add the aggregate count and size for all types
            for(String type: totals.keySet()) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add(MetricsUtil.DATE, date);
                job.add(PID, type);
                job.add(COUNT, totals.get(type));
                jab.add(job);
            }
        }
        return jab.build();
    }
    

    /**
     *
     * @param userInput A year and month in YYYY-MM format.
     * @return A year and month in YYYY-M     * Note that along with sanitization, this checks that the requested month is
     * not after the current one. This will need to be made more robust if we
     * start writing metrics for farther in the future (e.g. the current year) the current year)
     */
    public static String sanitizeYearMonthUserInput(String userInput) throws BadRequestException {
        logger.fine("string from user to sanitize (hopefully YYYY-MM format): " + userInput);
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern(YEAR_AND_MONTH_PATTERN)
                // To make the parser happy, we set the day of the month to the first of the month.
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter();
        LocalDate inputLocalDate = null;
        try {
            inputLocalDate = LocalDate.parse(userInput, dateTimeFormatter);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("The expected format is YYYY-MM but an exception was thrown: " + ex.getLocalizedMessage());
        }

        LocalDate currentDate = LocalDate.now();

        if (inputLocalDate.isAfter(currentDate)) {
            throw new BadRequestException("The requested date is set past the current month.");
        }

        String sanitized = inputLocalDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN));
        return sanitized;
    }

    public static String validateDataLocationStringType(String dataLocation) throws BadRequestException {
        if (null == dataLocation || "".equals(dataLocation)) {
            dataLocation = DATA_LOCATION_LOCAL;
        }
        if (!(DATA_LOCATION_LOCAL.equals(dataLocation) || DATA_LOCATION_REMOTE.equals(dataLocation) || DATA_LOCATION_ALL.equals(dataLocation))) {
            throw new BadRequestException("Data location must be 'local', 'remote', or 'all'");
        }

        return dataLocation;
    }

    public static String getCurrentMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(MetricsUtil.YEAR_AND_MONTH_PATTERN));
    }

    public static JsonObject stringToJsonObject(String str) {
        if (str == null) {
            return null;
        }
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonObject jo = jsonReader.readObject();
        jsonReader.close();

        return jo;
    }

    public static JsonArray stringToJsonArray(String str) {
        if (str == null) {
            return null;
        }
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonArray ja = jsonReader.readArray();
        jsonReader.close();

        return ja;
    }

    public static List<String> getDatesFrom(String startMonth) {
        List<String> dates = new ArrayList<String>();
        LocalDate next = LocalDate.parse(startMonth+ "-01").plusMonths(1);
        dates.add(startMonth);
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN);
        while (next.isBefore(LocalDate.now())) {
            dates.add(next.format(monthFormat));
            next = next.plusMonths(1);
        }
        return dates;
    }
}
