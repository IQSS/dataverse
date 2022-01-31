package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.ws.rs.core.Response;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    protected String status;
    protected String message;
    protected int code;
    private T data;

    // -------------------- CONSTRUCTORS --------------------

    public ApiResponseDTO() { }

    public ApiResponseDTO(String status, int code, T data, String message) {
        this.status = status;
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public ApiResponseDTO(String status, int code, T data) {
        this(status, code, data, null);
    }

    public ApiResponseDTO(Response.Status status, T data, String message) {
        this(status.name(), status.getStatusCode(), data, message);
    }

    public ApiResponseDTO(Response.Status status, T data) {
        this(status, data, null);
    }

    // -------------------- GETTERS --------------------

    public String getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
