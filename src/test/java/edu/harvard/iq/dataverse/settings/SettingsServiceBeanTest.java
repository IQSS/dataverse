package edu.harvard.iq.dataverse.settings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsServiceBeanTest {
    
    @Nested
    class KeyEnumTest {
        static List<Arguments> parseTestParameters() {
            return List.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("    ", null),
                Arguments.of("foobar", null),
                Arguments.of("ShowMuteOptions", null),
                Arguments.of(":FooBar", null),
                Arguments.of(":ShowMuteOptions", SettingsServiceBean.Key.ShowMuteOptions)
            );
        }
        
        @MethodSource("parseTestParameters")
        @ParameterizedTest
        void testParse(String sut, SettingsServiceBean.Key expected) {
            assertEquals(expected, SettingsServiceBean.Key.parse(sut));
        }
        
        @Test
        void testToString() {
            // Make sure we test the intended behavior so it doesn't change by accident.
            assertEquals(":ShowMuteOptions", SettingsServiceBean.Key.ShowMuteOptions.toString());
        }
        
        @Test
        void testRoundtrip() {
            for (SettingsServiceBean.Key key : SettingsServiceBean.Key.values()) {
                assertEquals(key, SettingsServiceBean.Key.parse(key.toString()));
            }
        }
    }
}