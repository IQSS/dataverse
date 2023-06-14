package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.Dataverse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumlike class to bundle available options for PIDs of Dataset Versions.
 * Must be a class to ensure case-insensitive configuration values (not possible/reliable with enum)
 */
public final class VersionPidMode {
    
    /**
     * Means feature is switched off, no version PIDs will be minted
     */
    public static final VersionPidMode OFF = new VersionPidMode("off");
    
    /**
     * Means the feature is activated, but instance wide for all collections and datasets
     * only major versions can have a PID. Enabling for minor versions is prohibited.
     */
    public static final VersionPidMode ALLOW_MAJOR = new VersionPidMode("allow-major");
    
    /**
     * Means the feature is activated and any collection may go for PIDs assigned to major and/or minor versions.
     */
    public static final VersionPidMode ALLOW_MINOR = new VersionPidMode("allow-minor");
    
    /**
     * A collection of conducts for Dataverse collections, used in {@link edu.harvard.iq.dataverse.Dataverse}
     * and {@link edu.harvard.iq.dataverse.DataverseServiceBean#wantsDatasetVersionPids(Dataverse)}:
     * <ol>
     *     <li>Collection may inherit version pid behaviour from the parent collection(s),</li>
     *     <li>collection may choose to opt out and skip the minting,</li>
     *     <li>collection may choose to activate it for major versions only, or</li>
     *     <li>collection may choose to activate it for major and minor versions.</li>
     * </ol>
     * Note that the instance wide configuration will limit available choices.
     */
    public enum CollectionConduct {
        INHERIT("inherit"),
        SKIP("skip"),
        MAJOR("major"),
        MINOR("minor");
        
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
        
        public static List<String> asList() {
            return values.stream().map(Object::toString).collect(Collectors.toList());
        }
    }
    
    /**
     * Defining a list of styles how to generate the version PIDs.
     * This is not extensible from the outside on purpose.
     * This is a class to enable auto-conversion via MicroProfile Config
     * also with lowercase setting values.
     */
    public static final class GenStyle {
        
        public static final GenStyle DATASET = new GenStyle("DATASET");
        public static final GenStyle SUFFIX = new GenStyle("SUFFIX");
    
        // Init as unmodifiable set
        public static final Set<GenStyle> values;
        static {
            values = Set.of(DATASET, SUFFIX);
        }
        
        private final String style;
    
        GenStyle(String style) {
            this.style = style;
        }
    
        @Override
        public String toString() {
            return this.style;
        }
    
        public static GenStyle of(String style) {
            return values.stream()
                .filter(m -> m.style.equalsIgnoreCase(style))
                .findAny()
                .orElse(null);
        }
    }
    
    
    // Init as unmodifiable set
    public static final Set<VersionPidMode> values;
    static {
        values = Set.of(OFF, ALLOW_MAJOR, ALLOW_MINOR);
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
