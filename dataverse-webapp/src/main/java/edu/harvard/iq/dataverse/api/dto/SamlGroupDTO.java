package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.group.SamlGroup;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SamlGroupDTO {
    private String name;
    private String entityId;
    private Long id;

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getEntityId() {
        return entityId;
    }

    public Long getId() {
        return id;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public SamlGroupDTO convert(SamlGroup group) {
            SamlGroupDTO converted = new SamlGroupDTO();
            converted.setName(group.getName());
            converted.setEntityId(group.getEntityId());
            converted.setId(group.getId());
            return converted;
        }
    }
}
