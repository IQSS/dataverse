package edu.harvard.iq.dataverse.util.signing;

/** Test helper: an {@link ApiSigningSecretServiceBean} that serves a fixed secret. */
public final class FixedSigningSecret {

    private FixedSigningSecret() {
    }

    public static ApiSigningSecretServiceBean withSecret(String secret) {
        ApiSigningSecretServiceBean bean = new ApiSigningSecretServiceBean();
        bean.secret = secret;
        return bean;
    }
}
