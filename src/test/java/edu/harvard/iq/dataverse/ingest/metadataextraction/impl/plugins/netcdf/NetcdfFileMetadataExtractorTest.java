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
        // Both are over 180. Change.
        assertEquals(new WestAndEastLongitude("-16.320007", "-6.220001"), extractor.getStandardLongitude(new WestAndEastLongitude("343.68", "353.78")));
        // One is over 180. Change
        assertEquals(new WestAndEastLongitude("-260.0", "-179.0"), extractor.getStandardLongitude(new WestAndEastLongitude("100", "181")));
        // Both are under 180. No change.
        assertEquals(new WestAndEastLongitude("25", "35"), extractor.getStandardLongitude(new WestAndEastLongitude("25", "35")));
        assertEquals(null, extractor.getStandardLongitude(new WestAndEastLongitude("foo", "bar")));
    }

}
