package edu.harvard.iq.dataverse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

public class DatasetVersionServiceBeanTest {

  private DatasetVersionServiceBean datasetVersionServiceBean;

  @BeforeEach
  public void setUp() {
    this.datasetVersionServiceBean = new DatasetVersionServiceBean();
    
  }

  @AfterEach
  public void tearDown() {
    this.datasetVersionServiceBean = null;
  }

  @Test
  public void testGetContributorsNamesWithoutContributors() {
    // create mock auth service using Mockito
    AuthenticationServiceBean mockAuthService = mock(AuthenticationServiceBean.class);
    Mockito.when(mockAuthService.getAuthenticatedUser(null)).thenReturn(null);

    // make DatasetVersionServiceBean use the mockAuthService
    this.datasetVersionServiceBean.authService = mockAuthService;

    // DatesetVersion without contributors
    DatasetVersion datasetVersion = new DatasetVersion();

    String contribNames = this.datasetVersionServiceBean.getContributorsNames(datasetVersion);

    String emptyString = new String("");

    assertEquals(emptyString, contribNames);
  }

  @Test
  public void testGetContributorsNamesWithOneContributor() {
    // create mock users
    AuthenticatedUser user1 = new AuthenticatedUser();
    AuthenticatedUserDisplayInfo info1 = new AuthenticatedUserDisplayInfo("Albert", "Einstein", "email", "affiliation", "position");
    user1.applyDisplayInfo(info1);
    user1.setUserIdentifier("alberteinstein");

    // create mock auth service using Mockito
    AuthenticationServiceBean mockAuthService = mock(AuthenticationServiceBean.class);
    Mockito.when(mockAuthService.getAuthenticatedUser("alberteinstein")).thenReturn(user1);

    // make DatasetVersionServiceBean use the mockAuthService
    this.datasetVersionServiceBean.authService = mockAuthService;

    // DatasetVersion with one contributor
    DatasetVersionUser dataSetVersionUser1 = new DatasetVersionUser();
    dataSetVersionUser1.setAuthenticatedUser(user1);

    DatasetVersion datasetVersion = new DatasetVersion();
    datasetVersion.setUserDatasets(Arrays.asList(dataSetVersionUser1));

    String contribNames = this.datasetVersionServiceBean.getContributorsNames(datasetVersion);

    assertEquals("Albert Einstein", contribNames);
  }

  @Test
  public void testGetContributorsNamesWithMultipleContributors() {
    // create mock users
    AuthenticatedUser user1 = new AuthenticatedUser();
    AuthenticatedUserDisplayInfo info1 = new AuthenticatedUserDisplayInfo("Albert", "Einstein", "email", "affiliation", "position");
    user1.applyDisplayInfo(info1);
    user1.setUserIdentifier("alberteinstein");

    AuthenticatedUser user2 = new AuthenticatedUser();
    AuthenticatedUserDisplayInfo info2 = new AuthenticatedUserDisplayInfo("Nikola", "Tesla", "email", "affiliation", "position");
    user2.applyDisplayInfo(info2);
    user2.setUserIdentifier("nikolatesla");

    // create mock auth service using Mockito
    AuthenticationServiceBean mockAuthService = mock(AuthenticationServiceBean.class);
    Mockito.when(mockAuthService.getAuthenticatedUser("alberteinstein")).thenReturn(user1);
    Mockito.when(mockAuthService.getAuthenticatedUser("nikolatesla")).thenReturn(user2);

    // make DatasetVersionServiceBean use the mockAuthService
    this.datasetVersionServiceBean.authService = mockAuthService;

    // DatasetVersion with more than one contributor
    DatasetVersionUser dataSetVersionUser1 = new DatasetVersionUser();
    dataSetVersionUser1.setAuthenticatedUser(user1);

    DatasetVersionUser dataSetVersionUser2 = new DatasetVersionUser();
    dataSetVersionUser2.setAuthenticatedUser(user2);

    DatasetVersion datasetVersion = new DatasetVersion();
    datasetVersion.setUserDatasets(Arrays.asList(dataSetVersionUser1, dataSetVersionUser2));

    String contribNames = this.datasetVersionServiceBean.getContributorsNames(datasetVersion);

    assertEquals("Albert Einstein, Nikola Tesla", contribNames);
  }

}