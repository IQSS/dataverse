package edu.harvard.iq.dataverse.externaltools;

import java.io.Serializable;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * A specification or definition for how an external tool is intended to
 * operate. The specification is applied dynamically on a per-file basis through
 * an {@link ExternalToolHandler}.
 */
@Entity
public class ExternalTool implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The display name (on the button, for example) of the tool in English.
     */
    // TODO: How are we going to internationalize the display name?
    @Column(nullable = false)
    private String displayName;

    /**
     * The description of the tool in English.
     */
    // TODO: How are we going to internationalize the description?
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String toolUrl;

    /**
     * Parameters the tool requires such as DataFile id and API Token as a JSON
     * object, persisted as a String.
     */
    @Column(nullable = false)
    private String toolParameters;

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method
     * [...ExternalTool.<Default Constructor>], with no parameters, does not
     * exist, or is not accessible
     *
     * Don't use it.
     */
    @Deprecated
    public ExternalTool() {
    }

    public ExternalTool(String displayName, String description, String toolUrl, String toolParameters) {
        this.displayName = displayName;
        this.description = description;
        this.toolUrl = toolUrl;
        this.toolParameters = toolParameters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getToolUrl() {
        return toolUrl;
    }

    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    public String getToolParameters() {
        return toolParameters;
    }

    public void setToolParameters(String toolParameters) {
        this.toolParameters = toolParameters;
    }

    public JsonObjectBuilder toJson() {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("id", getId());
        jab.add(ExternalToolHandler.DISPLAY_NAME, getDisplayName());
        jab.add(ExternalToolHandler.DESCRIPTION, getDescription());
        jab.add(ExternalToolHandler.TOOL_URL, getToolUrl());
        jab.add(ExternalToolHandler.TOOL_PARAMETERS, getToolParameters());
        return jab;
    }

}
