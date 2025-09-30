package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserNotificationServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<UserNotification> query;

    @InjectMocks
    private UserNotificationServiceBean userNotificationService;

    @BeforeEach
    void setUp() {
        when(em.createQuery(anyString(), eq(UserNotification.class))).thenReturn(query);
    }

    @Test
    void testFindByUser_withoutPagination() {
        // Arrange
        Long userId = 1L;
        List<UserNotification> expectedNotifications = Arrays.asList(new UserNotification(), new UserNotification());
        when(query.getResultList()).thenReturn(expectedNotifications);

        // Act
        List<UserNotification> actualNotifications = userNotificationService.findByUser(userId, false, null, null);

        // Assert
        assertEquals(2, actualNotifications.size());
        verify(query).setParameter("userId", userId);
        verify(query).setParameter("onlyUnread", false);
        // Verify that pagination methods were never called
        verify(query, never()).setFirstResult(anyInt());
        verify(query, never()).setMaxResults(anyInt());
    }

    @Test
    void testFindByUser_withOnlyUnread() {
        // Arrange
        Long userId = 1L;
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Act
        userNotificationService.findByUser(userId, true, null, null);

        // Assert
        verify(query).setParameter("userId", userId);
        verify(query).setParameter("onlyUnread", true);
    }

    @Test
    void testFindByUser_withPagination() {
        // Arrange
        Long userId = 1L;
        int limit = 10;
        int offset = 20;
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Act
        userNotificationService.findByUser(userId, false, limit, offset);

        // Assert
        verify(query).setParameter("userId", userId);
        verify(query).setParameter("onlyUnread", false);
        // Verify that pagination methods were called with the correct values
        verify(query).setFirstResult(offset);
        verify(query).setMaxResults(limit);
    }

    @Test
    void testFindByUser_withOnlyLimit() {
        // Arrange
        Long userId = 1L;
        int limit = 5;
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Act
        userNotificationService.findByUser(userId, false, limit, null);

        // Assert
        verify(query).setMaxResults(limit);
        verify(query, never()).setFirstResult(anyInt());
    }

    @Test
    void testFindByUser_withOnlyOffset() {
        // Arrange
        Long userId = 1L;
        int offset = 15;
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Act
        userNotificationService.findByUser(userId, false, null, offset);

        // Assert
        verify(query).setFirstResult(offset);
        verify(query, never()).setMaxResults(anyInt());
    }
}
