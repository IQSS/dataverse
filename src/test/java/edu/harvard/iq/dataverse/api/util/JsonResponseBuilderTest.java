package edu.harvard.iq.dataverse.api.util;

import edu.harvard.iq.dataverse.api.ApiBlockingFilter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonResponseBuilderTest {
    
    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {
        "test",
        ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM+"=supersecret",
        ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM+"=supersecret&hello=1234",
        "hello=1234&"+ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM+"=supersecret",
        "hello=1234&"+ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM+"=supersecret&test=1234"})
    void testMaskingOriginalURL(String query) {
        HttpServletRequest test = Mockito.mock(HttpServletRequest.class);
        when(test.getQueryString()).thenReturn(query);
        assertFalse(JsonResponseBuilder.getOriginalURL(test).contains("supersecret"));
    }
}