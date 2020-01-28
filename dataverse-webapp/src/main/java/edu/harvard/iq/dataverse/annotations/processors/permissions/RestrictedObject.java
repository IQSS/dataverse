package edu.harvard.iq.dataverse.annotations.processors.permissions;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class RestrictedObject {
    public final Set<DvObject> objects;
    public final String name;
    public final Set<Permission> permissions;
    public final boolean allRequired;

    // -------------------- CONSTRUCTORS --------------------

    public RestrictedObject(String name, Set<DvObject> objects, Set<Permission> permissions, boolean allRequired) {
        this.objects = objects;
        this.name = name;
        this.permissions = Collections.unmodifiableSet(permissions);
        this.allRequired = allRequired;
    }

    // -------------------- LOGIC --------------------

    public static RestrictedObject of(String name, Set<DvObject> objects, Set<Permission> permissions, boolean allRequired) {
        return new RestrictedObject(name, objects, permissions, allRequired);
    }

    // -------------------- HashCode & Equals --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestrictedObject that = (RestrictedObject) o;
        return allRequired == that.allRequired &&
                Objects.equals(objects, that.objects) &&
                name.equals(that.name) &&
                Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objects, name, permissions, allRequired);
    }


    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "RestrictedObject[" +
                "objects=" + objects +
                ", name=" + name +
                ", permissions=" + permissions +
                ", allRequired=" + allRequired +
                "]";
    }
}
