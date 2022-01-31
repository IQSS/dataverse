package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.VariableCategory;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IngestFrequencyTest {

    private IngestServiceBean ingestService = new IngestServiceBean();

    @Test
    public void testFrequency() throws IOException {
        DataFile dataFile = readFileCalcFreq("sav/frequency-test.sav" , "application/x-spss-sav");

        assertNotNull(dataFile);

        long varQuant = dataFile.getDataTable().getVarQuantity();
        assertEquals(varQuant, 3);

        Collection<VariableCategory> cats1 = dataFile.getDataTable().getDataVariables().get(0).getCategories();
        assertEquals(cats1.size(),2);
        testVariable(cats1, VariableCategory::getLabel,
                Tuple.of("Male", 1537),
                Tuple.of("Female", 1508));

        Collection<VariableCategory> cats2 = dataFile.getDataTable().getDataVariables().get(1).getCategories();
        assertEquals(cats2.size(),4);
        testVariable(cats2, VariableCategory::getValue,
                Tuple.of("1", 0),
                Tuple.of("2", 691),
                Tuple.of("3", 1262),
                Tuple.of("4", 1092));

        Collection<VariableCategory> cats3 = dataFile.getDataTable().getDataVariables().get(2).getCategories();
        assertEquals(cats3.size(),2);
        testVariable(cats3, VariableCategory::getValue,
                Tuple.of("1", 2497),
                Tuple.of("2", 548));

        DataFile dataFileDta = readFileCalcFreq("dta/test_cat_values.dta" , "application/x-stata-14");
        assertNotNull(dataFileDta);

        long varQuantDta = dataFileDta.getDataTable().getVarQuantity();
        assertEquals(varQuantDta, 1);

        Collection<VariableCategory> cats = dataFileDta.getDataTable().getDataVariables().get(0).getCategories();
        assertEquals(cats.size(),2);
        testVariable(cats, VariableCategory::getLabel,
                Tuple.of("Urban", 6),
                Tuple.of("Rural", 4));
    }

    // -------------------- PRIVATE --------------------

    private DataFile readFileCalcFreq(String fileName, String type ) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(UnitTestUtils.readFileToByteArray(fileName));
            BufferedInputStream fileInputStream = new BufferedInputStream(inputStream)) {

            TabularDataFileReader ingestPlugin = ingestService.getTabDataReaderByMimeType(type);
            assertNotNull(ingestPlugin);

            TabularDataIngest tabDataIngest = ingestPlugin.read(fileInputStream, null);
            File tabFile = tabDataIngest.getTabDelimitedFile();

            assertNotNull(tabDataIngest.getDataTable());
            assertNotNull(tabFile);

            DataTable dataTable = tabDataIngest.getDataTable();
            DataFile dataFile = new DataFile();

            dataFile.setDataTable(dataTable);
            dataTable.setDataFile(dataFile);
            String[][] table = TabularSubsetGenerator.readFileIntoTable(dataTable, tabFile);
            IngestServiceBean.produceFrequencyStatistics(table, dataFile);
            return dataFile;
        }
    }

    private void testVariable(Collection<VariableCategory> categories, Function<VariableCategory, String> extractor,
                              Tuple2<String, Integer>... values) {
        Map<String, Integer> testValues = Arrays.stream(values)
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
        for (VariableCategory category : categories) {
            long frequency = Math.round(category.getFrequency());
            Integer expectedValue = testValues.get(extractor.apply(category));
            Assertions.assertThat((int) frequency).isEqualTo(expectedValue);
        }
    }
}
