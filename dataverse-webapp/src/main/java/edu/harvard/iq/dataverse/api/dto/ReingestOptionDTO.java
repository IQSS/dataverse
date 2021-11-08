package edu.harvard.iq.dataverse.api.dto;

public class ReingestOptionDTO {

    /**
     * Encoding to use for the input file
     */
    private String encoding;

    // -------------------- GETTERS --------------------

    public String getEncoding() {
        return encoding;
    }

    // -------------------- SETTERS --------------------

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
