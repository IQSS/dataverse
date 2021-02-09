package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDTO<T> {

    private int code;
    private T data;

    // -------------------- CONSTRUCTORS --------------------

    public ResponseDTO() { }

    public ResponseDTO(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public ResponseDTO(int code) {
        this.code = code;
    }

    // -------------------- GETTERS --------------------

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    // -------------------- SETTERS --------------------

    public void setCode(int code) {
        this.code = code;
    }

    public void setData(T data) {
        this.data = data;
    }
}
