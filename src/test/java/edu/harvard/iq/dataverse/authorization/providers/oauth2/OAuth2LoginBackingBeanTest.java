package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2APTest;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.util.Faces;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginBackingBeanTest {
    
    OAuth2LoginBackingBean loginBackingBean = new OAuth2LoginBackingBean();
    @Spy static AbstractOAuth2AuthenticationProvider testIdp = new GitHubOAuth2APTest();
    
    @Mock AuthenticationServiceBean authenticationServiceBean;
    @Mock SystemConfig systemConfig;
    
    Clock constantClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    
    /**
     * Save the current JSF context to reset after all test cases done.
     * Without doing this, many tests will fail with NPEs while fetching the bundle,
     * when the locale is received from the context.
     */
    static FacesContext save = Faces.getContext();
    @AfterAll
    static void cleanupFaces() {
        Faces.setContext(save);
    }
    
    @BeforeEach
    void setUp() {
        
        // create a fixed (not running) clock for testing.
        // --> reset the clock for every test to the fixed time, to avoid sideeffects of a DeLorean time travel
        this.loginBackingBean.clock = constantClock;
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
        @Mock OAuth2FirstLoginPage newAccountPage;
        @Mock DataverseSession session;
        @Mock OAuth2TokenDataServiceBean oauth2Tokens;
        Optional<String> redirect = Optional.of("/hellotest");
        String state;
        
        @BeforeEach
        void setUp() throws IOException {
            loginBackingBean.newAccountPage = this.newAccountPage;
            loginBackingBean.session = this.session;
            loginBackingBean.oauth2Tokens = this.oauth2Tokens;
            
            // mock FacesContext to make the method testable
            Faces.setContext(facesContextMock);
            when(facesContextMock.getExternalContext()).thenReturn(externalContextMock);
            when(externalContextMock.getRequest()).thenReturn(requestMock);
            lenient().when(externalContextMock.getFlash()).thenReturn(flashMock);
            lenient().when(requestMock.getReader()).thenReturn(reader);
            
            // Save the state as we need it for injection (necessary because of PKCE support)
            state = loginBackingBean.createState(testIdp, this.redirect);
            doReturn(state).when(requestMock).getParameter("state");
            
            // travel in time at least 10 milliseconds (remote calls & redirects are much likely longer)
            // (if not doing this tests become flaky on fast machinas)
            loginBackingBean.clock = Clock.offset(constantClock, Duration.ofMillis(10));
        }
        
        @Test
        void noCode() {
            assertDoesNotThrow(() -> loginBackingBean.exchangeCodeForToken());
            assertThat(loginBackingBean.getError(), Matchers.isA(OAuth2Exception.class));
        }
        
        @Test
        void newUser() throws Exception {
            // GIVEN
            String code = "randomstring";
            OAuth2UserRecord userRecord = mock(OAuth2UserRecord.class);
            String newUserRedirect = "/oauth2/firstLogin.xhtml";
            
            // fake the code received from the provider
            when(requestMock.getParameter("code")).thenReturn(code);
            // let's deep-fake the result of getUserRecord()
            doReturn(userRecord).when(testIdp).getUserRecord(code, state, null);
    
            // WHEN (& then)
            // capture the redirect target from the faces context
            ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(externalContextMock).redirect(redirectUrlCaptor.capture());
            
            assertDoesNotThrow(() -> loginBackingBean.exchangeCodeForToken());
            
            // THEN
            // verify that the user object is passed on to the first login page
            verify(newAccountPage, times(1)).setNewUser(userRecord);
            // verify that the user is redirected to the first login page
            assertThat(redirectUrlCaptor.getValue(), equalTo(newUserRedirect));
        }
    
        @Test
        void existingUser() throws Exception {
            // GIVEN
            String code = "randomstring";
            OAuth2UserRecord userRecord = mock(OAuth2UserRecord.class);
            UserRecordIdentifier userIdentifier = mock(UserRecordIdentifier.class);
            AuthenticatedUser user = mock(AuthenticatedUser.class);
            OAuth2TokenData tokenData = mock(OAuth2TokenData.class);
        
            // fake the code received from the provider
            when(requestMock.getParameter("code")).thenReturn(code);
            // let's deep-fake the result of getUserRecord()
            doReturn(userRecord).when(testIdp).getUserRecord(code, state, null);
            doReturn(tokenData).when(userRecord).getTokenData();
            // also fake the result of the lookup in the auth service
            doReturn(userIdentifier).when(userRecord).getUserRecordIdentifier();
            doReturn(user).when(authenticationServiceBean).lookupUser(userIdentifier);
        
            // WHEN (& then)
            // capture the redirect target from the faces context
            ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(externalContextMock).redirect(redirectUrlCaptor.capture());
    
            assertDoesNotThrow(() -> loginBackingBean.exchangeCodeForToken());
        
            // THEN
            // verify session handling: set the looked up user
            verify(session, times(1)).setUser(user);
            // verify that the user is redirected to the first login page
            assertThat(redirectUrlCaptor.getValue(), equalTo(redirect.get()));
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
        // travel in time at least 10 milliseconds (remote calls & redirects are much likely longer)
        // (if not doing this tests become flaky on fast machinas)
        loginBackingBean.clock = Clock.offset(constantClock, Duration.ofMillis(10));
    
        // when & then
        assertThat(loginBackingBean.parseStateFromRequest(stateWithRedirect), is(Optional.of(testIdp)));
        assertThat(loginBackingBean.redirectPage.isPresent(), is(present));
        if (present) {
            assertThat(loginBackingBean.redirectPage.get(), equalTo(redirectPage.get()));
        }
    }
}