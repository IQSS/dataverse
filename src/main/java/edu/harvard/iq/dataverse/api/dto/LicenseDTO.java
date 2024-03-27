package edu.harvard.iq.dataverse.api.dto;

/**
 *
 * @author mderuijter
 */
public class LicenseDTO {
    String name;
    String uri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
