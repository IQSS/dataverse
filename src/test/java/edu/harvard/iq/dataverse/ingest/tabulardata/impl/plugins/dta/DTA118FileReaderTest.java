package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

public class DTA118FileReaderTest {

    DTA118FileReader instance = new DTA118FileReader(null);
    File nullDataFile = null;

    @Test
    public void testOs() throws IOException {
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/open-source-at-harvard118.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(10, result.getDataTable().getDataVariables().size());
    }

    @Ignore
    @Test
    public void testAggregated() throws Exception {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3140457 Stata 14: 2018_04_06_Aggregated_dataset_v2.dta
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/2018_04_06_Aggregated_dataset_v2.dta"))), nullDataFile);
        assertEquals(227, result.getDataTable().getDataVariables().size());
    }
    
    //For now this test really just shows that we can parse a file with strls
    //There is no obvious output of parsing the strls as the are just added to our tab delimited file like other data
    //The test file use was based off auto.dta, adding an extra strL variable (column).
    //Also see here for a real Stata 14 file with strls: https://dataverse.harvard.edu/file.xhtml?fileId=2775556 mm_public_120615_v14.dta
    @Ignore
    @Test
    public void testStrls() throws Exception {        
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/stata14-auto-withstrls.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(13, result.getDataTable().getDataVariables().size());
    }

}
