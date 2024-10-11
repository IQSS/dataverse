package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author michael
 */
public class LruCacheTest {

    LruCache<Long, String> sut;

    public LruCacheTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        sut = new LruCache<>();
    }

    @AfterEach
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

        for (long l = 10; l < 20; l++) {
            sut.put(l, "filler" + l);
        }

        assertEquals(maxSize, sut.size());
        assertNull(sut.get(0l));

        for (long l = 10; l < 20; l++) {
            assertEquals(sut.get(l), "filler" + l);
        }

    }


}
