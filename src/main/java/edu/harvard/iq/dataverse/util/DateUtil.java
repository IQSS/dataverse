package edu.harvard.iq.dataverse.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jchengan
 */
public class DateUtil {

    private static final Logger logger = Logger.getLogger(DateUtil.class.getCanonicalName());

    public static String formatDate(Date dateToformat) {
        String formattedDate;
        DateFormat dateFormatter;
        try {
            Locale currentLocale = BundleUtil.getCurrentLocale();
            dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);
            formattedDate = dateFormatter.format(dateToformat);
            logger.log(Level.SEVERE, "******** JUAN ******: Fecha Formateada en formatDate: {0}", formattedDate);
            logger.log(Level.SEVERE, "******** JUAN ******: Locale en formatDate: {0}", currentLocale.getDisplayName());

            return formattedDate;
        } catch(Exception e) {
            return null;
        }
    }

    public static String formatDate(String dateToformat, String format) {

        
        
        String formattedDate = "";
        DateFormat inputFormat = new SimpleDateFormat(format);
        Date _date = null;
        logger.log(Level.SEVERE, "******** JUAN ******: Fecha A formatear en formatDate instancia 2: {0}", dateToformat);
        logger.log(Level.SEVERE, "******** JUAN ******: Formato en formatDate instancia 2: {0}", format);
        try {
            Locale currentLocale = BundleUtil.getCurrentLocale();
            DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);
            
            
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
