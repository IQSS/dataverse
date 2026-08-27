package edu.harvard.iq.dataverse.util.signing;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiSigningSecretServiceBeanTest {

    private SettingsServiceBean settingsService;
    private ApiSigningSecretServiceBean bean;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsServiceBean.class);
        bean = new ApiSigningSecretServiceBean();
        bean.settingsService = settingsService;
    }

    @Test
    void generatesAndStoresSecretWhenAbsent() {
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn(null);

        String secret = bean.getSecret();

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(settingsService).setValueForKey(eq(Key.ApiSigningSecret), stored.capture());
        assertEquals(stored.getValue(), secret);
        // 32 random bytes, Base64-encoded
        assertEquals(32, Base64.getDecoder().decode(secret).length);
    }

    @Test
    void reusesStoredSecretWithoutRegenerating() {
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn("stored-secret");

        assertEquals("stored-secret", bean.getSecret());
        verify(settingsService, never()).setValueForKey(any(), any());
    }

    @Test
    void concurrentGenerationLoserAdoptsTheWinnersSecret() {
        // First read: nothing stored. Store fails (another node won the race). Re-read: winner's value.
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn(null, "winner-secret");
        when(settingsService.setValueForKey(eq(Key.ApiSigningSecret), any()))
                .thenThrow(new PersistenceException("duplicate key"));

        assertEquals("winner-secret", bean.getSecret());
    }

    @Test
    void failsLoudlyWhenSecretCannotBeStoredOrRead() {
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn(null);
        when(settingsService.setValueForKey(eq(Key.ApiSigningSecret), any()))
                .thenThrow(new PersistenceException("db down"));

        assertThrows(IllegalStateException.class, () -> bean.getSecret());
    }

    @Test
    void signingKeyIsSecretPlusApiToken() {
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn("secret");

        assertEquals("secrettoken-123", bean.getSigningKey("token-123"));
    }

    @Test
    void generatedSecretsDiffer() {
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn(null);

        assertNotEquals(bean.getSecret(), bean.getSecret());
    }
}
