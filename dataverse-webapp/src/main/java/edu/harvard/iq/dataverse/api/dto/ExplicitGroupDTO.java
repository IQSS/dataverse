package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;

import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExplicitGroupDTO {
    private String identifier;
    private String groupAliasInOwner;
    private Long owner;
    private String description;
    private String displayName;
    private List<String> containedRoleAssignees;

    // -------------------- GETTERS --------------------

    public String getIdentifier() {
        return identifier;
    }

    public String getGroupAliasInOwner() {
        return groupAliasInOwner;
    }

    public Long getOwner() {
        return owner;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getContainedRoleAssignees() {
        return containedRoleAssignees;
    }

    // -------------------- SETTERS --------------------

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setGroupAliasInOwner(String groupAliasInOwner) {
        this.groupAliasInOwner = groupAliasInOwner;
    }

    public void setOwner(Long owner) {
        this.owner = owner;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setContainedRoleAssignees(List<String> containedRoleAssignees) {
        this.containedRoleAssignees = containedRoleAssignees;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public ExplicitGroupDTO convert(ExplicitGroup group) {
            ExplicitGroupDTO converted = new ExplicitGroupDTO();
            converted.setIdentifier(group.getIdentifier());
            converted.setGroupAliasInOwner(group.getGroupAliasInOwner());
            converted.setOwner(group.getOwner().getId());
            converted.setDescription(group.getDescription());
            converted.setDisplayName(group.getDisplayName());
            converted.setContainedRoleAssignees(group.getContainedRoleAssgineeIdentifiers().stream()
                    .sorted()
                    .collect(Collectors.toList()));
            return converted;
        }
    }
}
