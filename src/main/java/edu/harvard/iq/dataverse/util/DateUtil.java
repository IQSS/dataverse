package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseLocaleBean;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author jchengan
 */
public class DateUtil {

    public static String formatDate(Date dateToformat) {
        String formattedDate;
        DateFormat dateFormatter;
        try{
            DataverseLocaleBean d = new DataverseLocaleBean();
            Locale currentLocale = new Locale(d.getLocaleCode());
            dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);
            formattedDate = dateFormatter.format(dateToformat);
            return formattedDate;
        }
        catch(Exception e) {
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
         try{
             DataverseLocaleBean d = new DataverseLocaleBean();
             Locale currentLocale = new Locale(d.getLocaleCode());

             //switched to SimpleDateFormat, since the Time zone value was tricky to get using getDateTimeInstance()
             /*
             dateFormatter = DateFormat.getDateTimeInstance(
                                                     DateFormat.DEFAULT,
                                                     DateFormat.DEFAULT,
                                                     currentLocale);
             formattedDate = dateFormatter.format(datetimeToformat);
              */

             //copied this format from UserNotification.java , getSendDate()
             formattedDate = new SimpleDateFormat("MMMM d, yyyy h:mm a z",currentLocale).format(datetimeToformat);
             return formattedDate;
         } catch (Exception e) {
             e.printStackTrace();
             return null;
         }
    }
}
