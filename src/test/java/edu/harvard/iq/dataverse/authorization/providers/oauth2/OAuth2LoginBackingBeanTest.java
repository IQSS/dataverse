package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2APTest;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginBackingBeanTest {
    
    OAuth2LoginBackingBean loginBackingBean = new OAuth2LoginBackingBean();
    static AbstractOAuth2AuthenticationProvider testIdp = new GitHubOAuth2APTest();
    
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
    
    @ParameterizedTest(name = "\"{0}\" should be Optional.empty")
    @NullSource
    @EmptySource
    @ValueSource(strings = {"    ", "\t"})
    void parseStateFromRequestStateNullOrEmpty(String state) {
        assertThat(loginBackingBean.parseStateFromRequest(state), is(Optional.empty()));
    }
    
    @ParameterizedTest(name = "\"{0}\" should be Optional.empty")
    @ValueSource(strings = {"test", "test~", "test~test"})
    void parseStateFromRequestStateInvalidString(String state) {
        assertThat(loginBackingBean.parseStateFromRequest(state), is(Optional.empty()));
    }
    
    static Stream<Arguments> tamperedStates() {
        return Stream.of(
            // encrypted nonsense
            Arguments.of(testIdp.getId() + "~" + StringUtil.encrypt("test", testIdp.getClientSecret())),
            // expired timestamp < 0
            Arguments.of(testIdp.getId() + "~" + StringUtil.encrypt(testIdp.getId()+"~-1", testIdp.getClientSecret())),
            // expired timestamp (10 milliseconds to old...)
            Arguments.of(testIdp.getId() + "~" + StringUtil.encrypt(testIdp.getId()+"~"+(System.currentTimeMillis()-OAuth2LoginBackingBean.STATE_TIMEOUT-10), testIdp.getClientSecret()))
        );
    }
    @ParameterizedTest
    @MethodSource("tamperedStates")
    void parseStateFromRequestStateTampered(String state) {
        // when
        when(this.authenticationServiceBean.getOAuth2Provider(testIdp.getId())).thenReturn(testIdp);
        
        // then
        assertThat(loginBackingBean.parseStateFromRequest(state), is(Optional.empty()));
    }
    
    /**
     * Testing for side effect with proper parameters.
     */
    static Stream<Arguments> provideStates() {
        return Stream.of(
            Arguments.of(Optional.of("dataverse.xhtml"), true),
            Arguments.of(Optional.empty(), false)
        );
    }
    @ParameterizedTest
    @MethodSource("provideStates")
    void parseStateFromRequestStateValid(Optional<String> redirectPage, boolean present) {
        // given
        String idpId = testIdp.getId();
    
        // when
        when(this.authenticationServiceBean.getOAuth2Provider(idpId)).thenReturn(testIdp);
        String stateWithRedirect = loginBackingBean.createState(testIdp, redirectPage);
    
        // then
        assertThat(loginBackingBean.parseStateFromRequest(stateWithRedirect), is(Optional.of(testIdp)));
        assertThat(loginBackingBean.redirectPage.isPresent(), is(present));
        if (present) {
            assertThat(loginBackingBean.redirectPage.get(), equalTo(redirectPage.get()));
        }
    }
}