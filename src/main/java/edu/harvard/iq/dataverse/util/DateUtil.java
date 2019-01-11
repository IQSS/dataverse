package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.locale.DataverseLocaleBean;
import org.apache.commons.lang.StringUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author jchengan
 */
public class DateUtil {

    public static String formatDate(Date dateToformat) {
        String formattedDate;
        DateFormat dateFormatter;
        try {
            DataverseLocaleBean d = new DataverseLocaleBean();
            Locale currentLocale = new Locale(d.getLocaleCode());
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
             DataverseLocaleBean d = new DataverseLocaleBean();
             Locale currentLocale = new Locale(d.getLocaleCode());
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

    public static LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime())
                .atZone(DataverseClock.zoneId)
                .toLocalDateTime();
    }

    public static Date convertToDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return java.util.Date.from(date.atZone(DataverseClock.zoneId).toInstant());
    }

    public static String formatDateToYMD_HM(Date date, SimpleDateFormat format) {
        return date == null ? StringUtils.EMPTY : format.format(date);
    }
}
