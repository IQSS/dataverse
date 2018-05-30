package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

public class DTA119FileReaderTest {

    DTA119FileReader instance = new DTA119FileReader(null);
    File nullDataFile = null;

    // TODO: Is there a way to exersise this code with a smaller file? 33k.dta is 21MB.
    @Ignore
    @Test
    public void test33k() throws IOException {
        // for i in `echo {0..33000}`; do echo -n "var$i,"; done > 33k.csv
        // Then open Stata 15, run `set maxvar 40000` and import.
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/33k.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 15", result.getDataTable().getOriginalFormatVersion());
        assertEquals(33001, result.getDataTable().getDataVariables().size());
    }

}
