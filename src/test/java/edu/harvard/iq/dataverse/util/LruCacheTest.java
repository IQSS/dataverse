package edu.harvard.iq.dataverse.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class LruCacheTest {
    
    LruCache<Long, String> sut;
    
    @Before
    public void setUp() {
        sut = new LruCache<>();
    }
    
    @After
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

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxSizeWithException() {
        sut.setMaxSize(0l);
    }

    @Test
    public void testInvalidateWholeCache() {
        // prepare cache
        Long key = 0l;
        String value = "x";
        assertEquals("put value", value, sut.put(key, value));
        assertEquals("get value", value, sut.get(key));

        // invalidate cache
        sut.invalidate();

        // verify invalidation
        assertEquals("verify that value is no longer here", null, sut.get(key));
    }

    @Test
    public void testInvalidateOneKeyOfCache() {
        // prepare cache
        Long key1 = 0l;
        String value1 = "x";
        assertEquals("put value 1", value1, sut.put(key1, value1));
        assertEquals("get value 1", value1, sut.get(key1));

        Long key2 = 1l;
        String value2 = "y";
        assertEquals("put value 2", value2, sut.put(key2, value2));
        assertEquals("get value 2", value2, sut.get(key2));

        // invalidate cache
        sut.invalidate(key1);

        // verify invalidation
        assertEquals("verify that value 1 is no longer here", null, sut.get(key1));
        assertEquals("verify that value 2 still exists", value2, sut.get(key2));
    }
}
