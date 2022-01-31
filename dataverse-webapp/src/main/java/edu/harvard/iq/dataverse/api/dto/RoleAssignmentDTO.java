package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class RoleAssignmentDTO {

    private Long id;
    private String assignee;
    private Long roleId;

    @JsonProperty("_roleAlias")
    private String roleAlias;

    private String privateUrlToken;
    private Long definitionPointId;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getAssignee() {
        return assignee;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleAlias() {
        return roleAlias;
    }

    public String getPrivateUrlToken() {
        return privateUrlToken;
    }

    public Long getDefinitionPointId() {
        return definitionPointId;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setRoleAlias(String roleAlias) {
        this.roleAlias = roleAlias;
    }

    public void setPrivateUrlToken(String privateUrlToken) {
        this.privateUrlToken = privateUrlToken;
    }

    public void setDefinitionPointId(Long definitionPointId) {
        this.definitionPointId = definitionPointId;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public RoleAssignmentDTO convert(RoleAssignment assignment) {
            RoleAssignmentDTO converted = new RoleAssignmentDTO();
            converted.setId(assignment.getId());
            converted.setAssignee(assignment.getAssigneeIdentifier());
            converted.setRoleId(assignment.getRole().getId());
            converted.setRoleAlias(assignment.getRole().getAlias());
            converted.setPrivateUrlToken(assignment.getPrivateUrlToken());
            converted.setDefinitionPointId(assignment.getDefinitionPoint().getId());
            return converted;
        }
    }
}
