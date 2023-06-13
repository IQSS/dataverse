package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository repository;

    @InjectMocks
    private UserNotificationService service;

    @Test
    void findLastSubmitNotificationForDataset() {
        // given
        Mockito.when(repository.findLastSubmitNotificationByObjectId(128L)).thenReturn(new UserNotification());
        Dataset dataset = new Dataset();
        DatasetVersion version = dataset.getEditVersion();
        version.setId(128L);

        // when
        UserNotification notification = service.findLastSubmitNotificationForDataset(dataset);

        // then
        assertThat(notification).isNotNull();
    }
}