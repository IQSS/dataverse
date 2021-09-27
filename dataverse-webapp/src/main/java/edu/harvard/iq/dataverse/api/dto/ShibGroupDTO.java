package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.group.ShibGroup;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ShibGroupDTO {
    private String name;
    private String attribute;
    private String pattern;
    private Long id;

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getPattern() {
        return pattern;
    }

    public Long getId() {
        return id;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public ShibGroupDTO convert(ShibGroup group) {
            ShibGroupDTO converted = new ShibGroupDTO();
            converted.setName(group.getName());
            converted.setAttribute(group.getAttribute());
            converted.setPattern(group.getPattern());
            converted.setId(group.getId());
            return converted;
        }
    }
}
