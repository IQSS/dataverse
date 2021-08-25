package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;

import javax.inject.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserNotificationRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private UserNotificationRepository userNotificationRepository;
    @Inject
    private AuthenticatedUserRepository authenticatedUserRepository;

    // -------------------- TESTS --------------------

    @Test
    public void findByUser() {
        // given
        AuthenticatedUser user = authenticatedUserRepository.findById(2L).get();
        // when
        List<UserNotification> notifications = userNotificationRepository.findByUser(user.getId());
        // then
        assertThat(notifications)
            .hasSize(3)
            .extracting(UserNotification::getUser)
            .allMatch(notificationUser -> notificationUser.equals(user));
    }

    @Test
    public void findByRequestorUser() {
        // given
        AuthenticatedUser user = authenticatedUserRepository.findById(2L).get();
        // when
        List<UserNotification> notifications = userNotificationRepository.findByRequestor(user.getId());
        // then
        assertThat(notifications)
            .hasSize(1)
            .extracting(UserNotification::getRequestor)
            .allMatch(notificationUser -> notificationUser.equals(user));
    }

    @Test
    public void getUnreadNotificationCountByUser() {
        // when
        Long unreadCount = userNotificationRepository.getUnreadNotificationCountByUser(2L);
        // then
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    public void getUnreadNotificationCountByUser_non_existing_user() {
        // when
        Long unreadCount = userNotificationRepository.getUnreadNotificationCountByUser(999L);
        // then
        assertThat(unreadCount).isEqualTo(0);
    }
    
    @Test
    @Transactional(TransactionMode.DISABLED)
    public void updateEmailSent() {
        // given
        Long notEmailedNotificationId = userNotificationRepository.findAll().stream()
            .filter(notification -> !notification.isEmailed())
            .findFirst().get().getId();

        // when
        int modifiedRecordsCount = userNotificationRepository.updateEmailSent(notEmailedNotificationId);
        // then
        assertThat(modifiedRecordsCount).isEqualTo(1);
        assertThat(userNotificationRepository.getById(notEmailedNotificationId))
            .extracting(UserNotification::isEmailed)
            .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
            .isTrue();
    }
}
