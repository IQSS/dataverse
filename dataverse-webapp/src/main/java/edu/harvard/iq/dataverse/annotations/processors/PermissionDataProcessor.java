package edu.harvard.iq.dataverse.annotations.processors;


import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
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

    // -------------------- LOGIC --------------------

    public Set<RestrictedObject> gatherPermissionRequirements(Method method, Object[] parameterValues) {
        if (method == null || parameterValues == null || parameterValues.length == 0) {
            return Collections.emptySet();
        }

        Map<String, PermissionNeeded> permissionMap = createPermissionMap(extractAnnotations(method));

        return createNamedParameterStream(method, parameterValues)
                .flatMap(t -> createRestrictedObjects(t._2,
                        getOrThrow(permissionMap, t._1,
                            () -> new IllegalStateException("No permission data for name: " + t._1))
                        )
                )
                .filter(r -> !r.permissions.isEmpty())
                .collect(Collectors.toSet());
    }

    // -------------------- PRIVATE --------------------

    private <K,V> V getOrThrow(Map<K,V> map, K key, Supplier<RuntimeException> exceptionSupplier) {
        if (!map.containsKey(key)) {
            throw exceptionSupplier.get();
        }
        return map.get(key);
    }

    private Map<String, PermissionNeeded> createPermissionMap(PermissionNeeded[] configuration) {
        return Arrays.stream(configuration)
                .collect(Collectors.toMap(PermissionNeeded::on, Function.identity()));
    }

    private PermissionNeeded[] extractAnnotations(Method method) {
        return Optional.ofNullable(method.getAnnotation(Restricted.class))
                .map(Restricted::value)
                .orElseGet(() -> new PermissionNeeded[0]);
    }

    private Stream<RestrictedObject> createRestrictedObjects(DvObject object, PermissionNeeded config) {
        return Stream.of(
                RestrictedObject.of(config.on(), object, toSet(config.needs()), config.allRequired()),
                RestrictedObject.of(config.on() + "_owner", object != null ? object.getOwner() : null,
                        toSet(config.needsOnOwner()), config.allRequired())
        );
    }

    private Set<Permission> toSet(Permission[] array) {
        return Arrays.stream(array)
                .collect(Collectors.toSet());
    }

    private Stream<Tuple2<String, DvObject>> createNamedParameterStream(Method method, Object[] parameterValues) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (annotations.length < parameterValues.length) {
            throw new IllegalStateException("Mismatch between annotation and parameter array size: too few parameters. " +
                    "Cannot properly process annotation.");
        }

        return IntStream.range(0, parameterValues.length)
                .mapToObj(i -> Tuple.of(annotations[i], parameterValues[i]))
                .map(t -> Tuple.of(extractNameIfAnnotationExists(t._1), t._2))
                .filter(t -> t._1.isPresent())
                .map(t -> Tuple.of(t._1.get(), (DvObject) t._2));
    }

    private Optional<String> extractNameIfAnnotationExists(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .filter(a -> a instanceof PermissionNeeded)
                .map(PermissionNeeded.class::cast)
                .map(PermissionNeeded::value)
                .findFirst(); // it's safe to call, because PermissionNeeded.value is always non-null
    }
}
