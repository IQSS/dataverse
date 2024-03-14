package edu.harvard.iq.dataverse.common;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateUtilTest {

    private static final long TEST_DATE = 1563871120499L;

    @Test
    public void shouldCorrectlyFormatDate() {
        //given
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date testDate = Date.from(Instant.ofEpochSecond(1547111385L));
        //when
        String formattedDate = DateUtil.formatDate(testDate, format);
        //then
        assertEquals("2019-01-10 09:09", formattedDate);
    }

    @Test
    public void shouldCorrectlyReturnEmptyString() {
        //given
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        //when
        String formattedDate = DateUtil.formatDate(null, format);
        //then
        assertEquals(StringUtils.EMPTY, formattedDate);
    }

    @Test
    public void shouldCorrectlyFormatDate_forUTC() {
        //given
        Date testDate = Date.from(Instant.ofEpochMilli(TEST_DATE));

        //when
        DateTimeFormatter isoFormatter = DateUtil.retrieveISOFormatter(ZoneId.of("UTC"));
        String formattedDate = isoFormatter.format(testDate.toInstant());

        //then
        assertEquals("2019-07-23T08:38:40Z", formattedDate);

    }

    @Test
    public void shouldCorrectlyFormatDate_forOffsetPlusTwo() {
        //given
        Date testDate = Date.from(Instant.ofEpochMilli(TEST_DATE));

        //when
        DateTimeFormatter isoFormatter = DateUtil.retrieveISOFormatter(ZoneId.of("CET"));
        String formattedDate = isoFormatter.format(testDate.toInstant());

        //then
        assertEquals("2019-07-23T10:38:40+02:00", formattedDate);

    }

    @ParameterizedTest
    @CsvSource({
            "2024,                                  2024-01-01T00:00:00Z",
            "2024-04,                               2024-04-01T00:00:00Z",
            "2024-04-02,                            2024-04-02T00:00:00Z",
            "2024-04-02T03:15:45,                   2024-04-02T03:15:45Z",
            "2024-04-02T03:15:45.3,                 2024-04-02T03:15:45.300Z",
            "2024-04-02T03:15:45.345,               2024-04-02T03:15:45.345Z",
            "2024-04-02T03:15:45.345678,            2024-04-02T03:15:45.345678Z",
            "2024-04-02T03:15:45.345678900,         2024-04-02T03:15:45.345678900Z",
            "2024-04-02T03:15:45[Australia/Sydney], 2024-04-01T16:15:45Z",
            "2024-04-02T03:15:45+11:00,             2024-04-01T16:15:45Z",
            "2024-04-02T03:15:45AEDT,               2024-04-01T16:15:45Z",
    })
    public void parseDateTimeFormat(String dateFormat, String expectedUTCString) {
        //when
        Instant parsed = DateUtil.parseDateTimeFormat(dateFormat, ZoneId.of("UTC"));

        //then
        assertEquals(expectedUTCString, DateTimeFormatter.ISO_INSTANT.format(parsed));
    }

    @Test
    public void parseDateTimeFormat__null_value() {
        //when
        Instant parsed = DateUtil.parseDateTimeFormat(null);

        //then
        assertNull(parsed);
    }

    @Test
    public void parseDateTimeFormat__empty_value() {
        //when
        Instant parsed = DateUtil.parseDateTimeFormat("");

        //then
        assertNull(parsed);
    }

    @Test
    public void parseDateTimeFormat__invalid_format() {
        //when & then
        assertThrows(DateTimeParseException.class, () -> DateUtil.parseDateTimeFormat("2024-2-1"));
    }

    @Test
    public void formatISOLocalDate() {
        //given
        Instant instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2024-04-01T16:15:45Z"));

        //when
        String formatted = DateUtil.formatISOLocalDate(instant);

        //then
        assertEquals("2024-04-01", formatted);
    }

    @Test
    public void formatISOLocalDate__null_value() {
        //when
        String formatted = DateUtil.formatISOLocalDate(null);

        //then
        assertNull(formatted);
    }
}
