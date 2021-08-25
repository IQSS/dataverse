package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Class containing date utility methods intended to
 * use on xhtml templates. Methods are exposed
 * in dataverse taglib
 * 
 * @see dataverse.taglib.xml
 */
public class DateUIUtils {

    /**
     * Formats {@link Date} into date only string
     * using MEDIUM format and current user locale
     */
    public static String formatDate(Date dateToformat) {
        if (dateToformat == null) {
            return StringUtils.EMPTY;
        }

        Locale currentLocale = BundleUtil.getCurrentLocale();
        DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale);
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatter.format(dateToformat);
    }

    /**
     * Formats {@link Date} into date and time string
     * using MEDIUM format for date and LONG format for
     * time and current user locale
     */
    public static String formatDateTime(Date datetimeToformat) {
        if (datetimeToformat == null) {
            return StringUtils.EMPTY;
        }

        Locale currentLocale = BundleUtil.getCurrentLocale();
        DateFormat dateFormatter = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.LONG,
                currentLocale);
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatter.format(datetimeToformat);
    }
}
