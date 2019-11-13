package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2APTest;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.util.Faces;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.IOException;

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
        lenient().when(this.authenticationServiceBean.getOAuth2Provider(testIdp.getId())).thenReturn(testIdp);
    }
    
    /**
     * TODO: this should be a parameterized test case testing all providers available
     */
    @Test
    void linkFor() {
        // given
        String redirectPage = "dataverse.xhtml"; // @see LoginPage.redirectPage
        String callbackURL = "oauth2/callback.xhtml";
        
        // when
        when(this.systemConfig.getOAuth2CallbackUrl()).thenReturn(callbackURL);
        
        String link = loginBackingBean.linkFor(testIdp.getId(), redirectPage);
        
        // then
        assertThat(link, notNullValue());
        assertThat(link, not(isEmptyString()));
        assertThat(link, StringContains.containsString(testIdp.getService(callbackURL).getAuthorizationUrl()));
    }
    
    @Nested
    @DisplayName("Tests for exchangeCodeForToken()")
    class ecft {
        @Mock FacesContext facesContextMock;
        @Mock ExternalContext externalContextMock;
        @Mock Flash flashMock;
        @Mock HttpServletRequest requestMock;
        @Mock BufferedReader reader;
        
        @BeforeEach
        void setUp() throws IOException {
            // mock FacesContext to make the method testable
            Faces.setContext(facesContextMock);
            when(facesContextMock.getExternalContext()).thenReturn(externalContextMock);
            lenient().when(externalContextMock.getFlash()).thenReturn(flashMock);
            when(externalContextMock.getRequest()).thenReturn(requestMock);
            when(requestMock.getReader()).thenReturn(reader);
            when(requestMock.getParameter("state")).thenReturn(loginBackingBean.createState(testIdp, Optional.of("/dataverse.xhtml")));
        }
        
        @Test
        void noCode() {
            assertDoesNotThrow(() -> loginBackingBean.exchangeCodeForToken());
            assertThat(loginBackingBean.getError(), Matchers.isA(OAuth2Exception.class));
        }
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
        String stateWithRedirect = loginBackingBean.createState(testIdp, redirectPage);
    
        // when & then
        assertThat(loginBackingBean.parseStateFromRequest(stateWithRedirect), is(Optional.of(testIdp)));
        assertThat(loginBackingBean.redirectPage.isPresent(), is(present));
        if (present) {
            assertThat(loginBackingBean.redirectPage.get(), equalTo(redirectPage.get()));
        }
    }
}