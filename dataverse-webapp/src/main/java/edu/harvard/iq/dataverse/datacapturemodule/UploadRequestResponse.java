package edu.harvard.iq.dataverse.datacapturemodule;

public class UploadRequestResponse {

    private final int httpStatusCode;
    private final String response;

    public UploadRequestResponse(int httpStatusCode, String response) {
        this.httpStatusCode = httpStatusCode;
        this.response = response;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getResponse() {
        return response;
    }

}
