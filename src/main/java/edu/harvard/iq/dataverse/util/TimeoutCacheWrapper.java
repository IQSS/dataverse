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
    
    /* Convienience method for a 1 second cache with 300 entries*/
    public static TimeoutCache addOrGet(String key){
        return addOrGet(key, 300);
    }
    
    /* Convienience method for a 1 second cache*/
    public static TimeoutCache addOrGet(String key, int capacity){
        return addOrGet(key, capacity, 1000);
    }
    
    public static TimeoutCache addOrGet(String key, int capacity, int timeout){
        if (!maps.contains(key)){
            maps.put(key, new TimeoutCache(capacity, timeout));
        }
        return maps.get(key);
    }
}
