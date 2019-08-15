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

    public static String YEAR_DASH_MONTH_PATTERN = "yyyy-MM";

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

}
