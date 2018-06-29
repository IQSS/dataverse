/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

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

}
