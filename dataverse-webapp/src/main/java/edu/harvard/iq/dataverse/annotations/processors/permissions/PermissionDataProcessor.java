package edu.harvard.iq.dataverse.annotations.processors.permissions;


import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.annotations.processors.permissions.extractors.DvObjectExtractor;
import edu.harvard.iq.dataverse.annotations.processors.permissions.extractors.CastingExtractor;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class PermissionDataProcessor {

    private final Method method;

    private final Object[] parameterValues;

    private Map<String, PermissionNeeded> permissionMap;

    // -------------------- CONSTRUCTORS --------------------

    private PermissionDataProcessor(Method method, Object[] parameterValues) {
        this.method = method;
        this.parameterValues = parameterValues;
    }

    // -------------------- LOGIC --------------------

    public static Set<RestrictedObject> gatherPermissionRequirements(Method method, Object[] parameterValues) {
        if (method == null || parameterValues == null || parameterValues.length == 0) {
            return Collections.emptySet();
        }

        return new PermissionDataProcessor(method, parameterValues)
                .processPermissionData();
    }

    // -------------------- PRIVATE --------------------

    private Set<RestrictedObject> processPermissionData() {
        permissionMap = Arrays.stream(extractAnnotations())
                .collect(Collectors.toMap(PermissionNeeded::on, Function.identity()));

        Annotation[][] annotations = method.getParameterAnnotations();
        validateAnnotationsArraySize(annotations);

        return IntStream.range(0, parameterValues.length)
                .mapToObj(i -> Tuple.of(annotations[i], parameterValues[i]))
                .flatMap(this::createNamedObjects)
                .map(this::createRestrictedObjects)
                .filter(r -> !r.permissions.isEmpty())
                .collect(Collectors.toSet());
    }

    private PermissionNeeded[] extractAnnotations() {
        return Optional.ofNullable(method.getAnnotation(Restricted.class))
                .map(Restricted::value)
                .orElseGet(() -> new PermissionNeeded[0]);
    }

    private void validateAnnotationsArraySize(Annotation[][] annotations) {
        if (annotations.length < parameterValues.length) {
            throw new IllegalStateException("Mismatch between annotation and parameter array size: too few parameters. " +
                    "Cannot properly process annotation.");
        }
    }

    private Stream<Tuple2<String, Set<DvObject>>> createNamedObjects(Tuple2<Annotation[], Object> annotationsAndObject) {
        return Arrays.stream(annotationsAndObject._1)
                .flatMap(this::extractPermissionAnnotations)
                .map(p -> Tuple.of(
                        p.value(),
                        extractDvObjects(annotationsAndObject._2, p.extractor())
                ));
    }

    private Stream<PermissionNeeded> extractPermissionAnnotations(Annotation annotation) {
        return annotation instanceof PermissionNeeded
                ? Stream.of((PermissionNeeded) annotation)
                : annotation instanceof PermissionNeeded.Container
                    ? Arrays.stream(((PermissionNeeded.Container) annotation).value())
                    : Stream.empty();
    }

    private Set<DvObject> extractDvObjects(Object object, Class<? extends DvObjectExtractor> extractorClass) {
        return flattenIfContainer(object)
                .map(o -> extractIfNeeded(o, extractorClass))
                .collect(Collectors.toSet());
    }

    private DvObject extractIfNeeded(Object object, Class<? extends DvObjectExtractor> extractorClass) {
        try {
            return extractorClass.newInstance()
                    .extract(object);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("Cannot instantiate extractor", ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private Stream<Object> flattenIfContainer(Object object) {
        Class<?> cls = object.getClass();
        return Collection.class.isAssignableFrom(cls)
                ? ((Collection) object).stream()
                : cls.isArray()
                    ? Arrays.stream((Object[]) object)
                    : Stream.of(object);
    }

    private RestrictedObject createRestrictedObjects(Tuple2<String, Set<DvObject>> nameAndObjects) {
        PermissionNeeded config = getOrThrow(permissionMap, nameAndObjects._1,
                () -> new IllegalStateException("No permission data for name: " + nameAndObjects._1));
        Set<Permission> requiredPermissions = Arrays.stream(config.needs())
                .collect(Collectors.toSet());
        return RestrictedObject.of(config.on(), nameAndObjects._2, requiredPermissions, config.allRequired());
    }

    private <K,V> V getOrThrow(Map<K,V> map, K key, Supplier<RuntimeException> exceptionSupplier) {
        if (!map.containsKey(key)) {
            throw exceptionSupplier.get();
        }
        return map.get(key);
    }
}

