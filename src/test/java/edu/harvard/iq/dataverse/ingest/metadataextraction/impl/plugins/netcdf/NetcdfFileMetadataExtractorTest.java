package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.netcdf;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataIngest;
import java.io.File;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class NetcdfFileMetadataExtractorTest {

    /**
     * Expect some lat/long values (geospatial bounding box) with longtude
     * values that have been transformed from a domain of 0 to 360 to a domain
     * of -180 to 180.
     */
    @Test
    public void testExtractLatLong() throws Exception {
        String pathAndFile = "src/test/resources/netcdf/ICOADS_R3.0.0_1662-10.nc";
        File file = new File(pathAndFile);
        NetcdfFileMetadataExtractor instance = new NetcdfFileMetadataExtractor();
        FileMetadataIngest netcdfMetadata = instance.ingestFile(file);
        Map<String, Set<String>> map = netcdfMetadata.getMetadataMap();
        assertEquals("-16.320007", map.get(DatasetFieldConstant.westLongitude).toArray()[0]);
        assertEquals("-6.220001", map.get(DatasetFieldConstant.eastLongitude).toArray()[0]);
        assertEquals("41.8", map.get(DatasetFieldConstant.southLatitude).toArray()[0]);
        assertEquals("49.62", map.get(DatasetFieldConstant.northLatitude).toArray()[0]);
    }

    /**
     * The NetCDF file under test doesn't have values for latitude/longitude
     * (geospatial bounding box).
     */
    @Test
    public void testExtractNoLatLong() throws Exception {
        String pathAndFile = "src/test/resources/netcdf/madis-raob";
        File file = new File(pathAndFile);
        NetcdfFileMetadataExtractor instance = new NetcdfFileMetadataExtractor();
        FileMetadataIngest netcdfMetadata = null;
        netcdfMetadata = instance.ingestFile(file);
        Map<String, Set<String>> map = netcdfMetadata.getMetadataMap();
        assertNull(map.get(DatasetFieldConstant.westLongitude).toArray()[0]);
        assertNull(map.get(DatasetFieldConstant.eastLongitude).toArray()[0]);
        assertNull(map.get(DatasetFieldConstant.southLatitude).toArray()[0]);
        assertNull(map.get(DatasetFieldConstant.northLatitude).toArray()[0]);
    }

    @Test
    public void testStandardLongitude() {
        NetcdfFileMetadataExtractor extractor = new NetcdfFileMetadataExtractor();

        // Both are over 180. Subtract 360 from both.
        // before: https://linestrings.com/bbox/#343.68,41.8,353.78,49.62
        // after: https://linestrings.com/bbox/#-16.320007,41.8,-6.220001,49.62
        assertEquals(new WestAndEastLongitude("-16.320007", "-6.220001"), extractor.getStandardLongitude(new WestAndEastLongitude("343.68", "353.78")));

        // "If one of them is <0, the domain is -180:180." No change. https://linestrings.com/bbox/#-10,20,100,40
        assertEquals(new WestAndEastLongitude("-10", "100"), extractor.getStandardLongitude(new WestAndEastLongitude("-10", "100")));

        // Both are negative. No change. https://linestrings.com/bbox/#-124.7666666333333,25.066666666666666,-67.058333300000015,49.40000000000000
        assertEquals(new WestAndEastLongitude("-124.7666666333333", "-67.058333300000015"), extractor.getStandardLongitude(new WestAndEastLongitude("-124.7666666333333", "-67.058333300000015")));

        // Both between 0 and 180. Leave it alone. No change. https://linestrings.com/bbox/#25,20,35,40
        assertEquals(new WestAndEastLongitude("25", "35"), extractor.getStandardLongitude(new WestAndEastLongitude("25", "35")));

        // When only one value is over 180 we can't know if we should subtract 360 from both.
        // Expect null. Don't insert potentially incorrect data into the database. https://linestrings.com/bbox/#100,20,181,40
        assertEquals(null, extractor.getStandardLongitude(new WestAndEastLongitude("100", "181")));

        // "If one of them is <0, the domain is -180:180." No change. https://linestrings.com/bbox/#-10,20,181,40
        assertEquals(null, extractor.getStandardLongitude(new WestAndEastLongitude("-10", "181")));

        // Both values are less than -180 and out of range. Expect null. No database insert https://linestrings.com/bbox/#999,20,-888,40
        assertEquals(null, extractor.getStandardLongitude(new WestAndEastLongitude("-999", "-888")));

        // Garbage in, garbage out. You can't bass "foo" and "bar" as longitudes
        assertEquals(null, extractor.getStandardLongitude(new WestAndEastLongitude("foo", "bar")));
    }

}
