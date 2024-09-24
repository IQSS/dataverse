package edu.harvard.iq.dataverse.persistence.user;

import java.util.List;

/**
 * Query result including the total count of available results.
 */
public class UserNotificationQueryResult {

    private final List<UserNotification> result;

    private final Long totalCount;

    // -------------------- CONSTRUCTORS --------------------

    public UserNotificationQueryResult(List<UserNotification> result, Long totalCount) {
        this.result = result;
        this.totalCount = totalCount;
    }

    // -------------------- GETTERS --------------------

    public List<UserNotification> getResult() {
        return result;
    }

    public Long getTotalCount() {
        return totalCount;
    }
}
