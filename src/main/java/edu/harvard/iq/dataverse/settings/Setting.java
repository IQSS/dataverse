package edu.harvard.iq.dataverse.settings;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * A single value in the config of dataverse.
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="Setting.deleteByName",
                query="DELETE FROM Setting s WHERE s.name=:name"),
    @NamedQuery( name="Setting.findAll",
                query="SELECT s FROM Setting s"),
    @NamedQuery( name="Setting.deleteByNameAndLang",
                query="DELETE FROM Setting s WHERE s.name=:name AND s.lang=:lang"),
    @NamedQuery( name="Setting.findByNameAndLang",
                query = "SELECT s FROM Setting s WHERE s.name=:name AND s.lang=:lang" ),

})
@Entity
public class Setting implements Serializable {
    
    @Id
    private String name;

    @Id
    @Column(columnDefinition = "TEXT")
    private String lang;

    @Column(columnDefinition = "TEXT")
    private String content;

    public Setting() {
    }

    public Setting(String name, String content) {
       this.name = name;
       this.content = content;
       this.lang = "en";
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
