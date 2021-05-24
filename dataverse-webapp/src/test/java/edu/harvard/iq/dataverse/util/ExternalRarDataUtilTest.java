package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalRarDataUtilTest {

    private ExternalRarDataUtil util = new ExternalRarDataUtil("", "", "-");

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should read size from output")
    void parseOutput() {
        // given & when
        long size = util.parseOutput(toLines("---------\n12345"));

        // then
        assertThat(size).isEqualTo(12345L);
    }

    @ParameterizedTest
    @DisplayName("Should return 0 on pathological outputs")
    @ValueSource(strings = {
            "\n",
            " \n ",
            "-----------------",
            "-----------------\nabc",
            "-----------------\n "
    })
    void parseOutput__erroneousOutputs(String output) {
        // given & when
        long size = util.parseOutput(toLines(output));

        // then
        assertThat(size).isEqualTo(0L);
    }

    // -------------------- PRIVATE --------------------

    private String[] toLines(String data) {
        return data.split("\n");
    }
}