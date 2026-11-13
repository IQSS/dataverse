package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlOriginUtilTest {

    @ParameterizedTest
    @CsvSource({
            "https://demo.dataverse.org,          https://demo.dataverse.org",
            "https://demo.dataverse.org/,         https://demo.dataverse.org",
            "https://demo.dataverse.org/a/b?c=d,  https://demo.dataverse.org",
            "HTTPS://DEMO.Dataverse.ORG,          https://demo.dataverse.org",
            "  https://demo.dataverse.org  ,      https://demo.dataverse.org",
            "https://demo.dataverse.org:443,      https://demo.dataverse.org",
            "http://demo.dataverse.org:80,        http://demo.dataverse.org",
            "http://localhost:8080,               http://localhost:8080",
            "https://demo.dataverse.org:8443,     https://demo.dataverse.org:8443",
    })
    void testToOrigin_normalizesToSchemeHostPort(String input, String expected) {
        assertEquals(expected, UrlOriginUtil.toOrigin(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "null",                       // what browsers send as Origin under Referrer-Policy: no-referrer
            "demo.dataverse.org",         // no scheme
            "https://",                   // no host
            "/api/access/datafile/1",
            "http://[not a host]",
    })
    void testToOrigin_failsClosedOnUnusableInput(String input) {
        assertNull(UrlOriginUtil.toOrigin(input));
    }

    @Test
    void testToOrigin_userInfoDoesNotMasqueradeAsHost() {
        assertEquals("https://evil.example.com",
                UrlOriginUtil.toOrigin("https://demo.dataverse.org@evil.example.com"));
    }

    @Test
    void testToOrigin_suffixHostIsNotTheSiteOrigin() {
        assertEquals("https://demo.dataverse.org.evil.example.com",
                UrlOriginUtil.toOrigin("https://demo.dataverse.org.evil.example.com/x"));
    }
}
