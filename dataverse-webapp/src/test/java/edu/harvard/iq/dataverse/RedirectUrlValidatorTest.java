package edu.harvard.iq.dataverse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectUrlValidatorTest {
    LoginPage loginPage = new LoginPage();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://www.wp.pl",
            "http://wp.pl",
            "https://www.wp.pl",
            "https://wp.pl",
            "https://example.com/foo/bar?k1=v1&k2=v2"
    })
    void validURLs(String url) {
        assertTrue(loginPage.validateIsRedirectUrlAnExternalResource(url), "URL should be valid: " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "www.wp.pl",
            "wp.pl",
            "invalid-url",
            "http://.com",
            "http://",
            "https:///",
            "ftp://ftp.example"
    })
    void invalidURLs(String url) {
        assertFalse(loginPage.validateIsRedirectUrlAnExternalResource(url), "URL should be invalid: " + url);
    }
}