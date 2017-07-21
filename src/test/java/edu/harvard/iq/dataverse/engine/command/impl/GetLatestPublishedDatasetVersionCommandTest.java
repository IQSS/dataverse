package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;

/**
 *
 * @author michael
 */
public class GetLatestPublishedDatasetVersionCommandTest {
    
    TestDataverseEngine engine = new TestDataverseEngine( new TestCommandContext() );
    
    @Test
    public void testLatestPublishedNoDraft() throws CommandException {
        
        Dataset ds = MocksFactory.makeDataset();
        List<DatasetVersion> versions = make10Versions(ds);
        ds.setVersions(versions);
        
        assertEquals( 10l, engine.submit(new GetLatestPublishedDatasetVersionCommand(makeRequest(), ds)).getVersionNumber().longValue() );
        assertTrue( "Published datasets should require no permissions to view",
                        engine.getReqiredPermissionsForObjects().get(ds).isEmpty() );
    }
    
    @Test
    public void testLatestPublishedWithDraft() throws CommandException {
        
        Dataset ds = MocksFactory.makeDataset();
        List<DatasetVersion> versions = make10Versions(ds);
        versions.add( MocksFactory.makeDatasetVersion(ds.getCategories()) );
        ds.setVersions(versions);
        
        assertEquals( 10l, engine.submit(new GetLatestPublishedDatasetVersionCommand(makeRequest(), ds)).getVersionNumber().longValue() );
        assertTrue( "Published datasets should require no permissions to view",
                        engine.getReqiredPermissionsForObjects().get(ds).isEmpty() );
    }
    
    @Test
    public void testLatestNonePublished() throws CommandException {
        
        Dataset ds = MocksFactory.makeDataset();
        
        assertNull( engine.submit(new GetLatestPublishedDatasetVersionCommand(makeRequest(), ds)) );
    }

    private List<DatasetVersion> make10Versions(Dataset ds) {
        // setup: make 10 versions.
        List<DatasetVersion> versions = new ArrayList<>(10);
        for ( int i=10; i>0; i-- ) {
            DatasetVersion v = MocksFactory.makeDatasetVersion(ds.getCategories());
            v.setVersionNumber((long)i);
            v.setMinorVersionNumber(0l);
            v.setReleaseTime( MocksFactory.date(1990, i, 1) );
            v.setVersionState(DatasetVersion.VersionState.RELEASED);
            versions.add(v);
        }
        return versions;
    }
    
    
}
