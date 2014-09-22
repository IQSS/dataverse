package edu.harvard.iq.dataverse.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class LruCacheTest {
    
    LruCache<Long, String> sut;
    
    public LruCacheTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        sut = new LruCache<>();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of get method, of class LruCache.
     */
    @Test
    public void testGetSet() {
        sut.put(1l, "hello");
        sut.put(2l, "world");
        
        assertEquals("hello", sut.get(1l));
        assertEquals("world", sut.get(2l));
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

    
    
}
