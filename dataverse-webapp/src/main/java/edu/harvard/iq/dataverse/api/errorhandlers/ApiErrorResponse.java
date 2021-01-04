package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.AbstractApiBean;

public class ApiErrorResponse {

    private String status;
    private int code;
    private String message;
    private String incidentId;
    
    // -------------------- CONSTRUCTORS --------------------
    
    private ApiErrorResponse() { }
    
    // -------------------- GETTERS -------------------
    
    public String getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getIncidentId() {
        return incidentId;
    }

    // -------------------- LOGIC --------------------

    public static ApiErrorResponse errorResponse(int code, String message) {
        ApiErrorResponse errorResponse = new ApiErrorResponse();
        errorResponse.status = AbstractApiBean.STATUS_ERROR;
        errorResponse.code = code;
        errorResponse.message = message;

        return errorResponse;
    }
    
    public ApiErrorResponse withIncidentId(String incidentId) {
        this.incidentId = incidentId;
        return this;
    }
}
