package edu.harvard.iq.dataverse.api;

public final class ApiConstants {

    private ApiConstants() {
        // Restricting instantiation
    }

    // Statuses
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";

    // Authentication
    public static final String CONTAINER_REQUEST_CONTEXT_USER = "user";

    // Dataset
    public static final String DS_VERSION_LATEST = ":latest";
    public static final String DS_VERSION_DRAFT = ":draft";
    public static final String DS_VERSION_LATEST_PUBLISHED = ":latest-published";
}
