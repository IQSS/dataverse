package edu.harvard.iq.dataverse.util.signing;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Test helper: an {@link ApiSigningSecretServiceBean} that serves a fixed secret. */
public final class FixedSigningSecret {

    private FixedSigningSecret() {
    }

    public static ApiSigningSecretServiceBean withSecret(String secret) {
        SettingsServiceBean settingsService = mock(SettingsServiceBean.class);
        when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn(secret);
        ApiSigningSecretServiceBean bean = new ApiSigningSecretServiceBean();
        bean.settingsService = settingsService;
        return bean;
    }
}
