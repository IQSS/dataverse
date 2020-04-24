package edu.harvard.iq.dataverse.importers.ui;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This interface is used to simplify access to implementing class with EL, e.g.
 * instead of writing:
 * <br> <code>#{someBean.someClass.someMap[property]}</code>
 * <br> we could write:
 * <br> <code>#{someBean.someImplementingClass[property]}</code>.
 * <p>Please take care however, because when using class as a map, every property
 * is looked for inside the underlying map, and not in the class, so every property
 * should be get with the proper accessor, e.g.
 * <br> <code>#{someImplementingClass.getProperty()}</code>
 * <br> and not:
 * <br> <code>#{someImplementingClass.property}</code>.
 */
public interface MapForView<K,V> extends Map<K,V> {

    Map<K,V> getUnderlyingMap();

    // -------------------- DEFAULT --------------------

    @Override
    default int size() {
        return getUnderlyingMap().size();
    }

    @Override
    default boolean isEmpty() {
        return getUnderlyingMap().isEmpty();
    }

    @Override
    default boolean containsKey(Object key) {
        return getUnderlyingMap().containsKey(key);
    }

    @Override
    default boolean containsValue(Object value) {
        return getUnderlyingMap().containsValue(value);
    }

    @Override
    default V get(Object key) {
        return getUnderlyingMap().get(key);
    }

    @Override
    default V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    default V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<K> keySet() {
        return getUnderlyingMap().keySet();
    }

    @Override
    default Collection<V> values() {
        return getUnderlyingMap().values();
    }

    @Override
    default Set<Entry<K, V>> entrySet() {
        return getUnderlyingMap().entrySet();
    }
}
