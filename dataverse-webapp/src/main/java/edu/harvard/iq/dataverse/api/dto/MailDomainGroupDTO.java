package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.dto.validation.ValidMainDomainList;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class MailDomainGroupDTO {

    @NotBlank(message = "Group alias must not be blank")
    private String alias;

    private String displayName;

    private String description;

    @NotEmpty(message = "At least one domain must be added to inclusions list")
    @ValidMainDomainList
    private List<String> inclusions = new ArrayList<>();

    @ValidMainDomainList
    private List<String> exclusions = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public String getAlias() {
        return alias;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getInclusions() {
        return inclusions;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    // -------------------- SETTERS --------------------

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInclusions(List<String> inclusions) {
        this.inclusions = inclusions;
    }

    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }
}
