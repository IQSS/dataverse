package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Ignore;

public class DTA118FileReaderTest {

    DTA118FileReader instance = new DTA118FileReader(null);
    File nullDataFile = null;

    @Ignore
    @Test
    public void testOs() throws IOException {
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/open-source-at-harvard118.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(10, result.getDataTable().getDataVariables().size());
    }

    // TODO: Can we create a small file to check into the code base that exercises the value-label names non-zero offset issue?
    @Ignore
    @Test
    public void testFirstCategoryNonZeroOffset() throws IOException {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3140457 Stata 14: 2018_04_06_Aggregated_dataset_v2.dta
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/2018_04_06_Aggregated_dataset_v2.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(227, result.getDataTable().getDataVariables().size());
        DataVariable q10 = result.getDataTable().getDataVariables().get(25);
        assertEquals("Q10", q10.getName());
        assertEquals("Matching party leaders pics", q10.getLabel());
        assertEquals(2, q10.getCategories().size());
        List<VariableCategory> matching = (List) q10.getCategories();
        assertEquals("All matched", matching.get(0).getLabel());
        assertEquals("None matched", matching.get(1).getLabel());

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

    @Ignore
    @Test
    public void testBrooke3079508() throws IOException {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3079508 Stata 14: Brooke_Ketchley_APSR_replicationII.dta
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/Brooke_Ketchley_APSR_replicationII.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(2, result.getDataTable().getDataVariables().size());
        DataVariable year = result.getDataTable().getDataVariables().get(0);
        assertEquals("year", year.getName());
        assertEquals("year", year.getLabel());
        DataVariable missionaries = result.getDataTable().getDataVariables().get(1);
        assertEquals("missionaries", missionaries.getName());
        assertEquals("Number of Church Missionary Society missionaries", missionaries.getLabel());
    }

    @Ignore
    @Test
    public void testBrooke3079511() throws IOException {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3079511 Stata 14: Brooke_Ketchley_APSR_replicationI.dta
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/Brooke_Ketchley_APSR_replicationI.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(40, result.getDataTable().getDataVariables().size());
        DataVariable muhafatha = result.getDataTable().getDataVariables().get(0);
        assertEquals("muhafatha", muhafatha.getName());
        assertEquals("muhafatha", muhafatha.getLabel());
        DataVariable qism = result.getDataTable().getDataVariables().get(1);
        assertEquals("qism", qism.getName());
        assertEquals("qism", qism.getLabel());
    }

}
