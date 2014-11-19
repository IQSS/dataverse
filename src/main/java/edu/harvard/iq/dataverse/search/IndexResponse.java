package edu.harvard.iq.dataverse.search;

public class IndexResponse {

    private final String message;

    public IndexResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
