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

/**
 * A single value in the config of dataverse.
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="Setting.deleteByName",
                query="DELETE FROM Setting s WHERE s.name=:name AND s.lang IS NULL"),
    @NamedQuery( name="Setting.findAll",
                query="SELECT s FROM Setting s"),
    @NamedQuery( name="Setting.findAllWithoutLang",
                query="SELECT s FROM Setting s WHERE s.lang IS NULL"),
    @NamedQuery( name="Setting.findByName",
            query = "SELECT s FROM Setting s WHERE s.name=:name AND s.lang IS NULL" ),
    @NamedQuery( name="Setting.deleteByNameAndLang",
                query="DELETE FROM Setting s WHERE s.name=:name AND s.lang=:lang"),
    @NamedQuery( name="Setting.findByNameAndLang",
                query="SELECT s FROM Setting s WHERE s.name=:name AND s.lang=:lang")
})
@Entity
public class Setting implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String lang;

    @Column(columnDefinition = "TEXT")
    private String content;

    public Setting() {
    }

    public Setting(String name, String content) {
       this.name = name;
       this.content = content;
    }

    public Setting(String name, String lang, String content) {
        this.name = name;
        this.content = content;
        this.lang = lang;
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
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
