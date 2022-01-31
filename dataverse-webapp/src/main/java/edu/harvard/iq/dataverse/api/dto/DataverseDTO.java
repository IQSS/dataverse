package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;

import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataverseDTO {
    private Long id;
    private String alias;
    private String name;
    private String affiliation;
    private List<DataverseContactDTO> dataverseContacts;
    private Boolean permissionRoot;
    private String description;
    private String dataverseType;
    private Long ownerId;
    private String creationDate;
    private AuthenticatedUserDTO creator;
    private DataverseThemeDTO theme;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public List<DataverseContactDTO> getDataverseContacts() {
        return dataverseContacts;
    }

    public Boolean getPermissionRoot() {
        return permissionRoot;
    }

    public String getDescription() {
        return description;
    }

    public String getDataverseType() {
        return dataverseType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public AuthenticatedUserDTO getCreator() {
        return creator;
    }

    public DataverseThemeDTO getTheme() {
        return theme;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setDataverseContacts(List<DataverseContactDTO> dataverseContacts) {
        this.dataverseContacts = dataverseContacts;
    }

    public void setPermissionRoot(Boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDataverseType(String dataverseType) {
        this.dataverseType = dataverseType;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public void setCreator(AuthenticatedUserDTO creator) {
        this.creator = creator;
    }

    public void setTheme(DataverseThemeDTO theme) {
        this.theme = theme;
    }

    // -------------------- INNER CLASSES --------------------

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class DataverseContactDTO {
        private Integer displayOrder;
        private String contactEmail;

        // -------------------- CONSTRUCTORS --------------------

        public DataverseContactDTO(Integer displayOrder, String contactEmail) {
            this.displayOrder = displayOrder;
            this.contactEmail = contactEmail;
        }

        // -------------------- GETTERS --------------------

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        // -------------------- SETTERS --------------------

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class DataverseThemeDTO {
        private Long id;
        private String logo;
        private String tagline;
        private String linkUrl;
        private String linkColor;
        private String textColor;
        private String backgroundColor;
        private String logoBackgroundColor;

        // -------------------- GETTERS --------------------

        public Long getId() {
            return id;
        }

        public String getLogo() {
            return logo;
        }

        public String getTagline() {
            return tagline;
        }

        public String getLinkUrl() {
            return linkUrl;
        }

        public String getLinkColor() {
            return linkColor;
        }

        public String getTextColor() {
            return textColor;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public String getLogoBackgroundColor() {
            return logoBackgroundColor;
        }

        // -------------------- SETTERS --------------------

        public void setId(Long id) {
            this.id = id;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public void setTagline(String tagline) {
            this.tagline = tagline;
        }

        public void setLinkUrl(String linkUrl) {
            this.linkUrl = linkUrl;
        }

        public void setLinkColor(String linkColor) {
            this.linkColor = linkColor;
        }

        public void setTextColor(String textColor) {
            this.textColor = textColor;
        }

        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public void setLogoBackgroundColor(String logoBackgroundColor) {
            this.logoBackgroundColor = logoBackgroundColor;
        }
    }

    public static class Converter {

        // -------------------- LOGIC --------------------

        public DataverseDTO convert(Dataverse dataverse) {
            DataverseDTO converted = new DataverseDTO();
            converted.setId(dataverse.getId());
            converted.setAlias(dataverse.getAlias());
            converted.setName(dataverse.getName());
            converted.setAffiliation(dataverse.getAffiliation());
            converted.setDataverseContacts(dataverse.getDataverseContacts().stream()
                    .map(c -> new DataverseContactDTO(c.getDisplayOrder(), c.getContactEmail()))
                    .collect(Collectors.toList()));
            converted.setPermissionRoot(dataverse.isPermissionRoot());
            converted.setDescription(dataverse.getDescription());
            converted.setDataverseType(dataverse.getDataverseType().name());
            converted.setOwnerId(dataverse.getOwner() != null ? dataverse.getOwner().getId() : null);
            converted.setCreationDate(dataverse.getCreateDate() != null
                    ? Util.getDateTimeFormat().format(dataverse.getCreateDate()) : null);
            converted.setCreator(dataverse.getCreator() != null
                    ? new AuthenticatedUserDTO.Converter().convert(dataverse.getCreator()) : null);
            converted.setTheme(dataverse.getDataverseTheme() != null
                    ? convertTheme(dataverse.getDataverseTheme()) : null);
            return converted;
        }

        // -------------------- PRIVATE --------------------

        private DataverseThemeDTO convertTheme(DataverseTheme theme) {
            DataverseThemeDTO converted = new DataverseThemeDTO();
            converted.setId(theme.getId());
            converted.setLogo(theme.getLogo());
            converted.setTagline(theme.getTagline());
            converted.setLinkUrl(theme.getLinkUrl());
            converted.setLinkColor(theme.getLinkColor());
            converted.setTextColor(theme.getTextColor());
            converted.setBackgroundColor(theme.getBackgroundColor());
            converted.setLogoBackgroundColor(theme.getLogoAlignment() != null
                    ? theme.getLogoBackgroundColor() : null);
            return converted;
        }
    }
}
