package edu.harvard.iq.dataverse.common;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author jchengan
 */
public class DateUtil {

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
