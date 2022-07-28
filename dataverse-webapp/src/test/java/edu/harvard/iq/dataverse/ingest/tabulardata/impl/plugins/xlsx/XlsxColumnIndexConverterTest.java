package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XlsxColumnIndexConverterTest {

    private XlsxColumnIndexConverter converter = new XlsxColumnIndexConverter();

    @ParameterizedTest
    @CsvSource(value = {
            "A, 0",
            "J, 9",
            "Z, 25",
            "AB, 27",
            "ZY, 700",
            "AAB, 703"
        })
    void columnToIndex(String column, Integer index) {
        // when
        int result = converter.columnToIndex(column);

        // then
        assertThat(result).isEqualTo(index);
    }

    @Test
    void columnToIndex__lowercase() {
        // when
        int result = converter.columnToIndex("aa");

        // then
        assertThat(result).isEqualTo(26);
    }

    @Test
    void columnToIndex__throwOnDisallowedLetters() {
        // when & then
        assertThatThrownBy(() -> converter.columnToIndex("ĄŻ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1, B",
            "10, K",
            "26, AA",
            "701, ZZ",
            "702, AAA"
    })
    void indexToColumn(Integer index, String column) {
        // when
        String result = converter.indexToColumn(index);

        // then
        assertThat(result).isEqualTo(column);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12, 123, 12345, 123456, 1234567})
    void checkComposition__index(int index) {
        // when
        int result = converter.columnToIndex(converter.indexToColumn(index));

        // then
        assertThat(result).isEqualTo(index);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "AB", "ABC", "ABCD", "ZZZ", "ZAA", "AAA"})
    void checkComposition__column(String column) {
        // when
        String result = converter.indexToColumn(converter.columnToIndex(column));

        // then
        assertThat(result).isEqualTo(column);
    }
}