package edu.harvard.iq.dataverse.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author jchengan
 */
public class DateUtil {

    public static String YEAR_PATTERN = "yyyy";
    public static String YEAR_DASH_MONTH_PATTERN = "yyyy-MM";
    public static String YEAR_DASH_MONTH_DASH_DAY_PATTERN = "yyyy-MM-dd";

    public static String formatDate(Date dateToformat) {
        String formattedDate;
        DateFormat dateFormatter;
        try {
            Locale currentLocale = BundleUtil.getCurrentLocale();
            dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);
            formattedDate = dateFormatter.format(dateToformat);
            return formattedDate;
        } catch(Exception e) {
            return null;
        }
    }

    public static String formatDate(String dateToformat, String format) {
        String formattedDate = "";
        DateFormat inputFormat = new SimpleDateFormat(format);
        Date _date = null;
        try {
            _date = inputFormat.parse(dateToformat);
            formattedDate = formatDate(_date);
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatDate(Timestamp datetimeToformat) {
        String formattedDate;
        DateFormat dateFormatter;
        try {
             Locale currentLocale = BundleUtil.getCurrentLocale();
             dateFormatter = DateFormat.getDateTimeInstance(
                                                     DateFormat.DEFAULT,
                                                     DateFormat.LONG,
                                                     currentLocale);
             formattedDate = dateFormatter.format(datetimeToformat);

             return formattedDate;
         } catch (Exception e) {
             e.printStackTrace();
             return null;
         }
    }

    public static Date parseDate(String dateString) {
        SimpleDateFormat sdf;
        Date date;

        // YYYY-MM-DD
        date = parseDate(dateString, YEAR_DASH_MONTH_DASH_DAY_PATTERN);
        if (date != null) {
            return date;
        }

        // YYYY-MM
        date = parseDate(dateString, YEAR_DASH_MONTH_PATTERN);
        if (date != null) {
            return date;
        }

        // YYYT
        date = parseDate(dateString, YEAR_PATTERN);
        return date;
        
    }

    public static Date parseDate(String dateString, String format) {
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = sdf.parse(dateString);
            return date;
        } catch (ParseException ex) {
            // ignore
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

}
