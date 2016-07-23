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
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author michael
 */
public class CreateDatasetVersionCommandTest {
    
    public CreateDatasetVersionCommandTest() {
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

    @Test
    public void testSimpleVersionAddition() throws Exception {
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        // Create Dataset
        Dataset ds = makeDataset();
        
        // Populate the Initial version
        DatasetVersion dsvInitial = ds.getEditVersion();
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
        assertEquals( dsvNew, ds.getEditVersion() );
        Map<DvObject, Set<Permission>> expected = new HashMap<>();
        expected.put(ds, Collections.singleton(Permission.AddDataset));
        assertEquals(expected, testEngine.getReqiredPermissionsForObjects() );
    }
    
    @Test(expected=IllegalCommandException.class)
    public void testCantCreateTwoDraftVersions() throws Exception {
        DatasetVersion dsvNew = new DatasetVersion();
        dsvNew.setVersionState(DatasetVersion.VersionState.DRAFT);
        
        // Execute
        CreateDatasetVersionCommand sut = new CreateDatasetVersionCommand( makeRequest(), makeDataset(), dsvNew );
        
        TestDataverseEngine testEngine = new TestDataverseEngine( new TestCommandContext() );
        
        testEngine.submit(sut);
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
