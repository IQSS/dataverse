package edu.harvard.iq.dataverse.authorization.providers.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailData;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;

public class DataverseUserPageTest {

  private DataverseUserPage dataverseUserPage;
  private AuthenticatedUser user;
  private ConfirmEmailServiceBean mockConfirmEmailService;
  private AuthenticationProvider mockAuthProvider;
  private AuthenticationServiceBean mockAuthService;

  @BeforeEach
  public void setUp() {
    dataverseUserPage = new DataverseUserPage();
    user = new AuthenticatedUser();
    mockConfirmEmailService = mock(ConfirmEmailServiceBean.class);
    mockAuthProvider = mock(AuthenticationProvider.class);
    mockAuthService = mock(AuthenticationServiceBean.class);
  }

  @AfterEach
  public void tearDown() {
    dataverseUserPage = null;
    user = null;
    mockConfirmEmailService = null;
    mockAuthProvider = null;
    mockAuthService = null;
  }

  // provides inputs to testShowVerifyEmailButton()
  private static Stream<Arguments> provider_testShowVerifyEmailButton() {
    return Stream.of(
        Arguments.of(true, null, false, false, 1),
        Arguments.of(true, null, false, true, 2),
        Arguments.of(true, null, true, false, 3),
        Arguments.of(true, null, true, true, 4),
        Arguments.of(false, new Timestamp(0), false, false, 5), 
        Arguments.of(false, new Timestamp(0), false, true, 6),
        Arguments.of(false, new Timestamp(0), true, false, 7),
        Arguments.of(false, new Timestamp(0), true, true, 8)
    );
  }

  @ParameterizedTest
  @MethodSource("provider_testShowVerifyEmailButton")
  public void testShowVerifyEmailButton(boolean expected, Timestamp emailConfirmed, boolean confirmEmailData, boolean emailVerified, int counter) {
    user.setEmailConfirmed(emailConfirmed);
    
    dataverseUserPage.setCurrentUser(user);

    if (confirmEmailData) {
      Mockito.when(mockConfirmEmailService.findSingleConfirmEmailDataByUser(user))
          .thenReturn(new ConfirmEmailData(new AuthenticatedUser(), 1));
    } else {
      Mockito.when(mockConfirmEmailService.findSingleConfirmEmailDataByUser(user)).thenReturn(null);
    }
    
    if(emailConfirmed != null){
        
    Mockito.when(mockConfirmEmailService.hasVerifiedEmail(user))
          .thenReturn(true);
    } else {
      Mockito.when(mockConfirmEmailService.hasVerifiedEmail(user)).thenReturn(false);
    }

    Mockito.when(mockAuthProvider.isEmailVerified()).thenReturn(emailVerified);
    Mockito.when(mockAuthService.lookupProvider(user)).thenReturn(mockAuthProvider);

    dataverseUserPage.confirmEmailService = mockConfirmEmailService;
    dataverseUserPage.authenticationService = mockAuthService;

    assertEquals(expected, dataverseUserPage.showVerifyEmailButton());
  }

}
