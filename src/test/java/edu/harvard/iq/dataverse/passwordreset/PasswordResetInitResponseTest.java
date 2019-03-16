package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

public class PasswordResetInitResponseTest {

    private PasswordResetData passwordResetData = new PasswordResetData() {
        @Override
        public String getToken() {
            return "abcd";
        }
    };

    @Test
    public void testGeneratesValidResetUrl() {
        // TODO:
        fail();
    }
}
