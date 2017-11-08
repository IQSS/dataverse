package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.io.Serializable;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class ExternalTool implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: How are we going to internationize the display name?
    @Column(nullable = false)
    private String displayName;

    // TODO: How are we going to internationize the description?
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String toolUrl;

    /**
     * Parameters the tool requires such as DataFile id and API Token
     */
    @Column(nullable = false)
    private String toolParameters;

    // FIXME: Remove this.
    @Transient
    private DataFile dataFile;

    // FIXME: Remove this.
    @Transient
    private ApiToken apiToken;

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

    public String getToolUrlWithQueryParams() {
        // TODO: In addition to (or rather than) supporting API tokens as query parameters, support them as HTTP headers.
        return toolUrl + ExternalToolHandler.getQueryParametersForUrl(this, dataFile, apiToken);
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

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public void setApiToken(ApiToken apiToken) {
        this.apiToken = apiToken;
    }

    public JsonObjectBuilder toJson() {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("id", this.getId());
        jab.add(ExternalToolHandler.DISPLAY_NAME, this.getDisplayName());
        jab.add(ExternalToolHandler.DESCRIPTION, this.getDescription());
        jab.add(ExternalToolHandler.TOOL_URL, this.getToolUrl());
        jab.add(ExternalToolHandler.TOOL_PARAMETERS, this.getToolParameters());
        return jab;
    }

}
