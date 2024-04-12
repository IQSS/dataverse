package edu.harvard.iq.dataverse.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.UserServiceBean;

public class UserListMakerTest {

  private UserServiceBean mockUserService;
  private UserListMaker userListMaker;

  @BeforeEach
  public void setUp() {
    mockUserService = mock(UserServiceBean.class);
    userListMaker = new UserListMaker(mockUserService);
    userListMaker.userService = mockUserService;
  }

  @AfterEach
  public void tearDown() {
    mockUserService = null;
    userListMaker = null;
  }

  @Test
  public void testGetTotalUserCount_oneSuperuser() {
    boolean superusers_only = true;
    Mockito.when(mockUserService.getSuperUserCount()).thenReturn(1L);
    long nrOfSuperUsers = userListMaker.getTotalUserCount(superusers_only);

    assertEquals(1, nrOfSuperUsers);
  }

  @Test
  public void testGetTotalUserCount_multipleSuperusers() {
    boolean superusers_only = true;
    Mockito.when(mockUserService.getSuperUserCount()).thenReturn(3L);
    long nrOfSuperUsers = userListMaker.getTotalUserCount(superusers_only);

    assertEquals(3, nrOfSuperUsers);
  }

  @Test
  public void testGetTotalUserCount_noSuperusers() {
    boolean superusers_only = true;
    Mockito.when(mockUserService.getSuperUserCount()).thenReturn(0L);
    long nrOfSuperUsers = userListMaker.getTotalUserCount(superusers_only);

    assertEquals(0, nrOfSuperUsers);
  }

  @Test
  public void testGetTotalUserCount_oneUser() {
    boolean superusers_only = false;
    Mockito.when(mockUserService.getUserCount(null)).thenReturn(1L);
    long nrOfUsers = userListMaker.getTotalUserCount(superusers_only);

    assertEquals(1, nrOfUsers);
  }

  @Test
  public void testGetTotalUserCount_multipleUsers() {
    boolean superusers_only = false;
    Mockito.when(mockUserService.getUserCount(null)).thenReturn(3L);
    long nrOfUsers = userListMaker.getTotalUserCount(superusers_only);

    assertEquals(3, nrOfUsers);
  }

  @Test
  public void testGetTotalUserCount_noUsers() {
    boolean superusers_only = false;
    Mockito.when(mockUserService.getUserCount(null)).thenReturn(0L);
    long nrOfUsers = userListMaker.getTotalUserCount(superusers_only);

    assertEquals(0, nrOfUsers);
  }

}
