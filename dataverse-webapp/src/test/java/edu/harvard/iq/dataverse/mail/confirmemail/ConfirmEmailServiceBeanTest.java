package edu.harvard.iq.dataverse.mail.confirmemail;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.ConfirmEmailData;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.security.AuthProvider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.when;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmEmailServiceBeanTest {

    private Map<String, User> testUsers = new HashMap<>();
    private Clock clock = Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), UTC);

    @InjectMocks
    ConfirmEmailServiceBean confirmEmailServiceBean;

    @Mock
    SystemConfig systemConfig;

    @Mock
    AuthenticationServiceBean authenticationServiceBean;

    @Mock
    EntityManager em;

    @BeforeEach
    void setUp() {
        AuthenticatedUser superAdmin = new AuthenticatedUser();
        superAdmin.setSuperuser(true);

        testUsers.put("superAdmin", superAdmin);

        AuthenticatedUser confirmedMail = new AuthenticatedUser();
        confirmedMail.setEmailConfirmed(new Timestamp(clock.millis()));

        testUsers.put("confirmedMail", confirmedMail);
        testUsers.put("unconfirmedMail", new AuthenticatedUser());
        testUsers.put("guest", GuestUser.get());

        // Unfortunately following mocks have to be established in order to allow
        // the method hasEffectivelyUnconfirmedMail to work properly
        TypedQuery<ConfirmEmailData> typedQuery = Mockito.mock(TypedQuery.class);
        lenient().when(em.createNamedQuery(anyString(), any(Class.class))).thenReturn(typedQuery);
        AuthenticationProvider authProvider = Mockito.mock(AuthenticationProvider.class);
        lenient().when(authenticationServiceBean.lookupProvider(any())).thenReturn(authProvider);
        lenient().when(authProvider.isEmailVerified()).thenReturn(false);
    }

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @DisplayName("Should return effective status of mail confirmation when unconfirmed mail restriction mode is enabled")
    @CsvSource({"superAdmin, false",
            "confirmedMail, false",
            "unconfirmedMail, true",
            "guest, false"})
    void hasEffectivelyUnconfirmedMail__modeEnabled(String userKey, boolean expected) {
        // given
        when(systemConfig.isUnconfirmedMailRestrictionModeEnabled()).thenReturn(true);

        // when
        boolean result = confirmEmailServiceBean.hasEffectivelyUnconfirmedMail(testUsers.get(userKey));

        // then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Should return false when unconfirmed mail restriction mode is disabled")
    @CsvSource({"superAdmin, false",
            "confirmedMail, false",
            "unconfirmedMail, false",
            "guest, false"})
    void hasEffectivelyUnconfirmedMail__modeDisabled(String userKey, boolean expected) {
        // given
        when(systemConfig.isUnconfirmedMailRestrictionModeEnabled()).thenReturn(false);

        // when
        boolean result = confirmEmailServiceBean.hasEffectivelyUnconfirmedMail(testUsers.get(userKey));

        // then
        assertThat(result).isEqualTo(expected);
    }
}