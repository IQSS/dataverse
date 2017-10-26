package edu.harvard.iq.dataverse.externaltools;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ExternalTool implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * This text appears on the button and refers to a bundle key that can be
     * translated.
     */
    @Column(nullable = false)
    private String displayNameBundleKey;

    /**
     * This popup text describes the tool and refers to a bundle key that can be
     * translated.
     */
    @Column(nullable = false)
    private String descriptionBundleKey;

    /**
     * The base URL of the tool without parameters.
     */
    @Column(nullable = false)
    private String toolUrl;

    /**
     * Parameters the tool requires such as DataFile DOI and API Token
     */
    @Column(nullable = false)
    private String toolParameters;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayNameBundleKey() {
        return displayNameBundleKey;
    }

    public void setDisplayNameBundleKey(String displayNameBundleKey) {
        this.displayNameBundleKey = displayNameBundleKey;
    }

    public String getDescriptionBundleKey() {
        return descriptionBundleKey;
    }

    public void setDescriptionBundleKey(String descriptionBundleKey) {
        this.descriptionBundleKey = descriptionBundleKey;
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

}
