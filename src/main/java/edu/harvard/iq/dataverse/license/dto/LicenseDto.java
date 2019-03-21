package edu.harvard.iq.dataverse.license.dto;

import java.util.List;

public class LicenseDto {

    private Long id;

    private String name;

    private String url;

    private LicenseIconDto icon;

    private boolean active;

    private Long position;

    private List<LocaleTextDto> localizedNames;

    // -------------------- CONSTRUCTORS --------------------

    public LicenseDto(Long id, String name, String url, LicenseIconDto icon, boolean active,
                      Long position, List<LocaleTextDto> localizedNames) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.active = active;
        this.position = position;
        this.localizedNames = localizedNames;
    }

    public LicenseDto(String name, String url, LicenseIconDto icon,
                      boolean active, Long position, List<LocaleTextDto> localizedNames) {
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.active = active;
        this.position = position;
        this.localizedNames = localizedNames;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public LicenseIconDto getIcon() {
        return icon;
    }

    public boolean isActive() {
        return active;
    }

    public Long getPosition() {
        return position;
    }

    public List<LocaleTextDto> getLocalizedNames() {
        return localizedNames;
    }

    // -------------------- SETTERS --------------------

    public void setActive(boolean active) {
        this.active = active;
    }
  
}
