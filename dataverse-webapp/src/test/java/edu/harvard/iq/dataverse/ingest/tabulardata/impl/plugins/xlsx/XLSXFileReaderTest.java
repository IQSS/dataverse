package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv.CSVFileReaderTest;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import io.vavr.Tuple;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class XLSXFileReaderTest {

    @ParameterizedTest
    @ValueSource(strings = { "xslx/table-google.xlsx", "xslx/table-libre.xlsx", "xslx/table-excel.xlsx" })
    void read__various_sources(String xlsxFile) throws Exception {
        // when
        TabularDataIngest result = read(xlsxFile);

        // then
        assertThat(result.getDataTable().getVarQuantity()).isEqualTo(5);
        assertThat(result.getDataTable().getDataVariables()).extracting(DataVariable::getName)
                .containsExactly(
                        "Id", "Item", "cost", "count", "total");
        assertThat(Files.readAllLines(result.getTabDelimitedFile().toPath()))
                .containsExactly(
                        "1.0\t\"Banana\"\t2.3\t4.0\t9.2",
                        "2.0\t\"Choco\"\t8.49\t2.0\t16.98",
                        "3.0\t\"Headset\"\t248.99\t1.0\t248.99");
    }

    @ParameterizedTest
    @CsvSource({
            "xslx/missing-columns-libre.xlsx,false", // disabled, because unsupported by the current implementation
            "xslx/missing-columns-excel.xlsx,true" })
    void read__missing_columns(String xlsxFile, boolean enabled) throws Exception {
        Assumptions.assumeTrue(enabled, "Test file " + xlsxFile + " is disabled.");

        // when
        TabularDataIngest result = read(xlsxFile);

        // then
        assertThat(result.getDataTable().getVarQuantity()).isEqualTo(8);
        assertThat(result.getDataTable().getDataVariables()).extracting(DataVariable::getName)
                .containsExactly("A", "Col1", "Col2", "D", "Col4", "Col5", "G", "Col7");
        assertThat(Files.readAllLines(result.getTabDelimitedFile().toPath()))
                .containsExactly(
                        "\"Row1\"\t1.1\t1.2\t1.3\t1.4\t1.5\t1.6\t1.7",
                        "\"Row2\"\t2.1\t2.2\t2.3\t2.4\t2.5\t2.6\t2.7",
                        "\"Row3\"\t3.1\t3.2\t3.3\t3.4\t3.5\t3.6\t3.7",
                        "\"\"\t4.1\t4.2\t4.3\t4.4\t4.5\t4.6\t4.7",
                        "\"Row5\"\t5.1\t5.2\t5.3\t5.4\t6.5\t5.6\t5.7");
    }

    @ParameterizedTest
    @CsvSource({ "xslx/value-types-libre.xlsx", "xslx/value-types-excel.xlsx" })
    void read__value_types(String xlsxFile) throws Exception {
        // when
        TabularDataIngest result = read(xlsxFile);

        // then
        assertThat(result.getDataTable().getVarQuantity()).isEqualTo(4);
        assertThat(result.getDataTable().getDataVariables()).extracting(DataVariable::getName)
                .containsExactly("A", "B", "Total", "Div");
        assertThat(result.getDataTable().getDataVariables()).extracting(DataVariable::getType)
                .containsExactly(
                        DataVariable.VariableType.CHARACTER,
                        DataVariable.VariableType.NUMERIC,
                        DataVariable.VariableType.CHARACTER,
                        DataVariable.VariableType.CHARACTER);
        assertThat(Files.readAllLines(result.getTabDelimitedFile().toPath()))
                .containsExactly(
                        "\"1\"\t1.0\t\"1\"\t\"1\"",
                        "\"2\"\t4.0\t\"8\"\t\"0.5\"",
                        "\"A\"\t4.0\t\"#VALUE!\"\t\"#VALUE!\"",
                        "\"1\"\t0.0\t\"0\"\t\"#DIV/0!\"");
    }

    private TabularDataIngest read(String xlsxFile) throws IOException {
        try {
            File file = Paths.get(CSVFileReaderTest.class.getClassLoader().getResource(xlsxFile).toURI()).toFile();
            TabularDataIngest result;
            try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
                XLSXFileReader reader = new XLSXFileReader(new XLSXFileReaderSpi());
                result = reader.read(Tuple.of(is, file), null);
            }
            return result;
        } catch (URISyntaxException use) {
            throw new RuntimeException(use);
        }
    }
}
