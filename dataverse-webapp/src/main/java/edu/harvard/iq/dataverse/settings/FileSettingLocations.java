package edu.harvard.iq.dataverse.settings;

import javax.enterprise.inject.Vetoed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Locations of properties setting files that need to be
 * loaded on application startup.
 *
 * @author madryk
 */
@Vetoed
public class FileSettingLocations {

    public enum SettingLocationType {
        CLASSPATH,
        FILESYSTEM
    }

    public enum PathType {
        DIRECT,
        PROPERTY
    }

    private List<SettingLocation> settingLocations = new ArrayList<>();
    private Map<Integer, SettingLocation> fallbackLocations = new HashMap<>();

    // -------------------- GETTERS --------------------

    public List<SettingLocation> getSettingLocations() {
        return Collections.unmodifiableList(settingLocations);
    }

    public Map<Integer, SettingLocation> getFallbackLocations() {
        return Collections.unmodifiableMap(fallbackLocations);
    }

    // -------------------- LOGIC --------------------

    public FileSettingLocations addLocation(int order, SettingLocationType locationType, String path, PathType pathType, boolean isOptional) {
        settingLocations.add(new SettingLocation(order, locationType, path, pathType, isOptional));
        return this;
    }

    public FileSettingLocations addFallbackLocation(int order, SettingLocationType locationType, String path, PathType pathType) {
        fallbackLocations.put(order, new SettingLocation(order, locationType, path, pathType, true));
        return this;
    }

    // -------------------- INNER CLASSES --------------------

    public static class SettingLocation {
        private int order;
        private SettingLocationType locationType;
        private String path;
        private PathType pathType;
        private boolean isOptional;

        // -------------------- CONSTRUCTORS --------------------

        private SettingLocation(int order, SettingLocationType locationType, String path, PathType pathType, boolean isOptional) {
            super();
            this.order = order;
            this.locationType = Objects.requireNonNull(locationType);
            this.path = Objects.requireNonNull(path);
            this.pathType = Objects.requireNonNull(pathType);
            this.isOptional = isOptional;
        }

        // -------------------- GETTERS --------------------

        public int getOrder() {
            return order;
        }

        public SettingLocationType getLocationType() {
            return locationType;
        }

        public String getPath() {
            return path;
        }

        public PathType getPathType() {
            return pathType;
        }

        public boolean isOptional() {
            return isOptional;
        }

        // -------------------- toString --------------------

        @Override
        public String toString() {
            return String.format("SettingLocation{order=%d, locationType=%s, path='%s', pathType=%s, isOptional=%s}",
                    order, locationType, path, pathType, isOptional);
        }
    }
}
