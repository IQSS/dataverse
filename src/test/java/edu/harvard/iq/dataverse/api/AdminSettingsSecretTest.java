package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The API signing secret may be stored by admins, but never a weak (short) one. */
// The endpoints under test still use the deprecated SettingsServiceBean.set variants; the
// verifications must reference the same methods.
@SuppressWarnings({"deprecation", "removal", "java:S5738"})
class AdminSettingsSecretTest {

    private static final String SECRET_KEY = SettingsServiceBean.Key.ApiSigningSecret.toString();

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = new Admin();
        admin.settingsSvc = mock(SettingsServiceBean.class);
    }

    @Test
    void shortSigningSecretIsRejected() {
        Response response = admin.putSetting(SECRET_KEY, "too-short");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verify(admin.settingsSvc, never()).set(anyString(), anyString());
    }

    @Test
    void longEnoughSigningSecretIsStored() {
        String secret = "long-enough-signing-secret-0123456789";

        Response response = admin.putSetting(SECRET_KEY, secret);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(admin.settingsSvc).set(SECRET_KEY, secret);
    }

    @Test
    void shortSigningSecretIsRejectedOnTheLocalizedEndpointToo() {
        Response response = admin.putSettingLang(SECRET_KEY, "en", "too-short");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verify(admin.settingsSvc, never()).set(anyString(), anyString(), anyString());
    }

    @Test
    void otherSettingsAreNotLengthChecked() {
        Response response = admin.putSetting(":SystemEmail", "a@b.cd");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(admin.settingsSvc).set(":SystemEmail", "a@b.cd");
    }
}
