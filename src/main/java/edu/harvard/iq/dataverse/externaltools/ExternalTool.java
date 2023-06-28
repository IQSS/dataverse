package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.StringUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 * A specification or definition for how an external tool is intended to
 * operate. The specification is applied dynamically on a per-file basis through
 * an {@link ExternalToolHandler}.
 */
@Entity
public class ExternalTool implements Serializable {

    public static final String DISPLAY_NAME = "displayName";
    public static final String DESCRIPTION = "description";
    public static final String LEGACY_SINGLE_TYPE = "type";
    public static final String TYPES = "types";
    public static final String SCOPE = "scope";
    public static final String TOOL_URL = "toolUrl";
    public static final String TOOL_PARAMETERS = "toolParameters";
    public static final String CONTENT_TYPE = "contentType";
    public static final String TOOL_NAME = "toolName";
    public static final String ALLOWED_API_CALLS = "allowedApiCalls";
    public static final String REQUIREMENTS = "requirements";

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
     * Type of tool such as dct, explorer, etc
     */
    @Column(nullable = true)
    private String toolName;

    /**
     * The description of the tool in English.
     */
    // TODO: How are we going to internationalize the description?
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * A tool can be multiple types, "explore", "configure", "preview", etc.
     */
    @OneToMany(mappedBy = "externalTool", cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private List<ExternalToolType> externalToolTypes;

    /**
     * Whether the tool operates at the dataset or file level.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Scope scope;

    @Column(nullable = false)
    private String toolUrl;

    /**
     * Parameters the tool requires such as DataFile id and API Token as a JSON
     * object, persisted as a String.
     */
    @Column(nullable = false)
    private String toolParameters;

    /**
     * The file content type the tool works on. For tabular files, the type
     * text/tab-separated-values should be sent
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String contentType;

    /**
     * Set of API calls the tool would like to be able to use (e,.g. for retrieving
     * data through the Dataverse REST API). Used to build signedUrls for POST
     * headers, as in DP Creator
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String allowedApiCalls;

    /**
     * When non-null, the tool has indicated that it has certain requirements
     * that must be met before it should be shown to the user. This
     * functionality was added for tools that operate on aux files rather than
     * data files so "auxFilesExist" is one of the possible values.
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String requirements;

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

    public ExternalTool(String displayName, String toolName, String description, List<ExternalToolType> externalToolTypes, Scope scope, String toolUrl, String toolParameters, String contentType) {
       this(displayName, toolName, description, externalToolTypes, scope, toolUrl, toolParameters, contentType, null, null);
    }

    public ExternalTool(String displayName, String toolName, String description, List<ExternalToolType> externalToolTypes, Scope scope, String toolUrl, String toolParameters, String contentType, String allowedApiCalls, String requirements) {
        this.displayName = displayName;
        this.toolName = toolName;
        this.description = description;
        this.externalToolTypes = externalToolTypes;
        this.scope = scope;
        this.toolUrl = toolUrl;
        this.toolParameters = toolParameters;
        this.contentType = contentType;
        this.allowedApiCalls = allowedApiCalls;
        this.requirements = requirements;
    }

    public enum Type {

        EXPLORE("explore"),
        CONFIGURE("configure"),
        PREVIEW("preview"),
        QUERY("query");

        private final String text;

        private Type(final String text) {
            this.text = text;
        }

        public static Type fromString(String text) {
            if (text != null) {
                for (Type type : Type.values()) {
                    if (text.equals(type.text)) {
                        return type;
                    }
                }
            }
            throw new IllegalArgumentException("Type must be one of these values: " + Arrays.asList(Type.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public enum Scope {

        DATASET("dataset"),
        FILE("file");

        private final String text;

        private Scope(final String text) {
            this.text = text;
        }

        public static Scope fromString(String text) {
            if (text != null) {
                for (Scope scope : Scope.values()) {
                    if (text.equals(scope.text)) {
                        return scope;
                    }
                }
            }
            throw new IllegalArgumentException("Scope must be one of these values: " + Arrays.asList(Scope.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
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

    public String  getToolName() { return toolName; }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ExternalToolType> getExternalToolTypes() {
        return externalToolTypes;
    }

    public void setExternalToolTypes(List<ExternalToolType> externalToolTypes) {
        this.externalToolTypes = externalToolTypes;
    }

    public boolean isExploreTool() {
        for (ExternalToolType externalToolType : externalToolTypes) {
            if (externalToolType.getType().equals(Type.EXPLORE)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isQueryTool() {
        for (ExternalToolType externalToolType : externalToolTypes) {
            if (externalToolType.getType().equals(Type.QUERY)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isPreviewTool() {
        for (ExternalToolType externalToolType : externalToolTypes) {
            if (externalToolType.getType().equals(Type.PREVIEW)) {
                return true;
            }
        }
        return false;
    }

    public Scope getScope() {
        return scope;
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

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public JsonObjectBuilder toJson() {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("id", getId());
        jab.add(DISPLAY_NAME, getDisplayName());
        if (getToolName() != null) {
            jab.add(TOOL_NAME, getToolName());
        }
        jab.add(DESCRIPTION, getDescription());
        JsonArrayBuilder types = Json.createArrayBuilder();
        for (ExternalToolType externalToolType : externalToolTypes) {
            types.add(externalToolType.getType().text);
        }
        jab.add(TYPES, types);
        jab.add(SCOPE, getScope().text);
        jab.add(TOOL_URL, getToolUrl());
        jab.add(TOOL_PARAMETERS, getToolParameters());
        if (getContentType() != null) {
            jab.add(CONTENT_TYPE, getContentType());
        }
        if (getAllowedApiCalls()!= null) {
            jab.add(ALLOWED_API_CALLS,getAllowedApiCalls());
        }
        return jab;
    }

    public String getDescriptionLang() {
        String description = "";
        if (this.toolName != null) {
            description = (BundleUtil.getStringFromBundle("externaltools." + this.toolName + ".description"));
        } 
        if (StringUtil.isEmpty(description)) {
            description = this.getDescription();
        }
        return description;
    }

    public String getDisplayNameLang() {
        String displayName = "";
        if (this.toolName != null) {
            displayName = (BundleUtil.getStringFromBundle("externaltools." + this.toolName + ".displayname"));
        } 
        if (StringUtil.isEmpty(displayName)) {
            displayName = this.getDisplayName();
        }
        return displayName;
    }

    /**
     * @return the allowedApiCalls
     */
    public String getAllowedApiCalls() {
        return allowedApiCalls;
    }

    /**
     * @param allowedApiCalls the allowedApiCalls to set
     */
    public void setAllowedApiCalls(String allowedApiCalls) {
        this.allowedApiCalls = allowedApiCalls;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

}
