package edu.harvard.iq.dataverse.api;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;

public class Util {

    static final Set<String> VALID_BOOLEAN_VALUES;
    static final Set<String> BOOLEAN_TRUE_VALUES;

    static {
        BOOLEAN_TRUE_VALUES = new TreeSet<>();
        BOOLEAN_TRUE_VALUES.add("true");
        BOOLEAN_TRUE_VALUES.add("yes");
        BOOLEAN_TRUE_VALUES.add("1");

        VALID_BOOLEAN_VALUES = new TreeSet<>();
        VALID_BOOLEAN_VALUES.addAll(BOOLEAN_TRUE_VALUES );
        VALID_BOOLEAN_VALUES.add("no");
        VALID_BOOLEAN_VALUES.add("false");
        VALID_BOOLEAN_VALUES.add("0");
    }

    static JsonArray asJsonArray( String str ) {
        try ( JsonReader rdr = Json.createReader(new StringReader(str)) ) {
            return rdr.readArray();
        }
    }

    static boolean isBoolean( String s ) {
        return VALID_BOOLEAN_VALUES.contains(s.toLowerCase());
    }

    static boolean isTrue( String s ) {
        return BOOLEAN_TRUE_VALUES.contains(s.toLowerCase());
    }

    /**
     * @param date The Date object to convert.
     * @return A ISO 8601 date/time with UTC time zone (i.e.
     * 2015-01-23T19:51:50Z) or null.
     *
     * <p>
     *
     * "Law #1: Use ISO-8601 for your dates"
     * http://apiux.com/2013/03/20/5-laws-api-dates-and-times/
     *
     * <p>
     * "All timestamps are returned in ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ"
     * https://developer.github.com/v3/#schema
     *
     * <p>
     *
     * "Law #4: Return it in UTC"
     * http://apiux.com/2013/03/20/5-laws-api-dates-and-times/
     *
     */
    private static final  String DATE_TIME_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";

    private static final ThreadLocal<SimpleDateFormat> DATETIME_FORMAT_TL = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue()
        {
            SimpleDateFormat format =  new SimpleDateFormat(DATE_TIME_FORMAT_STRING);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_TL = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue()
        {
            SimpleDateFormat format =  new SimpleDateFormat(DATE_FORMAT_STRING);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };

    /**
     * Note: SimpleDateFormat is not thread-safe! Never retain the format returned by this method in a field.
     * @return The standard API format for date-and-time.
     */
    public static SimpleDateFormat getDateTimeFormat() {
        return DATETIME_FORMAT_TL.get();
    }

    /**
     * Note: SimpleDateFormat is not thread-safe! Never retain the format returned by this method in a field.
     * @return The standard API format for dates.
     */
    public static SimpleDateFormat getDateFormat() {
        return DATE_FORMAT_TL.get();
    }

    /**
     * Takes in a list of strings and returns a list stripped of nulls, empty strings and duplicates
     * @param stringsToCheck
     * @return
     */

    public static List<String> removeDuplicatesNullsEmptyStrings(List<String> stringsToCheck){

        if (stringsToCheck == null){
            throw new NullPointerException("stringsToCheck cannot be null");
        }

        return stringsToCheck.stream()
                        .filter(p -> p != null)         // no nulls
                        .map(String :: trim)            // strip strings
                        .filter(p -> p.length() > 0 )   // no empty strings
                        .distinct()                     // distinct
                        .collect(Collectors.toList());
    }

}
