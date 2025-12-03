package edu.harvard.iq.dataverse;

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
import static org.mockito.ArgumentMatchers.contains;
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

    @Mock
    private TypedQuery<Long> countQuery;

    @InjectMocks
    private UserNotificationServiceBean userNotificationService;


    private void setupFindByUserMock() {
        when(em.createQuery(anyString(), eq(UserNotification.class))).thenReturn(query);
    }

    @Test
    void testFindByUser_withoutPagination() {
        // Arrange
        setupFindByUserMock();
        Long userId = 1L;
        List<UserNotification> expectedNotifications = Arrays.asList(new UserNotification(), new UserNotification());
        when(query.getResultList()).thenReturn(expectedNotifications);

        // Act
        List<UserNotification> actualNotifications = userNotificationService.findByUser(userId, false, null, null);

        // Assert
        assertEquals(2, actualNotifications.size());
        verify(query).setParameter("userId", userId);
        verify(query).setParameter("onlyUnread", false);
        verify(query, never()).setFirstResult(anyInt());
        verify(query, never()).setMaxResults(anyInt());
    }

    @Test
    void testFindByUser_withOnlyUnread() {
        // Arrange
        setupFindByUserMock();
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
        setupFindByUserMock();
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
        setupFindByUserMock();
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
        setupFindByUserMock();
        Long userId = 1L;
        int offset = 15;
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Act
        userNotificationService.findByUser(userId, false, null, offset);

        // Assert
        verify(query).setFirstResult(offset);
        verify(query, never()).setMaxResults(anyInt());
    }

    @Test
    void testFindTotalCountByUser_nullUserId() {
        // Act
        Long count = userNotificationService.findTotalCountByUser(null, false);

        // Assert
        assertEquals(0L, count);
        verify(em, never()).createQuery(anyString(), eq(Long.class));
    }

    @Test
    void testFindTotalCountByUser_countAll() {
        // Arrange
        Long userId = 1L;
        Long expectedCount = 15L;
        when(em.createQuery(contains("count(un)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(expectedCount);

        // Act
        Long actualCount = userNotificationService.findTotalCountByUser(userId, false);

        // Assert
        assertEquals(expectedCount, actualCount);
        verify(em).createQuery(contains("count(un)"), eq(Long.class));
        verify(countQuery).setParameter("userId", userId);
        verify(countQuery).setParameter("onlyUnread", false);
        verify(countQuery).getSingleResult();
    }

    @Test
    void testFindTotalCountByUser_countOnlyUnread() {
        // Arrange
        Long userId = 1L;
        Long expectedCount = 7L;
        when(em.createQuery(contains("count(un)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(expectedCount);

        // Act
        Long actualCount = userNotificationService.findTotalCountByUser(userId, true);

        // Assert
        assertEquals(expectedCount, actualCount);
        verify(em).createQuery(contains("count(un)"), eq(Long.class));
        verify(countQuery).setParameter("userId", userId);
        verify(countQuery).setParameter("onlyUnread", true);
        verify(countQuery).getSingleResult();
    }
}
