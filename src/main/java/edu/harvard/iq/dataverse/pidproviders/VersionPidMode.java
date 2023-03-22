package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.Dataverse;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * Enumlike class to bundle available options for PIDs of Dataset Versions.
 * Must be a class to ensure case-insensitive configuration values (not possible/reliable with enum)
 */
public final class VersionPidMode {
    
    /**
     * Means feature is switched off, no version PIDs will be minted
     */
    public static final VersionPidMode OFF = new VersionPidMode("OFF");
    
    /**
     * Means the feature is activated instance wide for all collections and datasets
     * (No opt-out so far!)
     */
    public static final VersionPidMode GLOBAL = new VersionPidMode("GLOBAL");
    
    /**
     * Means the feature must be activated per Dataverse Collection (opt-in)
     */
    public static final VersionPidMode COLLECTION = new VersionPidMode("COLLECTION");
    
    /**
     * A collection of conducts for mode {@link #COLLECTION}, used in {@link edu.harvard.iq.dataverse.Dataverse}
     * and {@link edu.harvard.iq.dataverse.DataverseServiceBean#wantsDatasetVersionPids(Dataverse)}:
     * <ol>
     *     <li>Collection may inherit version pid behaviour from the parent collection(s),</li>
     *     <li>Collection may choose to be actively enabling it</li>
     *     <li>Collection may choose to opt out and skip the minting</li>
     * </ol>
     */
    public enum CollectionConduct {
        INHERIT("inherit"),
        ACTIVE("active"),
        SKIP("skip");
        
        private final String name;
        
        CollectionConduct(String name) {
            this.name = name;
        }
    
        @Override
        public String toString() {
            return this.name;
        }
        
        public static CollectionConduct findBy(String name) {
            return Arrays.stream(CollectionConduct.values())
                .filter(cs -> cs.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        }
    }
    
    
    // Init as unmodifiable set
    public static final Set<VersionPidMode> values;
    static {
        values = Set.of(OFF, GLOBAL, COLLECTION);
    }
    
    private final String mode;
    
    // Hide the no-arg constructor - no one shall get fancy ideas of extension.
    private VersionPidMode() {
        this.mode = "";
    }
    
    // Hide the constructor - no one shall get fancy ideas of extension.
    private VersionPidMode(String mode) {
        this.mode = mode;
    }
    
    /**
     * Used to enable auto-conversion for MicroProfile Config.
     * Comparison is done case-insensitive to enable all variants of writing the config value.
     *
     * Note that a non-matching value will return null, which will trigger a {@link java.util.NoSuchElementException}
     * for the conversion.
     *
     * @see <a href="https://download.eclipse.org/microprofile/microprofile-config-3.0/microprofile-config-spec-3.0.html#_automatic_converters">MicroProfile Config Spec for Autoconverts</a>
     *
     * @param mode The mode to lookup
     * @return A matching constant or null if not matching.
     */
    public static VersionPidMode of(String mode) {
        return values.stream()
            .filter(m -> m.mode.equalsIgnoreCase(mode))
            .findAny()
            .orElse(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionPidMode)) return false;
        VersionPidMode that = (VersionPidMode) o;
        return mode.equalsIgnoreCase(that.mode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(mode);
    }
}
