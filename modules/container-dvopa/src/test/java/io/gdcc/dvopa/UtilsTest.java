package io.gdcc.dvopa;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {
    
    @ParameterizedTest
    @ValueSource(strings = {
        "alpine",
        "alpine:latest",
        "test/alpine",
        "alpine:3.7",
        "docker.example.com/gmr/alpine:3.7",
        "docker.example.com:5000/alpine:latest",
        "docker.example.com:5000/gmr/alpine:latest",
        "pse/anabroker:latest",
        "ubuntu@sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f",
        "docker.example.com:5000/ubuntu@sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f",
        "docker.example.com:5000/test/ubuntu@sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f"
    })
    void validateContainerImageNames(String sut) {
        assertTrue(Utils.validateImage(sut));
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "_/alpine",
        "_/alpine:latest",
        "alpine:my+invalid+tag",
        "alpine:my/invalid/tag"
    })
    void failingContainerImageNames(String sut) {
        assertFalse(Utils.validateImage(sut));
    }
    
    @ParameterizedTest
    @CsvSource(textBlock = """
        alpine:3.7,                                                                     3.7
        docker.example.com/gmr/alpine:3.7,                                              3.7
        docker.example.com:5000/alpine:latest,                                          latest
        docker.example.com:5000/gmr/alpine:latest,                                      latest
        pse/anabroker:latest,                                                           latest
        ubuntu@sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f, sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f
        docker.example.com:5000/ubuntu@sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f, sha256:3235326357dfb65f1781dbc4df3b834546d8bf914e82cce58e6e6b676e23ce8f
        """
    )
    void getTagNameFromImage(String sut, String expected) {
        assertEquals(expected, Utils.getTagFromImage(sut));
    }
}