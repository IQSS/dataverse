package edu.harvard.iq.dataverse.api;

import java.util.List;

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
    public static final String DS_VERSION_IDENTIFIER_REGEX = "^(:latest|:draft|:latest-published|\\d+(?:\\.\\d+)?)$";
    public static final List<String> DS_VERSION_RESERVED_IDENTIFIERS = List.of(DS_VERSION_DRAFT, DS_VERSION_LATEST_PUBLISHED, DS_VERSION_LATEST);
    // TODO: should be replaced by a bundle reference
    public static final String DS_VERSION_IDENTIFIER_MESSAGE = "version must be one of :latest, :latest-published, :draft, or a numeric version like 1.0";
    
    // addFiles call
    public static final String API_ADD_FILES_COUNT_PROCESSED = "Total number of files";
    public static final String API_ADD_FILES_COUNT_SUCCESSFUL = "Number of files successfully added";
}
