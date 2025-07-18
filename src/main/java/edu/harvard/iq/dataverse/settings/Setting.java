package edu.harvard.iq.dataverse.settings;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A single value in the config of dataverse.
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="Setting.deleteByName",
                query="DELETE FROM Setting s WHERE s.name=:name AND s.lang=''"),
    @NamedQuery( name="Setting.findAll",
                query="SELECT s FROM Setting s"),
    @NamedQuery( name="Setting.findAllWithoutLang",
                query="SELECT s FROM Setting s WHERE s.lang=''"),
    @NamedQuery( name="Setting.findByName",
                query="SELECT s FROM Setting s WHERE s.name=:name AND s.lang=''"),
    @NamedQuery( name="Setting.deleteByNameAndLang",
                query="DELETE FROM Setting s WHERE s.name=:name AND s.lang=:lang"),
    @NamedQuery( name="Setting.findByNameAndLang",
                query="SELECT s FROM Setting s WHERE s.name=:name AND s.lang=:lang")
})
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "UC_setting_name_lang", columnNames = {"name", "lang"}),
})
public class Setting implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String name;
    
    /**
     * The default value is an empty string, which indicates no specific language is set.
     * Using a NULL value here instead would allow the UNIQUE constraint to fail blocking duplicate settings.
     * Allowing multiple null within a UNIQUE constraint is part of the SQL standard, which Postgres follows.
     * As it stores ISO codes, 10 chars is good enough (ISO codes are 2-8 chars by spec)
     */
    @Column(length = 10, nullable = false)
    private String lang = "";

    @Column(columnDefinition = "TEXT")
    private String content;

    protected Setting() {
        // Intentionally left blank - no empty settings allowed.
        // Protected visibility to allow JPA to work.
    }

    public Setting(String name, String content) {
        Objects.requireNonNull(name, "Setting name cannot be null");
        this.name = name;
        this.content = content;
    }
    
    /**
     * Constructs a new Setting object with the specified name, language, and content.
     *
     * @param name the name of the setting; must not be null
     * @param lang the language of the setting, represented as an ISO code; must not be null;
     *             may be empty to represent a non-localized setting.
     * @param content the content or value associated with this setting
     * @throws NullPointerException if the name or lang parameters are null
     */
    public Setting(String name, String lang, String content) {
        Objects.requireNonNull(name, "Setting name cannot be null");
        Objects.requireNonNull(lang, "Setting lang cannot be null");
        this.name = name;
        this.lang = lang;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Objects.requireNonNull(name, "Setting name cannot be null");
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Retrieves the language associated with this Setting instance.
     * The language is represented as an ISO code string.
     * An empty string indicates that no specific localization is set.
     *
     * @return the language code of this Setting; never null
     */
    public String getLang() {
        return lang;
    }
    
    /**
     * Sets the language for this Setting instance.
     * The language is represented as a non-null ISO code string.
     * An empty string indicates that no specific localization shall be set.
     *
     * @param lang the language code to set; must not be null
     * @throws NullPointerException if the provided lang parameter is null
     */
    public void setLang(String lang) {
        Objects.requireNonNull(lang, "Setting lang cannot be null");
        this.lang = lang;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, lang);
    }
    
    /**
     * Compares this Setting instance to another object for equality. Two Setting
     * objects are considered equal if their {@code name} and {@code lang} fields are
     * both equal.
     * @implNote We do not use the {@code id} and {@code content} fields for the comparison.
     *           This is due to how these objects usually are used:
     *           - Mutable content to use for comparison may break collections.
     *           - Configuration management requires stable identity based on setting's name and localization.
     *             The content of the settings is irrelevant for lookups.
     *
     * @param obj the object to compare this Setting with
     * @return {@code true} if the specified object is equal to this Setting, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Setting other)) {
            return false;
        }
        return Objects.equals(this.name, other.name) && Objects.equals(this.lang, other.lang);
    }
    
    @Override
    public String toString() {
        return "[Setting name:" + getName() + " value:" + getContent() + "]";
    }


     
}
