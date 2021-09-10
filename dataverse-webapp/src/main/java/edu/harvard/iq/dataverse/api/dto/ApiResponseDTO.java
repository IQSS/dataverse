package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.ws.rs.core.Response;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    protected String status;
    protected int code;
    private T data;

    // -------------------- CONSTRUCTORS --------------------

    public ApiResponseDTO() { }

    public ApiResponseDTO(String status, int code, T data) {
        this.status = status;
        this.code = code;
        this.data = data;
    }

    public ApiResponseDTO(Response.Status status, T data) {
        this(status.name(), status.getStatusCode(), data);
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

}
