package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;

import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataverseRoleDTO {
    private String alias;
    private String name;
    private List<String> permissions;
    private String description;
    private Long id;
    private Long ownerId;

    // -------------------- GETTERS --------------------

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    // -------------------- SETTERS --------------------

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public DataverseRoleDTO convert(DataverseRole role) {
            DataverseRoleDTO converted = new DataverseRoleDTO();
            converted.setAlias(role.getAlias());
            converted.setName(role.getName());
            converted.setPermissions(role.permissions().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList()));
            converted.setDescription(role.getDescription());
            converted.setId(role.getId());
            converted.setOwnerId(role.getOwner() != null ? role.getOwner().getId() : null);
            return converted;
        }
    }
}
