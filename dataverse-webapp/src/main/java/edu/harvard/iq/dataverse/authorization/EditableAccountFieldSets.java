package edu.harvard.iq.dataverse.authorization;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EditableAccountFieldSets {
    private static final Set<EditableAccountField> ALL = Initializer.createAllFieldsSet();
    private static final Set<EditableAccountField> SECONDARY = Initializer.createSecondaryFieldsSet();
    private static final Set<EditableAccountField> NONE = Initializer.createEmptySet();

    public static Set<EditableAccountField> allFields() {
        return ALL;
    }

    public static Set<EditableAccountField> secondaryFields() {
        return SECONDARY;
    }

    public static Set<EditableAccountField> noFields() {
        return NONE;
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        public static Set<EditableAccountField> createAllFieldsSet() {
            return Collections.unmodifiableSet(
                    Stream.of(EditableAccountField.values())
                            .collect(Collectors.toSet()));
        }

        public static Set<EditableAccountField> createSecondaryFieldsSet() {
            return Collections.unmodifiableSet(
                    Stream.of(EditableAccountField.AFFILIATION, EditableAccountField.POSITION, EditableAccountField.NOTIFICATIONS_LANG)
                            .collect(Collectors.toSet()));
        }

        public static Set<EditableAccountField> createEmptySet() {
            return Collections.emptySet();
        }
    }
}
