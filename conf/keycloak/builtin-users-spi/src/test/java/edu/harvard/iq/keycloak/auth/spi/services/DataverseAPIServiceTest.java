package edu.harvard.iq.keycloak.auth.spi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Disabled
class DataverseAPIServiceTest {

    private HttpURLConnection connectionMock;
    private URL urlMock;
    private DataverseAPIService sut;

    @BeforeEach
    void setUp() throws Exception {
        connectionMock = mock(HttpURLConnection.class);
        urlMock = mock(URL.class);
        when(urlMock.openConnection()).thenReturn(connectionMock);

        sut = new DataverseAPIService(urlMock);
    }

    @Test
    void canLogInAsBuiltinUser_validCredentials() throws Exception {
        when(connectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        boolean result = sut.canLogInAsBuiltinUser("validUser", "validPass");
        assertTrue(result);
    }

    @Test
    void canLogInAsBuiltinUser_invalidCredentials() throws Exception {
        when(connectionMock.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
        boolean result = sut.canLogInAsBuiltinUser("invalidUser", "invalidPass");
        assertFalse(result);
    }

    @Test
    void canLogInAsBuiltinUser_apiError() throws Exception {
        when(urlMock.openConnection()).thenThrow(new IOException("Connection error"));
        boolean result = sut.canLogInAsBuiltinUser("errorUser", "errorPass");
        assertFalse(result);
    }
}
