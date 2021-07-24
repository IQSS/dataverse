package edu.harvard.iq.dataverse.api.dto;

/**
 *
 * @author mderuijter
 */
public class LicenseDTO {
    String label;
    String uri;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
