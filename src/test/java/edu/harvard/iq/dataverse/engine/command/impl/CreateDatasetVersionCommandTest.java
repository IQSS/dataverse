package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.*;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author michael
 */
public class CreateDatasetVersionCommandTest {
    
    @Test
    public void testSimpleVersionAddition() throws Exception {
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        // Create Dataset
        Dataset ds = makeDataset();
        
        // Populate the Initial version
        DatasetVersion dsvInitial = ds.getOrCreateEditVersion();
        dsvInitial.setCreateTime( dateFmt.parse("20001012") );
        dsvInitial.setLastUpdateTime( dsvInitial.getLastUpdateTime() );
        dsvInitial.setId( MocksFactory.nextId() );
        dsvInitial.setReleaseTime( dateFmt.parse("20010101") );
        dsvInitial.setVersionState(DatasetVersion.VersionState.RELEASED);
        dsvInitial.setMinorVersionNumber(0l);
        dsvInitial.setVersionNumber(1l);
        
        // Create version to be added
        DatasetVersion dsvNew = new DatasetVersion();
        dsvNew.setVersionState(DatasetVersion.VersionState.DRAFT);
        
        // Execute
        CreateDatasetVersionCommand sut = new CreateDatasetVersionCommand( makeRequest(), ds, dsvNew );
        
        final MockDatasetServiceBean serviceBean = new MockDatasetServiceBean();
        TestDataverseEngine testEngine = new TestDataverseEngine( new TestCommandContext(){
            @Override public DatasetServiceBean datasets() { return serviceBean; }
        } );
        
        testEngine.submit(sut);
        
        // asserts
        assertTrue( serviceBean.storeVersionCalled );
        Date dsvCreationDate = dsvNew.getCreateTime();
        assertEquals( dsvCreationDate, dsvNew.getLastUpdateTime() );
        assertEquals( dsvCreationDate.getTime(), ds.getModificationTime().getTime() );
        assertEquals( ds, dsvNew.getDataset() );
        assertEquals( dsvNew, ds.getOrCreateEditVersion() );
        Map<DvObject, Set<Permission>> expected = new HashMap<>();
        expected.put(ds, Collections.singleton(Permission.AddDataset));
        assertEquals(expected, testEngine.getReqiredPermissionsForObjects() );
    }
    
    @Test
    void testCantCreateTwoDraftVersions() {
        DatasetVersion dsvNew = new DatasetVersion();
        dsvNew.setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset sampleDataset = makeDataset();
        sampleDataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        
        // Execute
        CreateDatasetVersionCommand sut = new CreateDatasetVersionCommand( makeRequest(), sampleDataset, dsvNew );
        
        TestDataverseEngine testEngine = new TestDataverseEngine( new TestCommandContext() {
            DatasetServiceBean dsb = new MockDatasetServiceBean();
            @Override
            public DatasetServiceBean datasets() {
                return dsb;
            }
            
        });
        
        assertThrows(IllegalCommandException.class, () -> testEngine.submit(sut));
    }
    
    
    static class MockDatasetServiceBean extends DatasetServiceBean {
         
        boolean storeVersionCalled = false;
        
        @Override
        public DatasetVersion storeVersion(DatasetVersion dsv) {
            storeVersionCalled = true;
            dsv.setId( nextId() );
            return dsv;
        }
        
    }
    
}
