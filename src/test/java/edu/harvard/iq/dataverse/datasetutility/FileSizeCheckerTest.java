/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;

import edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.FileSizeResponse;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author oscardssmith
 */
public class FileSizeCheckerTest {

    private FileSizeChecker fileSizeChecker;

    @Before
    public void setUp() {
        // initialize a system config and instantiate a file size checker
        // override the max file upload side to allow for testing
        fileSizeChecker = new FileSizeChecker(new SystemConfig() {
            @Override
            public Long getMaxFileUploadSize() {
                return 1000L;
            }
        });
    }

    @Test
    public void testBytesToHumanReadable() {
        long[] sizes = {1L, 1023L, 1986L, 125707L, 2759516000L, 12039650000000L};
        List<String> ans = new ArrayList<>();
        List<String> longAns = new ArrayList<>();
        for (long size: sizes){
            ans.add(bytesToHumanReadable(size));
            longAns.add(bytesToHumanReadable(size, 2));
        }
        String B = BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev");
        List<String> expAns = Arrays.asList(new String[]{"1 "+B, "1023 "+B, "1.9 K"+B, "122.8 K"+B, "2.6 G"+B, "10.9 T"+B});
        List<String> expLongAns = Arrays.asList(new String[]{"1 "+B, "1023 "+B, "1.94 K"+B, "122.76 K"+B, "2.57 G"+B, "10.95 T"+B});
        assertEquals(expAns, ans);
        assertEquals(expLongAns, longAns);
    }

    @Test(expected = NullPointerException.class)
    public void testIsAllowedFileSize_throwsOnNull() {
        fileSizeChecker.isAllowedFileSize(null);
    }

    @Test
    public void testIsAllowedFileSize_allowsSmallerFileSize() {
        FileSizeResponse response = fileSizeChecker.isAllowedFileSize(999L);
        assertTrue(response.fileSizeOK);
    }

    @Test
    public void testIsAllowedFileSize_allowsEqualFileSize() {
        FileSizeResponse response = fileSizeChecker.isAllowedFileSize(1000L);
        assertTrue(response.fileSizeOK);
    }

    @Test
    public void testIsAllowedFileSize_rejectsBiggerFileSize() {
        FileSizeResponse response = fileSizeChecker.isAllowedFileSize(1001L);
        assertFalse(response.fileSizeOK);
    }

    @Test(expected = NullPointerException.class)
    public void testIsAllowedFileSize_allowsOnUnboundedFileSize() {
        // initialize a system config and instantiate a file size checker
        // ensure that a max filesize is not set
        FileSizeChecker unboundedFileSizeChecker = new FileSizeChecker(new SystemConfig() {
            @Override
            public Long getMaxFileUploadSize() {
                return null;
            }
        });
        FileSizeResponse response = unboundedFileSizeChecker.isAllowedFileSize(Long.MAX_VALUE);
        assertTrue(response.fileSizeOK);
    }
}
