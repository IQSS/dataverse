package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class DatasetTest {

    /**
     * Test of isLockedFor method, of class Dataset.
     */
    @Test
    public void testIsLockedFor() {
        Dataset sut = new Dataset();
        assertFalse( sut.isLockedFor(DatasetLock.Reason.Ingest) );
        DatasetLock dl = new DatasetLock(DatasetLock.Reason.Ingest, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        sut.addLock(dl);
        assertTrue( sut.isLockedFor(DatasetLock.Reason.Ingest) );
        assertFalse( sut.isLockedFor(DatasetLock.Reason.Workflow) );
    }
    
    @Test
    public void testLocksManagement() {
        Dataset sut = new Dataset();
        assertFalse( sut.isLocked() );
        
        DatasetLock dlIngest = new DatasetLock(DatasetLock.Reason.Ingest, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        dlIngest.setId(MocksFactory.nextId());
        sut.addLock(dlIngest);
        assertTrue( sut.isLocked() );

        final DatasetLock dlInReview = new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        dlInReview.setId(MocksFactory.nextId());
        sut.addLock(dlInReview);
        assertEquals( 2, sut.getLocks().size() );
        
        DatasetLock retrievedDl = sut.getLockFor(DatasetLock.Reason.Ingest);
        assertEquals( dlIngest, retrievedDl );
        sut.removeLock(dlIngest);
        assertNull( sut.getLockFor(DatasetLock.Reason.Ingest) );
        
        assertTrue( sut.isLocked() );
        
        sut.removeLock(dlInReview);
        assertFalse( sut.isLocked() );
        
    }
 
}
