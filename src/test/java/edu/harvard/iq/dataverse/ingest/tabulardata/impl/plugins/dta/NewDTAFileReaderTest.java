package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

public class NewDTAFileReaderTest {

    NewDTAFileReader instance;
    File nullDataFile = null;

    // TODO: Can we create a small file to check into the code base that exercises the characteristics issue?
    // FIXME: testCharacteristics is passing in DTA117FileReaderTest but not here.
    @Ignore
    @Test
    public void testCharacteristics() throws IOException {
        instance = new NewDTAFileReader(null, 117);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/15aa6802ee5-5d2ed1bf55a5.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(441, result.getDataTable().getDataVariables().size());
    }

    @Test
    public void testAuto() throws IOException {
        instance = new NewDTAFileReader(null, 117);
        // From https://www.stata-press.com/data/r13/auto.dta
        // `strings` shows "<stata_dta><header><release>117</release>"
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/stata13-auto.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(12, result.getDataTable().getDataVariables().size());
        DataVariable foreign = result.getDataTable().getDataVariables().get(11);
        assertEquals(2, foreign.getCategories().size());
        List<VariableCategory> origins = (List) foreign.getCategories();
        assertEquals("Domestic", origins.get(0).getLabel());
        assertEquals("Foreign", origins.get(1).getLabel());
    }

    // TODO: Can we create a small file to check into the code base that exercises the value-label names non-zero offset issue?
    @Ignore
    @Test
    public void testFirstCategoryNonZeroOffset() throws IOException {
        instance = new NewDTAFileReader(null, 117);

        // https://dataverse.harvard.edu/file.xhtml?fileId=2865667 Stata 13 HouseImputingCivilRightsInfo.dta md5=7dd144f27cdb9f8d1c3f4eb9c4744c42
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/HouseImputingCivilRightsInfo.dta"))), nullDataFile);
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

    //For now this test really just shows that we can parse a file with strls
    //There is no obvious output of parsing the strls as the are just added to our tab delimited file like other data
    //The test file use was based off auto.dta, adding an extra strL variable (column).
    //See https://github.com/IQSS/dataverse/issues/1016 for info on Stata 13 strl support
    @Test
    public void testStrls() throws Exception {
        instance = new NewDTAFileReader(null, 117);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/stata13-auto-withstrls.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 13", result.getDataTable().getOriginalFormatVersion());
        assertEquals(13, result.getDataTable().getDataVariables().size());
    }

    @Ignore
    @Test
    public void testOs() throws IOException {
        instance = new NewDTAFileReader(null, 118);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/open-source-at-harvard118.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(10, result.getDataTable().getDataVariables().size());
    }

    // TODO: Can we create a small file to check into the code base that exercises the value-label names non-zero offset issue?
    @Ignore
    @Test
    public void testFirstCategoryNonZeroOffset1() throws IOException {
        instance = new NewDTAFileReader(null, 118);
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
        // Given the MD5 above, we expect the categories to come out in the order below.
        assertEquals("None matched", matching.get(0).getLabel());
        assertEquals("All matched", matching.get(1).getLabel());

    }

    //For now this test really just shows that we can parse a file with strls
    //There is no obvious output of parsing the strls as the are just added to our tab delimited file like other data
    //The test file use was based off auto.dta, adding an extra strL variable (column).
    //Also see here for a real Stata 14 file with strls: https://dataverse.harvard.edu/file.xhtml?fileId=2775556 mm_public_120615_v14.dta
    @Test
    public void testStrls1() throws Exception {
        instance = new NewDTAFileReader(null, 118);
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("scripts/search/data/tabular/stata14-auto-withstrls.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        assertEquals(13, result.getDataTable().getDataVariables().size());
    }

    @Ignore
    @Test
    public void testBrooke3079508() throws IOException {
        instance = new NewDTAFileReader(null, 118);
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
        instance = new NewDTAFileReader(null, 118);
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

    // TODO: Get this file to ingest.
    @Ignore
    @Test
    public void testSectionTagsAcrossByteBuffers() throws IOException {
        instance = new NewDTAFileReader(null, 118);
        // https://data.aussda.at/file.xhtml?fileId=188 Stata 14: 10007_da_de_v1_2.dta
        // Via https://groups.google.com/d/msg/dataverse-community/QAX3LaMsbjI/jHrf089QAgAJ
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/10007_da_de_v1_2.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 14", result.getDataTable().getOriginalFormatVersion());
        // TODO: Change size from 0 once we can parse the file.
        assertEquals(0, result.getDataTable().getDataVariables().size());
    }

    // TODO: Is there a way to exersise this code with a smaller file? 33k.dta is 21MB.
    @Ignore
    @Test
    public void test33k() throws IOException {
        instance = new NewDTAFileReader(null, 119);
        // for i in `echo {0..33000}`; do echo -n "var$i,"; done > 33k.csv
        // Then open Stata 15, run `set maxvar 40000` and import.
        TabularDataIngest result = instance.read(new BufferedInputStream(new FileInputStream(new File("/tmp/33k.dta"))), nullDataFile);
        assertEquals("application/x-stata", result.getDataTable().getOriginalFileFormat());
        assertEquals("STATA 15", result.getDataTable().getOriginalFormatVersion());
        assertEquals(33001, result.getDataTable().getDataVariables().size());
    }
}
