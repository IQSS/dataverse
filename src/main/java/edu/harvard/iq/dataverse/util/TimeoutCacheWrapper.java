/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author oscardssmith
 */
public class TimeoutCacheWrapper {
    private static final ConcurrentHashMap<String, TimeoutCache> maps= new ConcurrentHashMap<>();
    
    /* */
    public static TimeoutCache addOrGet(String key, int capacity){
        return addOrGet(key, capacity, 2*1000);
    }
    
    public static TimeoutCache addOrGet(String key, int capacity, int timeout){
        if (!maps.contains(key)){
            maps.put(key, new TimeoutCache(capacity, timeout));
        }
        return maps.get(key);
    }
}
