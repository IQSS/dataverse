package edu.harvard.iq.dataverse.annotations.processors;

import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import io.vavr.collection.HashSet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

class PermissionDataProcessorTest {

    private static final Dataset FIRST = new Dataset();
    private static final Dataset SECOND = new Dataset();
    private static final Dataset THIRD = new Dataset();
    private static final Dataverse OWNER = new Dataverse();

    private static final Annotated ANNOTATED_CLASS = new Annotated();

    private final PermissionDataProcessor processor = new PermissionDataProcessor();

    static {
        FIRST.setId(1L);
        FIRST.setOwner(OWNER);
        SECOND.setId(2L);
        THIRD.setId(3L);
    }

    // -------------------- TESTS --------------------

    @Test
    void shouldReturnEmptySetForNoAnnotations() {
        // given
        Method method = getMethod("notAnnotated", ANNOTATED_CLASS);
        Object[] parameterValues = new Object[] {FIRST, SECOND};

        // when
        Set<RestrictedObject> result = processor.gatherPermissionRequirements(method, parameterValues);

        // then
        assertThat("Empty set should be returned for not annotated method",
                result, empty());
    }

    @Test
    void shouldAssignUnnamedRequirementsToUnnamedObjects() {
        // given
        Method method = getMethod("unnamedOnly", ANNOTATED_CLASS);
        Object[] parameterValues = new Object[] {FIRST, SECOND};

        // when
        Set<RestrictedObject> result = processor.gatherPermissionRequirements(method, parameterValues);

        // then
        Set<Set<Permission>> setsOfPermissions = result.stream()
                .map(r -> r.permissions)
                .collect(toSet());
        assertThat("Two element set should be returned",
                result, hasSize(2));
        assertThat("Every element of result should have two permissions",
                setsOfPermissions, everyItem(hasSize(2)));
        assertThat("Unnamed permissions should be added to all unnamed parameters (all in this case)",
                setsOfPermissions, everyItem(containsInAnyOrder(Permission.EditDataset, Permission.AddDataset)));
    }

    @Test
    void shouldProperlyAssignUnnamedAndNamedPermissions() {
        // given
        Method method = getMethod("mixed", ANNOTATED_CLASS);
        Object[] parameterValues = new Object[] {FIRST, SECOND};

        // when
        Set<RestrictedObject> result = processor.gatherPermissionRequirements(method, parameterValues);

        // then
        Set<RestrictedObject> expected = HashSet.of(
                RestrictedObject.of("source", FIRST, Collections.singleton(Permission.EditDataset), false),
                RestrictedObject.of("", SECOND, Collections.singleton(Permission.AddDataset), false))
                .toJavaSet();

        assertThat("Result should be equal to expected set",
                result, equalTo(expected));
    }

    @Test
    void shouldAssignPermissionsToOwnerIfNeeded() {
        // given
        Method method = getMethod("withOwner", ANNOTATED_CLASS);
        Object[] parameterValues = new Object[] {FIRST};

        // when
        Set<RestrictedObject> result = processor.gatherPermissionRequirements(method, parameterValues);

        // then
        Set<RestrictedObject> expected = HashSet.of(
                RestrictedObject.of("source", FIRST, Collections.singleton(Permission.EditDataset), false),
                RestrictedObject.of("source_owner", OWNER, Collections.singleton(Permission.AddDataset), false))
                .toJavaSet();

        assertThat("Result should be equal to expected set",
                result, equalTo(expected));

    }

    @Test
    void shouldAssingPermissionsInProperOrder() {
        // given
        Method method = getMethod("multipleNames", ANNOTATED_CLASS);
        Object[] parameterValues = new Object[] {"someString", FIRST, SECOND, THIRD};

        // when
        Set<RestrictedObject> result = processor.gatherPermissionRequirements(method, parameterValues);

        // then
        Set<RestrictedObject> expected = HashSet.of(
                RestrictedObject.of("target", FIRST, Collections.singleton(Permission.EditDataset), false),
                RestrictedObject.of("aux", SECOND, Collections.singleton(Permission.EditDataset), false),
                RestrictedObject.of("source", THIRD, Collections.singleton(Permission.EditDataset), false))
                .toJavaSet();

        assertThat("Result should be equal to expected set",
                result, equalTo(expected));
    }

    // -------------------- PRIVATE --------------------

    private Method getMethod(String name, Annotated object) {
        Class<? extends Annotated> cls = object.getClass();
        return Arrays.stream(cls.getMethods())
                .filter(m -> name.equals(m.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method not found: " + name));
    }

    // -------------------- INNER CLASSES --------------------

    public static class Annotated {
        public void notAnnotated(DvObject o1, DvObject o2) {
            ;
        }

        @Restricted({
                @PermissionNeeded(needs = {Permission.EditDataset, Permission.AddDataset})
        })
        public void unnamedOnly(@PermissionNeeded DvObject o1, @PermissionNeeded DvObject o2) {
            ;
        }

        @Restricted({
                @PermissionNeeded(on = "source", needs = {Permission.EditDataset}),
                @PermissionNeeded(needs = {Permission.AddDataset})
        })
        public void mixed(@PermissionNeeded("source") DvObject o1, @PermissionNeeded DvObject o2) {
            ;
        }

        @Restricted({
                @PermissionNeeded(on = "source", needs = {Permission.EditDataset}, needsOnOwner = {Permission.AddDataset})
        })
        public void withOwner(@PermissionNeeded("source") DvObject o1) {
            ;
        }

        @Restricted({
                @PermissionNeeded(on = "source", needs = {Permission.EditDataset}),
                @PermissionNeeded(on = "target", needs = {Permission.EditDataset}),
                @PermissionNeeded(on = "aux", needs = {Permission.EditDataset})
        })
        public void multipleNames(String message,
                                  @PermissionNeeded("target") DvObject o1,
                                  @PermissionNeeded("aux") DvObject o2,
                                  @PermissionNeeded("source") DvObject o3) {
            ;
        }
    }
}