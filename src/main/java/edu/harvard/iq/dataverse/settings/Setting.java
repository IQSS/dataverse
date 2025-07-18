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

    @Column(columnDefinition = "TEXT")
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

    public Setting() {
    }

    public Setting(String name, String content) {
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
        Objects.requireNonNull(lang, "Setting lang cannot be null");
        this.name = name;
        this.lang = lang;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
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
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( !(obj instanceof Setting) ) {
            return false;
        }
        final Setting other = (Setting) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.content, other.content);
    }

    @Override
    public String toString() {
        return "[Setting name:" + getName() + " value:" + getContent() + "]";
    }


     
}
