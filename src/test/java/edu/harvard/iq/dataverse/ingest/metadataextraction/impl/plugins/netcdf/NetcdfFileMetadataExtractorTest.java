package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.netcdf;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataIngest;
import java.io.File;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class NetcdfFileMetadataExtractorTest {

    @Test
    public void testIngestFile() throws Exception {
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

}
