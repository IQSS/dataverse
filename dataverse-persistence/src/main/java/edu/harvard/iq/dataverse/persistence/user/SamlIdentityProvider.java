package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@NamedQuery(name = "SamlIdentityProvider.findByEntityId",
        query = "SELECT s FROM SamlIdentityProvider s WHERE s.entityId = :entityId")
public class SamlIdentityProvider implements JpaEntity<Long>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entityid", nullable = false, unique = true)
    private String entityId;

    @Column(name ="metadataurl", nullable = false)
    private String metadataUrl;

    @Column(name = "displayname", nullable = false)
    private String displayName;

    @Column(name = "configurationxml")
    private String configurationXml;

    @Column(name = "lasttimeofxmldownload")
    private Timestamp lastTimeOfXmlDownload;

    // -------------------- CONSTRUCTORS --------------------

    public SamlIdentityProvider() { }

    public SamlIdentityProvider(Long id, String entityId, String metadataUrl, String displayName) {
        this.id = id;
        this.entityId = entityId;
        this.metadataUrl = metadataUrl;
        this.displayName = displayName;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getConfigurationXml() {
        return configurationXml;
    }

    public Timestamp getLastTimeOfXmlDownload() {
        return lastTimeOfXmlDownload;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setConfigurationXml(String configurationXml) {
        this.configurationXml = configurationXml;
    }

    public void setLastTimeOfXmlDownload(Timestamp lastTimeOfXmlDownload) {
        this.lastTimeOfXmlDownload = lastTimeOfXmlDownload;
    }
}
