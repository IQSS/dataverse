package edu.harvard.iq.dataverse.util.signing;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@LocalJvmSettings
class ApiSigningSecretServiceBeanTest {

    private static final String CONFIGURED_SECRET = "long-enough-signing-secret-0123456789";

    private ApiSigningSecretServiceBean bean() {
        ApiSigningSecretServiceBean bean = new ApiSigningSecretServiceBean();
        bean.init();
        return bean;
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = CONFIGURED_SECRET)
    void usesTheConfiguredSecret() {
        assertEquals(CONFIGURED_SECRET, bean().getSecret());
    }

    @Test
    void generatesATemporarySecretWhenNoneIsConfigured() {
        ApiSigningSecretServiceBean bean = bean();
        String secret = bean.getSecret();
        // 36  random bytes, Base64-encoded
        assertEquals(ApiSigningSecretServiceBean.MIN_SECRET_LENGTH, Base64.getDecoder().decode(secret).length);
        // stable while the server runs, but different for every server start
        assertEquals(secret, bean.getSecret());
        assertNotEquals(secret, bean().getSecret());
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = "too-short")
    void ignoresAConfiguredSecretThatIsTooShort() {
        String secret = bean().getSecret();
        assertNotEquals("too-short", secret);
        assertEquals(ApiSigningSecretServiceBean.MIN_SECRET_LENGTH, Base64.getDecoder().decode(secret).length);
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNING_SECRET, value = CONFIGURED_SECRET)
    void signingKeyIsSecretPlusApiToken() {
        assertEquals(CONFIGURED_SECRET + "token-123", bean().getSigningKey("token-123"));
    }
}
