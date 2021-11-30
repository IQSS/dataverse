package edu.harvard.iq.dataverse.api.helpers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ExtensionStoreHelper {
    
    private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> beforeAfterMap =
        Map.of(BeforeEach.class, BeforeEach.class,
               BeforeAll.class, BeforeAll.class,
               AfterEach.class, BeforeEach.class,
               AfterAll.class, BeforeAll.class);
    
    /**
     * Exposing function to retrieve the correct object to build the scope for the @BeforeEach store
     * @return BeforeEach.class
     */
    public static Object getAfterEachScope() {
        return beforeAfterMap.get(AfterEach.class);
    }
    
    /**
     * Exposing function to retrieve the correct object to build the scope for the @BeforeAll store
     * @return BeforeAll.class
     */
    public static Object getAfterAllScope() {
        return beforeAfterMap.get(AfterAll.class);
    }
    
    
    public static Store getStore(Class<? extends Extension> extension, ExtensionContext context, Object thirdScopeElement) {
        return context.getStore(Namespace.create(extension, context.getRequiredTestClass(), thirdScopeElement));
    }
    
    /**
     * Retrieve the extension store, already properly namespaced and ready to use.
     * @param extension The extension class (first scope)
     * @param extensionContext The general context of the extensions (injected by JUnit). May not be null.
     * @param parameterContext If using from a {@link org.junit.jupiter.api.extension.ParameterResolver}, the context of the parameter. May be null.
     * @return The store ready to be used as a map
     */
    public static Store getStore(Class<? extends Extension> extension, ExtensionContext extensionContext, ParameterContext parameterContext) {
        if (extensionContext == null) {
            throw new IllegalArgumentException("No valid ExtensionContext given");
        }
        
        Object thirdScopeElement;
        // When given a parameter context, this is NOT a request from a before/after callback but a parameter resolver.
        // We need to either extract a parameter of a test method or a before/after annotated method and provide the
        // correct store.
        Optional<? extends Class<? extends Annotation>> oScope = getAnnotationStoreScope(parameterContext);
        
        // Note: this cannot be refactored into Optional.orElse as Method is not inheriting from Class!
        if (oScope.isPresent()) {
            thirdScopeElement = oScope.get();
        } else {
            thirdScopeElement = extensionContext.getRequiredTestMethod();
        }
        
        return getStore(extension, extensionContext, thirdScopeElement);
    }
    
    /**
     * A convenience method to store a list item within a given store and given store key.
     * The list is initialized if not present within the store.
     *
     * @param store A namespaces store, retrievable for example via {@link #getStore(Class, ExtensionContext, Object)}
     * @param key A key to lookup and save the list within the store.
     * @param newListItem The actual item to be saved within the list
     */
    public static <T> void addToStoredList(Store store, Object key, T newListItem) {
        List<T> list = getStoredList(store, key);
        list.add(newListItem);
        
        store.put(key, list);
    }
    
    /**
     * Get a list from the store or create a new, empty one.
     *
     * Be aware to keep track of the types you use, as this uses an unchecked conversion which might cause
     * a {@link ClassCastException}!
     *
     * @param store The scope store to retrieve the list from
     * @param key The key under which the store saves the desired list object
     * @return The found and cast list or a brand new list.
     * @throws ClassCastException if the objects in the store cannot be deserialized to the desired type.
     */
    public static <T> List<T> getStoredList(Store store, Object key) {
        @SuppressWarnings({"unchecked"})
        List<T> stored = (List<T>)store.get(key, List.class);
    
        return Objects.requireNonNullElseGet(stored, ArrayList::new);
    }
    
    /**
     * Check if the parameter is trying to be resolved from an After type method.
     * @param parameterContext The parameter context. May be null.
     * @return true if After type, false if context is null or not After type
     */
    public static boolean isAfterMethod(ParameterContext parameterContext) {
        return extractMatchingAnnotations(parameterContext)
            // filter for After types only
            .anyMatch(c -> c == AfterEach.class || c == AfterAll.class);
    }
    
    /**
     * Check if the parameter is trying to be resolved from an Before type method.
     * @param parameterContext The parameter context. May be null.
     * @return true if Before type, false if context is null or not Before type
     */
    public static boolean isBeforeMethod(ParameterContext parameterContext) {
        return extractMatchingAnnotations(parameterContext)
            // filter for After types only
            .anyMatch(c -> c == BeforeEach.class || c == BeforeAll.class);
    }
    
    /**
     * Retrieve the annotation class for Before/After JUnit methods and map to a store scope key.
     *
     * Note: AfterEach and AfterAll will be mapped to BeforeEach and BeforeAll as these are usually used to access
     * something stored *before* the test that needs to be cleanup up *after* the test, so we need the correct scope.
     *
     * @param parameterContext The JUnit 5 extension provided parameter context when
     *                         using a {@link org.junit.jupiter.api.extension.ParameterResolver}.
     * @return The scope key or, if this is not a method with the JUnit before/after annotation, an empty optional
     */
    private static Optional<? extends Class<? extends Annotation>> getAnnotationStoreScope(ParameterContext parameterContext) {
        return extractMatchingAnnotations(parameterContext)
            // get mapped class (to invert the after annotations)
            .map(beforeAfterMap::get)
            // return first as optional (as there might be no annotation present)
            .findFirst();
    }
    
    private static Stream<? extends Class<? extends Annotation>> extractMatchingAnnotations(ParameterContext parameterContext) {
        if (parameterContext == null) {
            return Stream.empty();
        }
        return
            // get all annotations from the method
            Arrays.stream(parameterContext.getDeclaringExecutable().getDeclaredAnnotations())
            // retrieve class of annotation
            .map(Annotation::annotationType)
            // filter by presence in map
            .filter(beforeAfterMap::containsKey);
    }
}
