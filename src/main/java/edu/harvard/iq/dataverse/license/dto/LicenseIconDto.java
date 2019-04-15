package edu.harvard.iq.dataverse.license.dto;

import org.primefaces.model.StreamedContent;

public class LicenseIconDto {

    private Long id;

    private StreamedContent content;

    // -------------------- CONSTRUCTORS --------------------

    public LicenseIconDto() {
    }

    public LicenseIconDto(Long id, StreamedContent content) {
        this.id = id;
        this.content = content;
    }

    // -------------------- GETTERS --------------------


    public Long getId() {
        return id;
    }

    public StreamedContent getContent() {
        return content;
    }

    // -------------------- SETTERS --------------------


    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(StreamedContent content) {
        this.content = content;
    }
}
