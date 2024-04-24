package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author michael
 */
public class LruCacheTest {
    
    LruCache<Long, String> sut;
    
    @BeforeEach
    public void setUp() {
        sut = new LruCache<>();
    }
    
    @AfterEach
    public void tearDown() {
        sut = null;
    }

    /**
     * Test of put and get method, of class LruCache.
     */
    @Test
    public void testGetSet() {
        String value1 = "hello";
        String value2 = "hello";

        assertEquals(value1, sut.put(1l, value1));
        assertEquals(value2, sut.put(2l, value2));
        
        assertEquals(value1, sut.get(1l));
        assertEquals(value2, sut.get(2l));
    }

    @Test
    public void testGetMaxSize() {
        assertEquals(128, sut.getMaxSize());
    }
  
    /**
     * Test of setMaxSize method, of class LruCache.
     */
    @Test
    public void testLRU() {
        int maxSize = 10;
        sut.setMaxSize(maxSize);
        sut.put(0l, "x");
        
        for ( long l=10; l<20; l++ ) {
            sut.put(l, "filler" + l);
        }
        
        assertEquals(maxSize, sut.size());
        assertNull( sut.get(0l) );
        
        for ( long l=10; l<20; l++ ) {
            assertEquals(sut.get(l), "filler" + l);
        }
        
    }

    @Test
    void testSetMaxSizeWithException() {
        assertThrows(IllegalArgumentException.class, () -> sut.setMaxSize(0l));
    }

    @Test
    public void testInvalidateWholeCache() {
        // prepare cache
        Long key = 0l;
        String value = "x";
        assertEquals(value, sut.put(key, value), "put value");
        assertEquals(value, sut.get(key), "get value");

        // invalidate cache
        sut.invalidate();

        // verify invalidation
        assertNull(sut.get(key), "verify that value is no longer here");
    }

    @Test
    public void testInvalidateOneKeyOfCache() {
        // prepare cache
        Long key1 = 0l;
        String value1 = "x";
        assertEquals(value1, sut.put(key1, value1), "put value 1");
        assertEquals(value1, sut.get(key1), "get value 1");

        Long key2 = 1l;
        String value2 = "y";
        assertEquals(value2, sut.put(key2, value2), "put value 2");
        assertEquals(value2, sut.get(key2), "get value 2");

        // invalidate cache
        sut.invalidate(key1);

        // verify invalidation
        assertNull(sut.get(key1), "verify that value 1 is no longer here");
        assertEquals(value2, sut.get(key2), "verify that value 2 still exists");
    }
}
