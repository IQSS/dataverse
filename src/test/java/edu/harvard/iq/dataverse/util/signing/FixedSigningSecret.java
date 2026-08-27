package edu.harvard.iq.dataverse.util.signing;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.mockito.Mockito;

/** Test helper: an {@link ApiSigningSecretServiceBean} that serves a fixed secret. */
public final class FixedSigningSecret {

    private FixedSigningSecret() {
    }

    public static ApiSigningSecretServiceBean withSecret(String secret) {
        SettingsServiceBean settingsService = Mockito.mock(SettingsServiceBean.class);
        Mockito.when(settingsService.getValueForKey(Key.ApiSigningSecret)).thenReturn(secret);
        ApiSigningSecretServiceBean bean = new ApiSigningSecretServiceBean();
        bean.settingsService = settingsService;
        return bean;
    }
}
