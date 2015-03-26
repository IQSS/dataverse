package edu.harvard.iq.dataverse.search;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class IndexUtilTest {

    public IndexUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of findDvObjectIdsToProcess method, of class IndexUtil.
     */
    @Test
    public void testFindDvObjectIdsToProcess() {
//        System.out.println("findDvObjectIdsToProcess");
        assertEquals(Arrays.asList(1l, 3l, 5l, 7l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 2, 1));
        assertEquals(Arrays.asList(2l, 4l, 6l, 8l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 2, 0));

        assertEquals(Arrays.asList(1l, 4l, 7l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 3, 1));
        assertEquals(Arrays.asList(2l, 5l, 8l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 3, 2));
        assertEquals(Arrays.asList(3l, 6l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 3, 0));

        assertEquals(Arrays.asList(1l, 5l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 4, 1));
        assertEquals(Arrays.asList(2l, 6l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 4, 2));
        assertEquals(Arrays.asList(3l, 7l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 4, 3));
        assertEquals(Arrays.asList(4l, 8l), IndexUtil.findDvObjectIdsToProcessMod(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), 4, 0));

    }

}
