package edu.harvard.iq.dataverse.globus;

public class MkDirResponse {
    private String DATA_TYPE;
    private String code;
    private String message;
    private String request_id;
    private String resource;

    public void setCode(String code) {
        this.code = code;
    }

    public void setDataType(String dataType) {
        this.DATA_TYPE = dataType;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRequestId(String requestId) {
        this.request_id = requestId;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getCode() {
        return code;
    }

    public String getDataType() {
        return DATA_TYPE;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return request_id;
    }

    public String getResource() {
        return resource;
    }

}
