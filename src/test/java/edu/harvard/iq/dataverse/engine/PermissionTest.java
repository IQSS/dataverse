/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class PermissionTest {

    /**
     * Test of appliesTo method, of class Permission.
     */
    @Test
    public void testAppliesTo() {
        assertTrue( Permission.Access.appliesTo(DvObject.class) );
        assertTrue( Permission.Access.appliesTo(Dataverse.class) );
        assertTrue( Permission.Access.appliesTo(DataFile.class) );
        
        assertTrue( Permission.RestrictFile.appliesTo(DataFile.class) );
        assertFalse( Permission.RestrictFile.appliesTo(DvObject.class) );
        assertFalse( Permission.RestrictFile.appliesTo(Dataverse.class) );
    }
    
}
