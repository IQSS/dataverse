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
    // Set on the request by CompoundAuthMechanism only for session-cookie
    // authentication — the one mechanism downstream code (AuthFilter's CSRF
    // hardening, the token bootstrap endpoint) needs to distinguish. Absent
    // for every other mechanism.
    public static final String CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM = "authMechanism";
    public static final String AUTH_MECHANISM_SESSION_COOKIE = "sessionCookie";
    public static final String CSRF_TOKEN_HEADER = "X-Dataverse-CSRF-Token";
    public static final String CSRF_TOKEN_ENDPOINT_PATH = ":csrf-token";
    public static final String ORIGIN_HEADER = "Origin";
    public static final String REFERER_HEADER = "Referer";

    // Dataset
    public static final String DS_VERSION_LATEST = ":latest";
    public static final String DS_VERSION_DRAFT = ":draft";
    public static final String DS_VERSION_LATEST_PUBLISHED = ":latest-published";
    
    // addFiles call
    public static final String API_ADD_FILES_COUNT_PROCESSED = "Total number of files";
    public static final String API_ADD_FILES_COUNT_SUCCESSFUL = "Number of files successfully added";
}
