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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author oscardssmith
 */
public class FileSizeCheckerTest {
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

    @Test
    public void testIsAllowedFileSize_throwsOnNull() {
        FileSizeChecker fileSizeChecker = new FileSizeChecker(new SystemConfig() {
            @Override
            public Long getMaxFileUploadSize() {
                return 1000L;
            }
        });
        assertThrows(NullPointerException.class, () -> {
            fileSizeChecker.isAllowedFileSize(null);
        });
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, 999L, 1000L })
    public void testIsAllowedFileSize_allowsSmallerOrEqualFileSize(Long fileSize) {
        // initialize a system config and instantiate a file size checker
        // override the max file upload side to allow for testing
        FileSizeChecker fileSizeChecker = new FileSizeChecker(new SystemConfig() {
            @Override
            public Long getMaxFileUploadSize() {
                return 1000L;
            }
        });
        FileSizeResponse response = fileSizeChecker.isAllowedFileSize(fileSize);
        assertTrue(response.fileSizeOK);
    }

    @ParameterizedTest
    @ValueSource(longs = { 1001L, Long.MAX_VALUE })
    public void testIsAllowedFileSize_rejectsBiggerFileSize(Long fileSize) {
        // initialize a system config and instantiate a file size checker
        // override the max file upload side to allow for testing
        FileSizeChecker fileSizeChecker = new FileSizeChecker(new SystemConfig() {
            @Override
            public Long getMaxFileUploadSize() {
                return 1000L;
            }
        });
        FileSizeResponse response = fileSizeChecker.isAllowedFileSize(fileSize);
        assertFalse(response.fileSizeOK);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, 1000L, Long.MAX_VALUE })
    public void testIsAllowedFileSize_allowsOnUnboundedFileSize(Long fileSize) {
        // initialize a system config and instantiate a file size checker
        // ensure that a max filesize is not set
        FileSizeChecker unboundedFileSizeChecker = new FileSizeChecker(new SystemConfig() {
            @Override
            public Long getMaxFileUploadSize() {
                return null;
            }
        });
        FileSizeResponse response = unboundedFileSizeChecker.isAllowedFileSize(fileSize);
        assertTrue(response.fileSizeOK);
    }
}
