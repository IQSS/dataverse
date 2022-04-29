package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Persistence for Saml groups.
 */
@Entity
@Table(name = "samlgroup")
@NamedQueries({
    @NamedQuery(name = "SamlGroup.findByEntityId", query = "SELECT g FROM SamlGroup g WHERE g.entityId = :entityId")
})
public class SamlGroup implements JpaEntity<Long>, Group, Serializable {

    public final static String GROUP_TYPE = "saml";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the group that will be displayed to the end user.
     */
    @Column(nullable = false)
    private String name;

    /**
     * EntityId of the identity provider.
     */
    @Column(name = "entityid", nullable = false)
    private String entityId;

    // -------------------- CONSTRUCTORS --------------------

    public SamlGroup() { }

    public SamlGroup(String name, String entityId) {
        this.name = name;
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return "SamlGroup{" + "id=" + id + ", name=" + name + ", entityId=" + entityId + '}';
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public String getAlias() {
        return GROUP_TYPE + Group.PATH_SEPARATOR + getId().toString();
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEditable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * i.e. &shib/1
     */
    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + GROUP_TYPE + Group.PATH_SEPARATOR + getId();
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(getName(), null);
    }

    // -------------------- equals & hashCode --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SamlGroup samlGroup = (SamlGroup) o;
        return Objects.equals(id, samlGroup.id)
                && name.equals(samlGroup.name)
                && entityId.equals(samlGroup.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, entityId);
    }
}
