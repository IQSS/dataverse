package edu.harvard.iq.dataverse.util;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TimeoutCache<K, V> implements Map<K, V> {

    private static final Logger logger = Logger.getLogger(TimeoutCache.class.getName());
    private class TimeNode<V> {

        public V val;
        public long time;

        public TimeNode(V val, long time) {
            this.val = val;
            this.time = time;
        }
    }

    private final int capacity;
    private final ConcurrentHashMap<K, TimeNode<V>> map;
    private final int timeout;

    /* timout in milliseconds
    */
    public TimeoutCache(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
        this.map = new ConcurrentHashMap<>(capacity);
    }

    @Override
    public V get(Object key) {
        K k = (K) key;
        TimeNode<V> value = this.map.get(k);
        long now = System.currentTimeMillis();
        if (value == null) {
            return null;
        } else if ((now - value.time) > timeout) {
            map.remove(k);
            return null;
        }
        return value.val;
    }

    @Override
    public V put(K key, V val) {
        if (!map.containsKey(key)){
            if (map.size() == this.capacity) {
                // Delete out of date entries before newer ones
                cull();
                if(map.size() == this.capacity){
                    map.remove(map.keySet().iterator().next());
                }
            }
        }
        TimeNode<V> ret = map.put(key, new TimeNode<>(val, System.currentTimeMillis()));
        if (ret == null){
            return null;
        }
        return ret.val;
    }
    
    /* becomes out of date as entries time out
    */
    @Override
    public int size() {
        return map.size();
    }

    /* becomes out of date as entries time out
    */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /* becomes out of date as entries time out
    */
    @Override
    public boolean containsKey(Object key) {
        K k = (K) key;
        return get(k) != null;
    }

    /* becomes out of date as entries time out
    */
    @Override
    public boolean containsValue(Object val) {
        V v = (V) val;
        return values().contains(v);
    }

    @Override
    public V remove(Object o) {
        return map.remove(o).val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        
        for (Entry<? extends K, ? extends V> pair : map.entrySet()) {
            put(pair.getKey(), pair.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        cull();
        return map.keySet();
    }

    @Override
    public Set<V> values() {
        cull();
        Set<V> values = new HashSet<>(capacity);
        for (TimeNode<V> val : map.values()) {
            values.add(val.val);
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        cull();
        Set<Entry<K, V>> entries = new HashSet<>(capacity);
        for (Entry<K, TimeNode<V>> entry : map.entrySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().val));
        }
        return entries;
    }
    
    /* purges old entries from the cache
    */
    private void cull() {
        long now = System.currentTimeMillis();
        Iterator<TimeNode<V>> it = map.values().iterator();
        while (it.hasNext()) {
            TimeNode<V> v = it.next();
            long dt = now - v.time;
            // dt shouldn't be less than 0, but time can be weird.
            if (dt < 0 || dt > timeout) {
                it.remove();
            }
        }
    }
}
