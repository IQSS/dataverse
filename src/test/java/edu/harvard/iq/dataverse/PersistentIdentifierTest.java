/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.Optional;
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
public class PersistentIdentifierTest {
    
    public PersistentIdentifierTest() {
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
     * Test of parse method, of class PersistentIdentifier.
     */
    @Test
    public void testParseValid() {
        PersistentIdentifier pid = PersistentIdentifier.parse("doi:auth:idtf", ":").get();
        assertEquals("doi", pid.getProtocol());
        assertEquals("auth", pid.getAuthority());
        assertEquals("idtf", pid.getIdentifier());
    }
    
    @Test
    public void testParseInvalid() {
        assertFalse( PersistentIdentifier.parse("noProtocol", ":").isPresent() );
        assertFalse( PersistentIdentifier.parse("", ":").isPresent() );
        assertFalse( PersistentIdentifier.parse(null, ":").isPresent() );
    }
    
    @Test
    public void testParseRoundtrip() {
        assertEquals("doi:auth:idtf", 
            PersistentIdentifier.parse("doi:auth:idtf", ":").get().toString());
    }

        
}
