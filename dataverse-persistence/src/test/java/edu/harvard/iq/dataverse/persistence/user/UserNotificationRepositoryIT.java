package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

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
    public void updateRequestor() {
        // when
        int updated = userNotificationRepository.updateRequestor(3L, 1L);
        // then
        assertThat(updated).isEqualTo(1);
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

    @Test
    public void findLastSubmitNotificationForDataset() {
        // given & when
        UserNotification notification = userNotificationRepository.findLastSubmitNotificationByObjectId(44L);

        // then
        assertThat(notification).extracting(UserNotification::getId, UserNotification::getType)
                .containsExactly(3L, NotificationType.SUBMITTEDDS);
    }

    @Test
    public void query() {
        // given
        UserNotificationQuery query = UserNotificationQuery.newQuery();

        // when
        UserNotificationQueryResult result = userNotificationRepository.query(query);

        // then
        assertThat(result.getResult()).hasSize(5);
        assertThat(result.getTotalCount()).isEqualTo(5);
    }

    @Test
    public void query_paging() {
        // given & when
        UserNotificationQueryResult page1 = userNotificationRepository.query(UserNotificationQuery.newQuery()
                .withUserId(2L)
                .withResultLimit(2));

        // then
        assertThat(page1.getResult()).extracting(UserNotification::getId).containsExactly(3L, 2L);
        assertThat(page1.getTotalCount()).isEqualTo(3);

        // given & when
        UserNotificationQueryResult page2 = userNotificationRepository.query(UserNotificationQuery.newQuery()
                .withUserId(2L)
                .withOffset(2)
                .withResultLimit(2));

        // then
        assertThat(page2.getResult()).extracting(UserNotification::getId).containsExactly(1L);
        assertThat(page2.getTotalCount()).isEqualTo(3);
    }

    @Test
    public void query_filtering() {
        // given
        UserNotificationQuery query = UserNotificationQuery.newQuery().withSearchLabel("Monkeys");

        // when
        UserNotificationQueryResult result = userNotificationRepository.query(query);

        // then
        assertThat(result.getResult()).extracting(UserNotification::getId).containsExactly(4L, 3L);
        assertThat(result.getTotalCount()).isEqualTo(2);
    }
}
