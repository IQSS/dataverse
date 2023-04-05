package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class NetcdfTest {
    @Test
    public void testHdf4File() throws IOException {
        String path = "/Users/pdurbin/Downloads/";
        String pathAndFile = path + "bi_1985.nc";
        File file = new File(pathAndFile);
        String contentType = FileUtil.determineFileType(file, pathAndFile);
        assertEquals("application/netcdf", contentType);
    }
    
}
