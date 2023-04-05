package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class NetcdfUtilTest {

    @Test
    public void testParseGeoFields() throws IOException {
        String path = "/Users/pdurbin/Downloads/";
        String pathAndFile = path + "bi_1985.nc";
        pathAndFile = "src/test/resources/netcdf/ICOADS_R3.0.0_1662-10.nc";
        File file = new File(pathAndFile);
        Map<String, String> geoFields = NetcdfUtil.parseGeospatial(NetcdfUtil.getNetcdfFile(file));
        System.out.println("geoFields: " + geoFields);

        // left, bottom, right, top
//        https://linestrings.com/bbox/#-71.187346,42.33661,-71.043056,42.409599
        System.out.println("https://linestrings.com/bbox/#"
                + geoFields.get(DatasetFieldConstant.westLongitude) + ","
                + geoFields.get(DatasetFieldConstant.southLatitude) + ","
                + geoFields.get(DatasetFieldConstant.eastLongitude) + ","
                + geoFields.get(DatasetFieldConstant.westLongitude)
        );
    }

}
