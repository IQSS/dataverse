/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rmp553
 */
public class AuthenticationProviderTest {
    
    public AuthenticationProviderTest() {
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
     * Create a map used to test known AuthenticationProvider ids
     * 
     * @return 
     */
    private Map<String, String> getBundleTestMap(){
        return Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>("builtin", "authenticationProvider.name.builtin"),
                new SimpleEntry<>("github", "authenticationProvider.name.github"),
                new SimpleEntry<>("google", "authenticationProvider.name.google"),
                new SimpleEntry<>("orcid", "authenticationProvider.name.orcid"),
                new SimpleEntry<>("orcid-sandbox", "authenticationProvider.name.orcid-sandbox"),
                new SimpleEntry<>("shib", "authenticationProvider.name.shib"))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
    }
    
    /**
     * Test of getFriendlyName method, of class AuthenticationProvider.
     */
    @Test
    public void testGetFriendlyName() {
        System.out.println("getFriendlyName");
        
        Map<String, String> bundleTestMap = this.getBundleTestMap();
        
        // ------------------------------------------
        // Test a null
        // ------------------------------------------
        String expResult = BundleUtil.getStringFromBundle("authenticationProvider.name.null");
        assertEquals(expResult, AuthenticationProvider.getFriendlyName(null));
        
        // ------------------------------------------
        // Test an id w/o a bundle entry--should default to id
        // ------------------------------------------
        String idNotInBundle = "id-not-in-bundle-so-use-id";
        String expResult2 = AuthenticationProvider.getFriendlyName(idNotInBundle);
        assertEquals(expResult2, idNotInBundle);
        
        // ------------------------------------------
        // Iterate through the map and test each item
        // ------------------------------------------
        bundleTestMap.forEach((authProviderId, bundleName)->{
            String expectedResult = BundleUtil.getStringFromBundle(bundleName);
            assertEquals(expectedResult, AuthenticationProvider.getFriendlyName(authProviderId));
        });    
                   
    }


    
}
