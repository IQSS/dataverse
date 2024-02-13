package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NewDTAFileReaderTest {
    NewDTAFileReader instance;
    File nullDataFile = null;
    private final String base = "src/test/java/edu/harvard/iq/dataverse/ingest/tabulardata/impl/plugins/dta/";

    @Test
    public void testAuto() throws IOException {
        instance = new NewDTAFileReader(null, 117);
        // From https://www.stata-press.com/data/r13/auto.dta
        // `strings` shows "<stata_dta><header><release>117</release>"
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/stata13-auto.dta"))), false, nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(12, result.getDataTable().getDataVariables().size());
        DataVariable foreign = result.getDataTable().getDataVariables().get(11);
        assertEquals(2, foreign.getCategories().size());
        List<VariableCategory> origins = (List) foreign.getCategories();
        assertEquals("Domestic", origins.get(0).getLabel());
        assertEquals("Foreign", origins.get(1).getLabel());
    }
    
    @Test
    public void testStrl() throws IOException {
        instance = new NewDTAFileReader(null, 118);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File(base + "strl.dta"))), false, nullDataFile);
        DataTable table = result.getDataTable();
        assertEquals("application/x-stata", table.getOriginalFileFormat());
        assertEquals("STATA 14", table.getOriginalFormatVersion());
        assertEquals(7, table.getDataVariables().size());
        assertEquals(3, (long)table.getCaseQuantity());
        
        String[] vars = {"make","price","mpg","rep78","trunk","gear_ratio","strls"};
        String[] actualVars = table.getDataVariables().stream().map((var) -> var.getName()).toArray(String[]::new);
        assertArrayEquals(vars, actualVars);
        String expected = "\"Buick LeSabre\"	5788	1.1111111111111111E21	100	32767	2.73	\"a\"\n" +
                          "\"Buick Opel\"	4453	26.0		10	2.87	\"bb\"\n" +
                          "\"Buick Regal\"	5189	20.0	3	16	2.93	\"ccc\"\n";
        assertEquals(expected, FileUtils.readFileToString(result.getTabDelimitedFile()));
    }
    
    @Test
    public void testDates() throws IOException {
        instance = new NewDTAFileReader(null, 118);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File(base + "dates.dta"))), false, nullDataFile);
        DataTable table = result.getDataTable();
        assertEquals("application/x-stata", table.getOriginalFileFormat());
        assertEquals("STATA 14", table.getOriginalFormatVersion());
        assertEquals(7, table.getDataVariables().size());
        assertEquals(4, (long)table.getCaseQuantity());
        String[] vars = {"Clock","Daily","Weekly","Monthly","Quarterly","BiAnnually","Annually"};
        String[] actualVars = table.getDataVariables().stream().map((var) -> var.getName()).toArray(String[]::new);
        assertArrayEquals(vars, actualVars);
        String expected = "2595-09-27 06:58:52.032	2018-06-20	2018-11-05	2018-06-01	2018-01-01	2018-01-01	2018\n" +
                          "2595-09-27 06:58:52.032	2018-06-20	2018-11-05	2018-06-01	2018-04-01	2018-01-01	2018\n" +
                          "2595-09-27 06:58:52.032	2018-06-20	2018-11-05	2018-06-01	2018-07-01	2018-07-01	2018\n" +
                          "2595-09-27 06:58:52.032	2018-06-20	2018-11-05	2018-06-01	2018-11-01	2018-07-01	2018\n";
        assertEquals(expected, FileUtils.readFileToString(result.getTabDelimitedFile()));
    }
    
    @Test
    void testNull() {
        instance = new NewDTAFileReader(null, 117);
        assertThrows(IOException.class, () -> instance.read(null, false, new File("")));
    }

    // TODO: Can we create a small file to check into the code base that exercises the value-label names non-zero offset issue?
    @Disabled
    @Test
    public void testFirstCategoryNonZeroOffset() throws IOException {
        instance = new NewDTAFileReader(null, 117);

        // https://dataverse.harvard.edu/file.xhtml?fileId=2865667 Stata 13 HouseImputingCivilRightsInfo.dta md5=7dd144f27cdb9f8d1c3f4eb9c4744c42
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/HouseImputingCivilRightsInfo.dta"))), false, nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(5, result.getDataTable().getDataVariables().size());
        DataVariable imputing = result.getDataTable().getDataVariables().get(4);
        assertEquals("imputingincludes10perofmembers", imputing.getName());
        assertEquals("Dummy Variable: 1 = More than 10% of votes cast were imputed; 0 = Less than 10%", imputing.getLabel());
        assertEquals(2, imputing.getCategories().size());
        List<VariableCategory> origins = (List) imputing.getCategories();
        // Given the MD5 above, we expect the categories to come out in the order below.
        assertEquals("Fewer than 10% Imputed", origins.get(0).getLabel());
        assertEquals("More than 10% Imputed", origins.get(1).getLabel());
    }

    // TODO: Can we create a small file to check into the code base that exercises the value-label names non-zero offset issue?
    @Disabled
    @Test
    public void testFirstCategoryNonZeroOffset1() throws IOException {
        instance = new NewDTAFileReader(null, 118);
        // https://dataverse.harvard.edu/file.xhtml?fileId=3140457 Stata 14: 2018_04_06_Aggregated_dataset_v2.dta
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/2018_04_06_Aggregated_dataset_v2.dta"))), false, nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(227, result.getDataTable().getDataVariables().size());
        DataVariable q10 = result.getDataTable().getDataVariables().get(25);
        assertEquals("Q10", q10.getName());
        assertEquals("Matching party leaders pics", q10.getLabel());
        assertEquals(2, q10.getCategories().size());
        List<VariableCategory> matching = (List) q10.getCategories();
        // Given the MD5 above, we expect the categories to come out in the order below.
        assertEquals("None matched", matching.get(0).getLabel());
        assertEquals("All matched", matching.get(1).getLabel());
    }
    
    // TODO: Is there a way to exersise this code with a smaller file? 33k.dta is 21MB.
    @Disabled
    @Test
    public void test33k() throws IOException {
        instance = new NewDTAFileReader(null, 119);
        // for i in `echo {0..33000}`; do echo -n "var$i,"; done > 33k.csv
        // Then open Stata 15, run `set maxvar 40000` and import.
    }
    
    // TODO: Can we create a small file to check into the code base that exercises the characteristics issue?
    // FIXME: testCharacteristics is passing in DTA117FileReaderTest but not here.
    @Disabled
    @Test
    public void testCharacteristics() throws IOException {
        instance = new NewDTAFileReader(null, 117);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/15aa6802ee5-5d2ed1bf55a5.dta"))), false, nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(441, result.getDataTable().getDataVariables().size());
    }
}
