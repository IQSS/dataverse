package edu.harvard.iq.dataverse.util;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

public class DateUtilTest {

    @Test
    public void shouldCorrectlyFormatDate() {
        //given
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date testDate = Date.from(Instant.ofEpochSecond(1547111385L));
        //when
        String formatedDate = DateUtil.formatDateToYMD_HM(testDate, format);
        //then
        Assert.assertEquals("2019-01-10 09:09", formatedDate);
    }

    @Test
    public void shouldCorrectlyReturnEmptyString() {
        //given
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        //when
        String formatedDate = DateUtil.formatDateToYMD_HM(null, format);
        //then
        Assert.assertEquals(StringUtils.EMPTY, formatedDate);
    }
}
