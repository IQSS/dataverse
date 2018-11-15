package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseLocaleBean;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    public static String formatDate(String dateToformat){
        String formattedDate = "";
        DateFormat inputFormat = DateFormat.getDateInstance(DateFormat.DEFAULT);
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
}
