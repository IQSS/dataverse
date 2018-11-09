package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseLocaleBean;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author jchengan
 */
public class DateUtil {

    private static final Logger logger = Logger.getLogger(DateUtil.class.getCanonicalName());


    public static String formatDate(Date dateToformat) {
        try
        {
            DataverseLocaleBean d = new DataverseLocaleBean();
            Locale bundle_locale = new Locale(d.getLocaleCode());

            String formattedDate = "";

            if(bundle_locale.getLanguage().contains("fr"))
            {
                DateFormat df = new SimpleDateFormat("dd MMM yyyy",bundle_locale);
                formattedDate = df.format(dateToformat);
            }
            else {
                DateFormat df = new SimpleDateFormat("MMM dd, yyyy", bundle_locale);
                formattedDate = df.format(dateToformat);
            }

            return formattedDate;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    public static String formatDate(String dateToformat) {

        try
        {
            DataverseLocaleBean d = new DataverseLocaleBean();
            Locale bundle_locale = new Locale(d.getLocaleCode());

            String formattedDate = "";

            DateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy");
            Date _date = inputFormat.parse(dateToformat);

            if(bundle_locale.getLanguage().contains("fr"))
            {
                DateFormat df = new SimpleDateFormat("dd MMM yyyy",bundle_locale);
                formattedDate = df.format(_date);
            }
            else {
                DateFormat df = new SimpleDateFormat("MMM dd, yyyy", bundle_locale);
                formattedDate = df.format(_date);
            }

            return formattedDate;
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
