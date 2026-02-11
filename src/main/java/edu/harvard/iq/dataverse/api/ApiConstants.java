package edu.harvard.iq.dataverse.api;

public final class ApiConstants {

    private ApiConstants() {
        // Restricting instantiation
    }

    // Statuses
    public static final String STATUS_FIELD = "status";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_WF_IN_PROGRESS = "WORKFLOW_IN_PROGRESS";
    
    public static final String DATA_FIELD = "data";
    public static final String TOTAL_COUNT_FIELD = "totalCount";
    public static final String MESSAGE_FIELD = "message";

    // Authentication
    public static final String CONTAINER_REQUEST_CONTEXT_USER = "user";
    public static final String ALIAS_KEY=":alias";
    public static final String DATAVERSE_KEY_HEADER_NAME = "X-Dataverse-key";
    public static final String DATAVERSE_WORKFLOW_INVOCATION_HEADER_NAME = "X-Dataverse-invocationID";
    public static final String RESPONSE_MESSAGE_AUTHENTICATED_USER_REQUIRED = "Only authenticated users can perform the requested operation";

    // Dataset
    public static final String PERSISTENT_ID_KEY=":persistentId";
    public static final String DS_VERSION_LATEST = ":latest";
    public static final String DS_VERSION_DRAFT = ":draft";
    public static final String DS_VERSION_LATEST_PUBLISHED = ":latest-published";
    
    // addFiles call
    public static final String API_ADD_FILES_COUNT_PROCESSED = "Total number of files";
    public static final String API_ADD_FILES_COUNT_SUCCESSFUL = "Number of files successfully added";
}
