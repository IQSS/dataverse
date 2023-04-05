package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.netcdf;

import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataIngest;
import java.io.File;
import org.junit.Test;

public class NetcdfFileMetadataExtractorTest {

    @Test
    public void testIngestFile() throws Exception {
        System.out.println("ingestFile");
        String pathAndFile = "src/test/resources/netcdf/ICOADS_R3.0.0_1662-10.nc";
        File file = new File(pathAndFile);
        NetcdfFileMetadataExtractor instance = new NetcdfFileMetadataExtractor();
        FileMetadataIngest expResult = null;
        // TODO: rename this to netcdfMetadata
        FileMetadataIngest fitsMetadata = instance.ingestFile(file);
        System.out.println("result: " + fitsMetadata);
        for (String mKey : fitsMetadata.getMetadataMap().keySet()) {
            System.out.println("mKey: " + mKey);
        }
    }

}
