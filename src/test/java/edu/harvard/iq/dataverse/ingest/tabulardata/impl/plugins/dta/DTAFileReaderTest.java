package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DTAFileReaderTest {

    DTAFileReader instance = new DTAFileReader(null);
    File nullDataFile = null;

    @Test
    public void testOs() throws IOException {
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/50by1000.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("rel_8_or_9", result.getDataTable().getOriginalFormatVersion());
        assertEquals(50, result.getDataTable().getDataVariables().size());
    }

}
