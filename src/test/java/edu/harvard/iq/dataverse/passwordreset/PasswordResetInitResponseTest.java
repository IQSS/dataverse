package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordResetInitResponseTest {

    @Test
    public void testIsNonLocalLoginEnabled() {
        // TODO:
    }
}
