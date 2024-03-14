package edu.harvard.iq.dataverse.common;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * @author jchengan
 */
public class DateUtil {
    private final static DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendPattern("['-'MM['-'dd['T'HH':'mm':'ss[.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]]]][OOOO][O][z][XXXXX][XXXX]['['VV']']")
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();

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

    /**
     * Parse a date string with one of the following formats:
     * <li>yyyy</li>
     * <li>yyyy-MM</li>
     * <li>yyyy-MM-dd</li>
     * <li>yyyy-MM-ddTHH:mm:ss</li>
     * <li>yyyy-MM-ddTHH:mm:ss.SSS</li>
     * Optionally with up to nanoseconds precision and zone/offset info.
     * Missing components are set to the start of day of the 1st of January with system default zoneId.
     *
     * @throws java.time.format.DateTimeParseException for invalid formats
     */
    public static Date parseDateTimeFormatAsDate(String date) {
        Instant parsedDate = parseDateTimeFormat(date, DataverseClock.zoneId);
        if (parsedDate != null) {
            return Date.from(parsedDate);
        }
        return null;
    }

    /**
     * Parse a date string with one of the following formats:
     * <li>yyyy</li>
     * <li>yyyy-MM</li>
     * <li>yyyy-MM-dd</li>
     * <li>yyyy-MM-ddTHH:mm:ss</li>
     * <li>yyyy-MM-ddTHH:mm:ss.SSS</li>
     * Optionally with up to nanoseconds precision and zone/offset info.
     * Missing components are set to the start of day of the 1st of January with system default zoneId.
     *
     * @throws java.time.format.DateTimeParseException for invalid formats
     */
    public static Instant parseDateTimeFormat(String date) {
        return parseDateTimeFormat(date, DataverseClock.zoneId);
    }

    /**
     * Parse a date string with one of the following formats:
     * <li>yyyy</li>
     * <li>yyyy-MM</li>
     * <li>yyyy-MM-dd</li>
     * <li>yyyy-MM-ddTHH:mm:ss</li>
     * <li>yyyy-MM-ddTHH:mm:ss.SSS</li>
     * Optionally with up to nanoseconds precision and zone/offset info.
     * Missing components are set to the start of day of the 1st of January with system default zoneId.
     *
     * @throws java.time.format.DateTimeParseException for invalid formats
     */
    public static Instant parseDateTimeFormat(String date, ZoneId defaultZoneId) {
        if (date != null && !date.trim().isEmpty()) {
            TemporalAccessor parsedDate = LOCAL_DATE_TIME_FORMATTER
                    .parseBest(date, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
            if (parsedDate instanceof ZonedDateTime) {
                return ((ZonedDateTime) parsedDate).toInstant();
            } else if (parsedDate instanceof LocalDateTime) {
                return ((LocalDateTime) parsedDate).atZone(defaultZoneId).toInstant();
            } else {
                return ((LocalDate) parsedDate).atStartOfDay(defaultZoneId).toInstant();
            }
        }
        return null;
    }

    /**
     * Formats an instant to 'yyyy-MM-dd' using default zone.
     */
    public static String formatISOLocalDate(Instant instant) {
        if (instant != null) {
            return DateTimeFormatter.ISO_LOCAL_DATE.withZone(DataverseClock.zoneId).format(instant);
        }
        return null;
    }
}
