package edu.harvard.iq.dataverse.dataaccess.ingest;

import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

interface IngestDataProviderTest {

    // -------------------- TESTS --------------------

    @Test
    default void getLongColumn() {
        // given
        IngestDataProvider provider = getInitializedProvider();

        // when
        Long[] column = provider.getLongColumn(0);

        // then
        assertThat(column).isEqualTo(new Long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L });
    }

    @Test
    default void getFloatColumn_getDoubleColumn() {
        // given
        IngestDataProvider provider = getInitializedProvider();

        // when
        Float[] floatColumn = provider.getFloatColumn(1);
        Double[] doubleColumn = provider.getDoubleColumn(1);

        // then
        assertThat(floatColumn).isEqualTo(new Float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f });
        assertThat(doubleColumn).isEqualTo(new Double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1d });
    }

    @Test
    default void getStringColumn() {
        // given
        IngestDataProvider provider = getInitializedProvider();

        // when
        String[] column = provider.getStringColumn(2);

        // then
        assertThat(column).isEqualTo(new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" });
    }

    @Test
    default void getColumnIterable() {
        // given
        IngestDataProvider provider = getInitializedProvider();

        // when
        StringBuilder sb = new StringBuilder();
        try (IngestDataProvider.CloseableIterable<String> iterable = provider.getColumnIterable(0)) {
            for (String value : iterable) {
                sb.append(value);
            }
        }

        // then
        assertThat(sb.toString()).isEqualTo("12345678910");
    }

    // -------------------- LOGIC --------------------

    IngestDataProvider getProvider();

    default IngestDataProvider getInitializedProvider() {
        IngestDataProvider provider = getProvider();
        provider.initialize(createDataTable(), getTabFile());
        return provider;
    }

    default DataTable createDataTable() {
        DataTable table = new DataTable();
        table.setCaseQuantity(10L);
        table.setVarQuantity(3L);
        return table;
    }

    default File getTabFile() {
        try {
            return new File(IngestDataProviderTest.class.getClassLoader().getResource("ingest/simple.tab").toURI());
        } catch (Exception ex) {
            Assertions.fail("Failed to access resource", ex);
            return null;
        }
    }
}