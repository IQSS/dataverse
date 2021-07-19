package edu.harvard.iq.dataverse.license.dto;

import org.apache.commons.lang.StringUtils;

public class LicenseIconDto {

    private Long id;

    private byte[] content;
    private String contentType;

    // -------------------- CONSTRUCTORS --------------------

    public LicenseIconDto() {
        content = new byte[0];
        contentType = StringUtils.EMPTY;
    }

    public LicenseIconDto(Long id, byte[] content, String contentType) {
        this.id = id;
        this.content = content;
        this.contentType = contentType;
    }

    // -------------------- GETTERS --------------------


    public Long getId() {
        return id;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    // -------------------- SETTERS --------------------


    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
