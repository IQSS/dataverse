package edu.harvard.iq.dataverse.metrics;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MetricsUtil {

    private static final Logger logger = Logger.getLogger(MetricsUtil.class.getCanonicalName());

    private final static String COUNT = "count";
    private final static String CATEGORY = "category";
    private final static String SUBJECT = "subject";
    public static String YEAR_AND_MONTH_PATTERN = "yyyy-MM";

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

    /**
     * @param userInput A year and month in YYYY-MM format.
     * @return A year and month in YYYY-MM format.
     * <p>
     * Note that along with sanitization, this checks that the inputted month is
     * not after the current one. This will need to be made more robust if we
     * start writing metrics for farther in the future (e.g. the current year)
     */
    public static String sanitizeYearMonthUserInput(String userInput) throws Exception {
        logger.fine("string from user to sanitize (hopefully YYYY-MM format): " + userInput);
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM")
                // To make the parser happy, we set the day of the month to the first of the month.
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter();
        LocalDate inputLocalDate = null;
        try {
            inputLocalDate = LocalDate.parse(userInput, dateTimeFormatter);
        } catch (DateTimeParseException ex) {
            throw new Exception("The expected format is YYYY-MM but an exception was thrown: " + ex.getLocalizedMessage());
        }

        LocalDate currentDate = (new Date()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (inputLocalDate.isAfter(currentDate)) {
            throw new Exception("The inputted date is set past the current month.");
        }

        String sanitized = inputLocalDate.format(DateTimeFormatter.ofPattern(YEAR_AND_MONTH_PATTERN));
        return sanitized;
    }

    public static String validateDataLocationStringType(String dataLocation) throws Exception {
        if (null == dataLocation || "".equals(dataLocation)) {
            dataLocation = DATA_LOCATION_LOCAL;
        }
        if (!(DATA_LOCATION_LOCAL.equals(dataLocation) || DATA_LOCATION_REMOTE.equals(dataLocation) || DATA_LOCATION_ALL.equals(dataLocation))) {
            throw new Exception("The inputted data location is not valid");
        }

        return dataLocation;
    }

    public static String getCurrentMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(MetricsUtil.YEAR_AND_MONTH_PATTERN));
    }

    //Responses need jsonObjectBuilder's to return correct json
    //Sadly this first requires creating a non-builder object and then populating a builder
    public static JsonObjectBuilder stringToJsonObjectBuilder(String str) {
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonObject jo = jsonReader.readObject();
        jsonReader.close();

        JsonObjectBuilder job = Json.createObjectBuilder();

        for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
            job.add(entry.getKey(), entry.getValue());
        }

        return job;
    }

    //Responses need jsonObjectBuilder's to return correct json
    //Sadly this first requires creating a non-builder object and then populating a builder
    public static JsonArrayBuilder stringToJsonArrayBuilder(String str) {
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonArray ja = jsonReader.readArray();
        jsonReader.close();

        JsonArrayBuilder jab = Json.createArrayBuilder();

        for (int i = 0; i < ja.size(); i++) {
            JsonObjectBuilder job = Json.createObjectBuilder();

            for (Map.Entry<String, JsonValue> entry : ja.getJsonObject(i).entrySet()) {
                job.add(entry.getKey(), entry.getValue());
            }

            jab.add(job);
        }

        return jab;
    }

    public static List<ChartMetrics> countMetricsPerYearAndFillMissingYears(List<ChartMetrics> metrics) {
        return fillMissingYearsForMetrics(countMetricsPerYear(metrics));
    }

    public static List<ChartMetrics> countMetricsPerYearAndFillMissingYearsDescending(List<ChartMetrics> metrics) {
        List<ChartMetrics> yearsDescending = fillMissingYearsForMetrics(countMetricsPerYear(metrics));
        yearsDescending.sort(Comparator.comparingLong(ChartMetrics::getYear).reversed());
        return yearsDescending;
    }

    static List<ChartMetrics> countMetricsPerYear(List<ChartMetrics> chartMetrics) {
        Map<Integer, Long> counts = new HashMap<>();

        chartMetrics.forEach(
                (metrics -> {
                    Long yearCount = counts.getOrDefault(metrics.getYear(), 0L);
                    counts.put(metrics.getYear(), metrics.getCount() + yearCount);
                }));

        return counts.entrySet().stream()
                .map(integerLongEntry -> new ChartMetrics((double) integerLongEntry.getKey(), integerLongEntry.getValue()))
                .sorted(Comparator.comparing(ChartMetrics::getYear))
                .collect(Collectors.toList());
    }

    static List<ChartMetrics> fillMissingMonthsForMetrics(List<ChartMetrics> chartMetrics, int year) {
        List<Integer> months = getListWithMonthsValues();

        List<ChartMetrics> filteredMetrics = chartMetrics.stream()
                .filter(metrics -> metrics.getYear() == year)
                .peek(metrics -> months.removeIf(month -> month == metrics.getMonth()))
                .collect(Collectors.toList());

        months.forEach(month -> filteredMetrics.add(new ChartMetrics((double) year, (double) month, 0L)));
        filteredMetrics.sort(Comparator.comparing(ChartMetrics::getMonth));
        return filteredMetrics;
    }

    static List<ChartMetrics> fillMissingYearsForMetrics(List<ChartMetrics> missingYearsMetrics) {
        int currentYear = LocalDate.now().getYear();
        int minYear = missingYearsMetrics.stream()
                .mapToInt(ChartMetrics::getYear)
                .min()
                .orElse(currentYear);

        List<Integer> years = getListWithYears(minYear, currentYear);
        List<ChartMetrics> filledYearsMetrics = new ArrayList<>();
        years.forEach(year -> filledYearsMetrics.add(new ChartMetrics((double) year,
                missingYearsMetrics.stream()
                        .filter(metric -> metric.getYear() == year)
                        .findFirst()
                        .map(ChartMetrics::getCount)
                        .orElse(0L)
        )));

        filledYearsMetrics.sort(Comparator.comparingLong(ChartMetrics::getYear));
        return filledYearsMetrics;
    }

    private static List<Integer> getListWithMonthsValues() {
        List<Integer> months = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            months.add(i);
        }

        return months;
    }

    private static List<Integer> getListWithYears(int fromYear, int toYear) {
        return IntStream.rangeClosed(fromYear, toYear).boxed().collect(Collectors.toList());
    }
}


