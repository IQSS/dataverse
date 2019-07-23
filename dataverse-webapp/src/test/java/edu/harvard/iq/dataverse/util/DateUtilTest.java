package edu.harvard.iq.dataverse.util;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

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
        Assert.assertEquals("2019-01-10 09:09", formattedDate);
    }

    @Test
    public void shouldCorrectlyReturnEmptyString() {
        //given
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        //when
        String formattedDate = DateUtil.formatDate(null, format);
        //then
        Assert.assertEquals(StringUtils.EMPTY, formattedDate);
    }

    @Test
    public void shouldCorrectlyFormatDate_forUTC() {
        //given
        Date testDate = Date.from(Instant.ofEpochMilli(TEST_DATE));

        //when
        DateTimeFormatter isoFormatter = DateUtil.retrieveISOFormatter(ZoneId.of("UTC"));
        String formattedDate = isoFormatter.format(testDate.toInstant());

        //then
        Assert.assertEquals("2019-07-23T08:38:40Z", formattedDate);

    }

    @Test
    public void shouldCorrectlyFormatDate_forOffsetPlusTwo() {
        //given
        Date testDate = Date.from(Instant.ofEpochMilli(TEST_DATE));

        //when
        DateTimeFormatter isoFormatter = DateUtil.retrieveISOFormatter(ZoneId.of("CET"));
        String formattedDate = isoFormatter.format(testDate.toInstant());

        //then
        Assert.assertEquals("2019-07-23T10:38:40+02:00", formattedDate);

    }
}
