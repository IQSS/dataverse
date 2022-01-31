package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.user.AuthenticationProviderRow;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthenticationProviderRowDTO {

    private String id;
    private String factoryAlias;
    private String title;
    private String subtitle;
    private String factoryData;
    private Boolean enabled;

    // -------------------- GETTERS --------------------

    public String getId() {
        return id;
    }

    public String getFactoryAlias() {
        return factoryAlias;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getFactoryData() {
        return factoryData;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    // -------------------- SETTERS --------------------

    public void setId(String id) {
        this.id = id;
    }

    public void setFactoryAlias(String factoryAlias) {
        this.factoryAlias = factoryAlias;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setFactoryData(String factoryData) {
        this.factoryData = factoryData;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public AuthenticationProviderRowDTO convert(AuthenticationProviderRow row) {
            AuthenticationProviderRowDTO converted = new AuthenticationProviderRowDTO();
            converted.setId(row.getId());
            converted.setFactoryAlias(row.getFactoryAlias());
            converted.setTitle(row.getTitle());
            converted.setSubtitle(row.getSubtitle());
            converted.setFactoryData(row.getFactoryData());
            converted.setEnabled(row.isEnabled());
            return converted;
        }
    }
}
