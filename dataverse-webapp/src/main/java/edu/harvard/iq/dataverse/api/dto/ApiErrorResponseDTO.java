package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.AbstractApiBean;

import javax.ws.rs.core.Response;

public class ApiErrorResponseDTO extends ApiResponseDTO<Void> {

    private String message;
    private String incidentId;

    // -------------------- CONSTRUCTORS --------------------

    private ApiErrorResponseDTO() { }

    // -------------------- GETTERS -------------------

    public String getMessage() {
        return message;
    }

    public String getIncidentId() {
        return incidentId;
    }

    // -------------------- LOGIC --------------------

    public static ApiErrorResponseDTO errorResponse(int code, String message) {
        ApiErrorResponseDTO errorResponse = new ApiErrorResponseDTO();
        errorResponse.status = AbstractApiBean.STATUS_ERROR;
        errorResponse.code = code;
        errorResponse.message = message;

        return errorResponse;
    }

    public ApiErrorResponseDTO withIncidentId(String incidentId) {
        this.incidentId = incidentId;
        return this;
    }

    public Response asJaxRsResponse() {
        return Response.status(getCode())
                .entity(this)
                .type("application/json")
                .build();
    }
}
