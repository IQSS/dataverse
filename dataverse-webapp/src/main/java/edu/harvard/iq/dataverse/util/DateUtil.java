package edu.harvard.iq.dataverse.util;

import org.apache.commons.lang.StringUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * @author jchengan
 */
public class DateUtil {

    public static String formatDate(Date dateToformat) {
        String formattedDate;
        DateFormat dateFormatter;
        try {
            Locale currentLocale = BundleUtil.getCurrentLocale();
            dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);
            formattedDate = dateFormatter.format(dateToformat);
            return formattedDate;
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatDate(String dateToformat, String format, Locale formatLocale) {
        String formattedDate = "";
        DateFormat inputFormat = new SimpleDateFormat(format, formatLocale);
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

    public static Date convertToDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return java.util.Date.from(date.atZone(DataverseClock.zoneId).toInstant());
    }

    public static String formatDate(Date date, SimpleDateFormat format) {

        return date == null ? StringUtils.EMPTY : format.format(date);
    }

    public static DateTimeFormatter retrieveISOFormatter(ZoneId zoneId) {

        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                .withZone(zoneId);
    }
}
