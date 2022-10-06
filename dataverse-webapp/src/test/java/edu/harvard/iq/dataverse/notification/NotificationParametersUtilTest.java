package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class NotificationParametersUtilTest {

    NotificationParametersUtil notificationParametersUtil = new NotificationParametersUtil();

    @Test
    void getParameters() {
        // given
        UserNotification notification = new UserNotification();
        notification.setParameters("{\"first\":\"A\", \"second\":\"B\"}");

        // when
        Map<String, String> parameters = notificationParametersUtil.getParameters(notification);

        // then
        assertThat(parameters.entrySet())
                .extracting(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactlyInAnyOrder(tuple("first", "A"), tuple("second", "B"));
    }

    @Test
    void setParameters() {
        // given
        UserNotification notification = new UserNotification();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("A", "1");
        parameters.put("B", "2");

        // when
        notificationParametersUtil.setParameters(notification, parameters);

        // then
        assertThat(notification.getParameters()).isEqualTo("{\"A\":\"1\",\"B\":\"2\"}");
    }
}