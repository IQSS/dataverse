package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.dataaccess.Range;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DownloadInstanceWriterTest {

    DownloadInstanceWriter diw;

    @BeforeEach
    public void setUpClass() {
        diw = new DownloadInstanceWriter();
    }

    // Get first 10 bytes.
    @Test
    public void testGetRange0to9of100() {
        List<Range> ranges = diw.getRanges("bytes=0-9", 100);
        assertEquals(0, ranges.get(0).getStart());
        assertEquals(9, ranges.get(0).getEnd());
        assertEquals(10, ranges.get(0).getLength());
    }

    // Don't start reading until 90th byte, get rest.
    @Test
    public void testGetRange90toNullof100() {
        List<Range> ranges = diw.getRanges("bytes=90-", 100);
        assertEquals(90, ranges.get(0).getStart());
        assertEquals(99, ranges.get(0).getEnd());
        assertEquals(10, ranges.get(0).getLength());
    }

    // Get last 10 bytes.
    @Test
    public void testGetRangeNullto5of10() {
        List<Range> ranges = diw.getRanges("bytes=-10", 100);
        assertEquals(90, ranges.get(0).getStart());
        assertEquals(99, ranges.get(0).getEnd());
        assertEquals(10, ranges.get(0).getLength());
    }

    // Get last byte
    @Test
    public void testGetRange100toNullof101() {
        List<Range> ranges = diw.getRanges("bytes=100-", 101);
        assertEquals(100, ranges.get(0).getStart());
        assertEquals(100, ranges.get(0).getEnd());
        assertEquals(1, ranges.get(0).getLength());
    }

    // When you request a range beyond the size of the file we just
    // give you what we can (rather than throwing an error). 
    @Test
    public void testGetRangeBeyondFilesize() {
        List<Range> ranges = diw.getRanges("bytes=100-149", 120);
        assertEquals(100, ranges.get(0).getStart());
        assertEquals(119, ranges.get(0).getEnd());
        assertEquals(20, ranges.get(0).getLength());
    }

    // Attempt to get invalid range (start larger than end).
    @Test
    public void testGetRangeInvalidStartLargerThanEnd() {
        Exception expectedException = null;
        try {
            List<Range> ranges = diw.getRanges("bytes=20-10", 100);
        } catch (Exception ex) {
            // "Start is larger than end or size of file."
            System.out.println("exception: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

    // Attempt to get invalid range (multiple ranges).
    @Test
    public void testGetRangeInvalidMultipleRanges() {
        Exception expectedException = null;
        try {
            List<Range> ranges = diw.getRanges("bytes=0-9,90-99", 100);
        } catch (Exception ex) {
            // "Only one range is allowed."
            System.out.println("exception: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

    // Attempt to get invalid range (multiple ranges, beyond file size).
    @Test
    public void testGetRangeInvalidMultipleRangesBeyondFileSize() {
        Exception expectedException = null;
        try {
            List<Range> ranges = diw.getRanges("bytes=0-9,90-99", 40);
        } catch (Exception ex) {
            // "Only one range is allowed."
            // We report the multiple ranges error before reporting the "beyond filesize" error.
            System.out.println("exception: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

    // Test "junk" instead of "bytes=0-10"
    @Test
    public void testGetRangeInvalidJunk() {
        Exception expectedException = null;
        try {
            List<Range> ranges = diw.getRanges("junk", 100);
        } catch (Exception ex) {
            // "The format is bytes=<range-start>-<range-end> where start and end are optional."
            System.out.println("exception: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

    // Get first 10 bytes and last 10 bytes. Not currently supported.
    @Test
    public void testGetRanges0to0and90toNull() {
        Exception expectedException = null;
        try {
            List<Range> ranges = diw.getRanges("bytes=0-9,-10", 100);
            // These asserts on start, end, etc. don't actually
            // run because we throw an expection that multiple
            // ranges are not supported.
            //
            // first range
            assertEquals(0, ranges.get(0).getStart());
            assertEquals(9, ranges.get(0).getEnd());
            assertEquals(10, ranges.get(0).getLength());
            // second range
            assertEquals(90, ranges.get(1).getStart());
            assertEquals(99, ranges.get(1).getEnd());
            assertEquals(10, ranges.get(1).getLength());
        } catch (Exception ex) {
            // Only one range is allowed.
            System.out.println("exception: " + ex);
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

}
