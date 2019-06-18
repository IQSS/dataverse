package edu.harvard.iq.dataverse.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.Serializable;
import java.util.Objects;

/**
 * A single value in the config of dataverse.
 *
 * @author michael
 */
@NamedQueries({
        @NamedQuery(name = "Setting.deleteByName",
                query = "DELETE FROM Setting s WHERE s.name=:name"),
        @NamedQuery(name = "Setting.findAll",
                query = "SELECT s FROM Setting s")
})
@Entity
public class Setting implements Serializable {

    @Id
    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    // -------------------- CONSTRUCTORS --------------------

    public Setting() {
    }

    public Setting(String name, String content) {
        this.name = name;
        this.content = content;
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    // -------------------- SETTERS --------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // -------------------- hashCode & equals --------------------

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
        if (!(obj instanceof Setting)) {
            return false;
        }
        final Setting other = (Setting) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.content, other.content);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[Setting name:" + getName() + " value:" + getContent() + "]";
    }


}
