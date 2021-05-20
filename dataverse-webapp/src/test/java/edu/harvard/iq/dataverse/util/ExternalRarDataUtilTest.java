package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExternalRarDataUtilTest {

    @Mock
    private Path empty;

    @BeforeEach
    void setUp() {
        Mockito.when(empty.toString()).thenReturn("");
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should read size from output")
    void checkRarExternally() {
        // given
        ExternalRarDataUtil util = createUtilWithGivenOutput("---------\n12345");

        // when
        long size = util.checkRarExternally(empty);

        // then
        assertThat(size).isEqualTo(12345L);
    }

    @ParameterizedTest
    @DisplayName("Should return 0 on pathological outputs")
    @ValueSource(strings = {
            "\n",
            "-----------------",
            "-----------------\nabc",
            "-----------------\n"
    })
    void checkRarExternally__erroneousOutputs(String output) {
        // given
        ExternalRarDataUtil util = createUtilWithGivenOutput(output);

        // when
        long size = util.checkRarExternally(empty);

        // then
        assertThat(size).isEqualTo(0L);
    }

    // -------------------- PRIVATE --------------------

    private ExternalRarDataUtil createUtilWithGivenOutput(String output) {
        // We use echo instead of some rar util in order to create
        // desired output.
        return new ExternalRarDataUtil("echo", output, "-");
    }
}