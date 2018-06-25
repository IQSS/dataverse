/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author oscardssmith
 */
public class FileSizeCheckerTest {
    private static final Logger logger = Logger.getLogger(FileSizeChecker.class.getCanonicalName());
    
    public FileSizeCheckerTest() {
    }

    @Test
    public void testIsAllowedFileSize() {
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
        
        List<String> expAns = Arrays.asList(new String[]{"1 B", "1023 B", "1.9 kB", "122.8 kB", "2.6 GB", "10.9 TB"});
        List<String> expLongAns = Arrays.asList(new String[]{"1 B", "1023 B", "1.94 kB", "122.76 kB", "2.57 GB", "10.95 TB"});
        assertEquals(expAns, ans);
        assertEquals(expLongAns, longAns);
    }

}
