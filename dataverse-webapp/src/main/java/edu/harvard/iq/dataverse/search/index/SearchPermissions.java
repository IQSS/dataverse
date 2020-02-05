package edu.harvard.iq.dataverse.search.index;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * @author madryk
 */
public class SearchPermissions {
    
    /**
     * Special value of {@link #getPublicFrom()} indicating that dvobject
     * will not ever could be seen by all users
     */
    public final static Instant NEVER_PUBLIC = Instant.ofEpochMilli(Long.MAX_VALUE);
    
    /**
     * Special value of {@link #getPublicFrom()} indicating that dvobject
     * can be seen by all users
     */
    public final static Instant ALWAYS_PUBLIC = Instant.EPOCH;
    
    
    private List<String> permissions;
    private Instant publicFrom;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public SearchPermissions(List<String> permissions, Instant publicFrom) {
        this.permissions = permissions;
        this.publicFrom = publicFrom;
    }

    // -------------------- GETTERS --------------------
    
    /**
     * Returns list of users and groups (formatted to solr specific string)
     * that will be able to see dvobject
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Returns {@link Instant} that points to time when dvobject will
     * be seen by all (bypassing assigned {@link #getPermissions()} restrictions).
     * 
     * @see {@link #NEVER_PUBLIC}
     * @see {@link #ALWAYS_PUBLIC}
     */
    public Instant getPublicFrom() {
        return publicFrom;
    }

    // -------------------- hashCode & equals --------------------
    
    @Override
    public int hashCode() {
        return Objects.hash(permissions, publicFrom);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SearchPermissions other = (SearchPermissions) obj;
        return Objects.equals(permissions, other.permissions) && Objects.equals(publicFrom, other.publicFrom);
    }

    // -------------------- toString --------------------
    
    @Override
    public String toString() {
        return "SearchPermissions [permissions=" + permissions + ", publicFrom=" + publicFrom + "]";
    }
}