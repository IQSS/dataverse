package edu.harvard.iq.dataverse.util;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe implementation of a capped-size cache, where the removal is done 
 * based on the "Least Recently Used" strategy.
 * This implementation allows some scenarios where the removed values are not the
 * least recently used, in order to provide better performance.
 * 
 * @author michael
 * @param <K> Class for the cache keys
 * @param <V> Class for the cache values
 */
public class LruCache<K,V> {
    private final LinkedHashMap<K, V> cache = new LinkedHashMap<>(10, 0.75f, true);
    private final ReentrantLock cacheLock = new ReentrantLock();
    private long maxSize = 128;
    
    /**
     * @param k The key to get
     * @return The value associated with {@code k}, or {@code null}, if there isn't any.
     */
    public V get( K k ) {
        try {
            cacheLock.lock();
            return cache.get(k);
        } finally { cacheLock.unlock(); }
    }
    
    /**
     * Associates {@code k} with {@code v}.
     * @param k the key
     * @param v the value
     * @return {@code v}, to allow method call chaining.
     */
    public V put( K k, V v ) {
        try {
            cacheLock.lock();
            cache.put(k, v);
            shrinkToMaxSize();
            return v;
        } finally { cacheLock.unlock(); }
    }

    public long size() {
        try {
            cacheLock.lock();
            return cache.size();
        } finally { cacheLock.unlock(); }
    }
        
    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        if ( maxSize < 1 ) {
            throw new IllegalArgumentException("Max cache size can't be less than 1");
        }
        try {
            cacheLock.lock();
            this.maxSize = maxSize;
            shrinkToMaxSize();
        } finally { cacheLock.unlock(); }
    }
    
    public void invalidate() {
        try {
            cacheLock.lock();
            cache.clear();
        } finally { cacheLock.unlock(); }
    }
    
    public void invalidate( K k ) {
        try {
            cacheLock.lock();
            cache.remove(k);
        } finally { cacheLock.unlock(); }
    }
    
    private void shrinkToMaxSize() {
        while( cache.size() > getMaxSize() ) {
            cache.remove( cache.entrySet().iterator().next().getKey() );
        }
    }
}
