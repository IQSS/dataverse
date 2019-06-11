package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DTAFileReaderTest {

    DTAFileReader instance = new DTAFileReader(null);
    File nullDataFile = null;

    @Test
    public void testOs() throws IOException {
        byte[] dtaFileBytes = IOUtils.resourceToByteArray("/dta/50by1000.dta");
        TabularDataIngest result = instance.read(new BufferedInputStream(new ByteArrayInputStream(dtaFileBytes)), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("rel_8_or_9", result.getDataTable().getOriginalFormatVersion());
        assertEquals(50, result.getDataTable().getDataVariables().size());
    }

}
