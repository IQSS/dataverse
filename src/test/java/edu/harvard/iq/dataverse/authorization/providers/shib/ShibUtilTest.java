package edu.harvard.iq.dataverse.authorization.providers.shib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShibUtilTest {

    public ShibUtilTest() {
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
     * Test of getDisplayNameFromDiscoFeed method, of class ShibUtil.
     */
    @Test
    public void testGetDisplayNameFromDiscoFeed() {
//        System.out.println("getDisplayNameFromDiscoFeed");

        String discoFeedExample = null;
        try {
            discoFeedExample = new String(Files.readAllBytes(Paths.get("src/main/webapp/resources/dev/sample-shib-identities.json")));
        } catch (IOException ex) {
            Logger.getLogger(ShibUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ShibUtil shibUtil = new ShibUtil();

        String testShib = shibUtil.getDisplayNameFromDiscoFeed("https://idp.testshib.org/idp/shibboleth", discoFeedExample);
        assertEquals("TestShib Test IdP", testShib);

        String harvardStage = shibUtil.getDisplayNameFromDiscoFeed("https://stage.fed.huit.harvard.edu/idp/shibboleth", discoFeedExample);
        assertEquals("Harvard Test IdP", harvardStage);

        String unknown = shibUtil.getDisplayNameFromDiscoFeed("https://nosuchdomain.com/idp/shibboleth", discoFeedExample);
        assertEquals(null, unknown);

        String searchForNull = shibUtil.getDisplayNameFromDiscoFeed(null, discoFeedExample);
        assertEquals(null, searchForNull);

        /**
         * @todo Is it an error to pass a null discoFeed?
         */
//        String nullDiscoFeed = shibUtil.getDisplayNameFromDiscoFeed("https://idp.testshib.org/idp/shibboleth", null);
//        assertEquals(null, nullDiscoFeed);
        /**
         * @todo Is it an error to pass junk (non-JSON) as a discoFeed?
         */
//        String unparseAbleDiscoFeed = shibUtil.getDisplayNameFromDiscoFeed("https://idp.testshib.org/idp/shibboleth", "unparseAbleAsJson");
//        assertEquals(null, unparseAbleDiscoFeed);
    }

}
