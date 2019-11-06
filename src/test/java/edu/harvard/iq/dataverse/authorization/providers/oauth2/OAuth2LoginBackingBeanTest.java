package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2APTest;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginBackingBeanTest {
    
    OAuth2LoginBackingBean loginBackingBean = new OAuth2LoginBackingBean();
    
    @Mock
    AuthenticationServiceBean authenticationServiceBean;
    @Mock
    SystemConfig systemConfig;
    
    @BeforeEach
    void setUp() {
        this.loginBackingBean.authenticationSvc = this.authenticationServiceBean;
        this.loginBackingBean.systemConfig = this.systemConfig;
    }
    
    /**
     * TODO: this should be a parameterized test case testing all providers available
     */
    @Test
    void linkFor() {
        // given
        String idpId = "github";
        String redirectPage = "dataverse.xhtml"; // @see LoginPage.redirectPage
        String callbackURL = "oauth2/callback.xhtml";
        AbstractOAuth2AuthenticationProvider idp = new GitHubOAuth2APTest();
        
        // when
        when(this.authenticationServiceBean.getOAuth2Provider(idpId)).thenReturn(idp);
        when(this.systemConfig.getOAuth2CallbackUrl()).thenReturn(callbackURL);
        
        String link = loginBackingBean.linkFor(idpId, redirectPage);
        
        // then
        assertThat(link, notNullValue());
        assertThat(link, not(isEmptyString()));
        assertThat(link, StringContains.containsString(idp.getService(callbackURL).getAuthorizationUrl()));
    }
    
    @Test
    void createStateFailNullIdp() {
        // given
        AbstractOAuth2AuthenticationProvider idp = null;
        Optional<String> page = Optional.empty();
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            loginBackingBean.createState(idp, page);
        });
    }
}