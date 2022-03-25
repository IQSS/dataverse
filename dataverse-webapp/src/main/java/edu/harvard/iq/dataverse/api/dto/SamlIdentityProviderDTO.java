package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SamlIdentityProviderDTO {
    private Long id;
    private String entityId;
    private String metadataUrl;
    private String displayName;

    // -------------------- CONSTRUCTORS --------------------


    public SamlIdentityProviderDTO() { }

    public SamlIdentityProviderDTO(Long id, String entityId, String metadataUrl, String displayName) {
        this.id = id;
        this.entityId = entityId;
        this.metadataUrl = metadataUrl;
        this.displayName = displayName;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public SamlIdentityProviderDTO convert(SamlIdentityProvider idp) {
            SamlIdentityProviderDTO converted = new SamlIdentityProviderDTO();
            converted.setId(idp.getId());
            converted.setEntityId(idp.getEntityId());
            converted.setMetadataUrl(idp.getMetadataUrl());
            converted.setDisplayName(idp.getDisplayName());
            return converted;
        }

        public SamlIdentityProvider toEntity(SamlIdentityProviderDTO dto) {
            SamlIdentityProvider entity = new SamlIdentityProvider();
            entity.setId(dto.getId());
            entity.setEntityId(dto.getEntityId());
            entity.setMetadataUrl(dto.getMetadataUrl());
            entity.setDisplayName(dto.getDisplayName());
            return entity;
        }
    }
}
