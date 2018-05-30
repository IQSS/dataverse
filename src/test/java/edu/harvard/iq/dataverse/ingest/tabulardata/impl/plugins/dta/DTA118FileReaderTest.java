package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

public class DTA118FileReaderTest {

    DTA118FileReader instance = new DTA118FileReader(null);
    File nullDataFile = null;

    @Test
    public void testOs() throws Exception {
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/open-source-at-harvard118.dta"))), nullDataFile);
        assertEquals(10, result.getDataTable().getDataVariables().size());
    }

    @Ignore
    @Test
    public void testAggregated() throws Exception {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3140457 Stata 14: 2018_04_06_Aggregated_dataset_v2.dta
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/2018_04_06_Aggregated_dataset_v2.dta"))), nullDataFile);
        assertEquals(227, result.getDataTable().getDataVariables().size());
    }

    @Ignore
    @Test
    public void testMmPublic() throws Exception {
        // TODO: This file was downloaded at random. We could keep trying to get it to ingest.
        // https://dataverse.harvard.edu/file.xhtml?fileId=2775556 Stata 14: mm_public_120615_v14.dta
        // For this file "hasSTRLs" is true so it might be nice to get it working.
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/mm_public_120615_v14.dta"))), nullDataFile);
        assertEquals(227, result.getDataTable().getDataVariables().size());
    }

}
