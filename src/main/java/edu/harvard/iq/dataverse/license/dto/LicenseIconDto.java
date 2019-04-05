package edu.harvard.iq.dataverse.license.dto;

import org.primefaces.model.StreamedContent;

public class LicenseIconDto {

    private StreamedContent content;

    // -------------------- CONSTRUCTORS --------------------

    public LicenseIconDto(StreamedContent content) {
        this.content = content;
    }

    public LicenseIconDto() {
    }

    // -------------------- GETTERS --------------------

    public StreamedContent getContent() {
        return content;
    }

    // -------------------- SETTERS --------------------

    public void setContent(StreamedContent content) {
        this.content = content;
    }
}
