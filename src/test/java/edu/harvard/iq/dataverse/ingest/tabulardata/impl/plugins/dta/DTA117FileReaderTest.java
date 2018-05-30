package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DTA117FileReaderTest {

    DTA117FileReader instance = new DTA117FileReader(null);
    File nullDataFile = null;

    @Test
    public void testAuto() throws IOException {
        // From https://www.stata-press.com/data/r13/auto.dta
        // `strings` shows "<stata_dta><header><release>117</release>"
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/stata13-auto.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(12, result.getDataTable().getDataVariables().size());
    }

}
