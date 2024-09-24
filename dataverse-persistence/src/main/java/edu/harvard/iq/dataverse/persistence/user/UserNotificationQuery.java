package edu.harvard.iq.dataverse.persistence.user;

/**
 * Query parameters used for user notifications.
 */
public class UserNotificationQuery {

    private final static int DEFAULT_RESULT_LIMIT = 10;

    private Long userId;

    private String searchLabel;

    private int offset;

    private int resultLimit = DEFAULT_RESULT_LIMIT;

    private boolean ascending;

    // -------------------- CONSTRUCTORS --------------------

    private UserNotificationQuery() {
    }

    // -------------------- GETTERS --------------------

    public Long getUserId() {
        return userId;
    }

    public String getSearchLabel() {
        return searchLabel;
    }

    public int getOffset() {
        return offset;
    }

    public int getResultLimit() {
        return resultLimit;
    }

    public boolean isAscending() {
        return ascending;
    }

    // -------------------- LOGIC --------------------

    static public UserNotificationQuery newQuery() {
        return new UserNotificationQuery();
    }

    public UserNotificationQuery withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public UserNotificationQuery withSearchLabel(String searchLabel) {
        this.searchLabel = searchLabel;
        return this;
    }

    public UserNotificationQuery withOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public UserNotificationQuery withResultLimit(int resultLimit) {
        this.resultLimit = resultLimit;
        return this;
    }

    public UserNotificationQuery withAscending(boolean ascending) {
        this.ascending = ascending;
        return this;
    }
}
