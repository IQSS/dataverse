package edu.harvard.iq.dataverse.globus;

public class PermissionsResponse {
    private String code;
    private String resource;
    private String DATA_TYPE;
    private String request_id;
    private String access_id;
    private String message;

    public String getDATA_TYPE() {
        return DATA_TYPE;
    }

    public String getResource() {
        return resource;
    }

    public String getRequestId() {
        return request_id;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getAccessId() {
        return access_id;
    }

    public void setDATA_TYPE(String DATA_TYPE) {
        this.DATA_TYPE = DATA_TYPE;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setRequestId(String requestId) {
        this.request_id = requestId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setAccessId(String accessId) {
        this.access_id = accessId;
    }
}
