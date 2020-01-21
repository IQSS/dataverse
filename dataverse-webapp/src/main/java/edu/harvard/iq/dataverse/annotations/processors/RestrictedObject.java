package edu.harvard.iq.dataverse.annotations.processors;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class RestrictedObject {
    public final DvObject object;
    public final String name;
    public final Set<Permission> permissions;
    public final boolean allRequired;

    // -------------------- CONSTRUCTORS --------------------

    public RestrictedObject(String name, DvObject object, Set<Permission> permissions, boolean allRequired) {
        this.object = object;
        this.name = name;
        this.permissions = Collections.unmodifiableSet(permissions);
        this.allRequired = allRequired;
    }

    // -------------------- LOGIC --------------------

    public static RestrictedObject of(String name, DvObject object, Set<Permission> permissions, boolean allRequired) {
        return new RestrictedObject(name, object, permissions, allRequired);
    }

    // -------------------- HashCode & Equals --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestrictedObject that = (RestrictedObject) o;
        return allRequired == that.allRequired &&
                Objects.equals(object, that.object) &&
                name.equals(that.name) &&
                permissions.equals(that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, name, permissions, allRequired);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "RestrictedObject[" +
                "object=" + object +
                ", name=" + name +
                ", permissions=" + permissions +
                ", allRequired=" + allRequired +
                "]";
    }
}
