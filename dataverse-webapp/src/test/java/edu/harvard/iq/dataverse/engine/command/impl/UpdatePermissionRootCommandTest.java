package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.mocks.MockRequestFactory;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author michael
 */
public class UpdatePermissionRootCommandTest {

    private DataverseDao mockBean;
    TestCommandContext testCommandContext;
    boolean serviceBeanCalled;

    @Before
    public void setUp() {
        mockBean = new DataverseDao() {
            @Override
            public Dataverse save(Dataverse dv) {
                serviceBeanCalled = true;
                return dv;
            }
        };
        testCommandContext = new TestCommandContext() {
            @Override
            public DataverseDao dataverses() {
                return mockBean;
            }
        };
        serviceBeanCalled = false;
    }

    @Test
    public void testNoChange()  {
        Dataverse dv = MocksFactory.makeDataverse();
        DataverseEngine ngn = new TestDataverseEngine(testCommandContext);
        dv.setPermissionRoot(false);

        UpdatePermissionRootCommand sut = new UpdatePermissionRootCommand(false, MockRequestFactory.makeRequest(), dv);
        Dataverse result = ngn.submit(sut);

        assertFalse(result.isPermissionRoot());
        assertFalse(serviceBeanCalled);

        dv.setPermissionRoot(true);

        sut = new UpdatePermissionRootCommand(true, MockRequestFactory.makeRequest(), dv);
        result = ngn.submit(sut);

        assertTrue(result.isPermissionRoot());
        assertFalse(serviceBeanCalled);
    }

    @Test
    public void testChange()  {
        Dataverse dv = MocksFactory.makeDataverse();
        DataverseEngine ngn = new TestDataverseEngine(testCommandContext);
        dv.setPermissionRoot(false);

        UpdatePermissionRootCommand sut = new UpdatePermissionRootCommand(true, MockRequestFactory.makeRequest(), dv);
        Dataverse result = ngn.submit(sut);

        assertTrue(result.isPermissionRoot());
        assertTrue(serviceBeanCalled);

        dv.setPermissionRoot(true);

        sut = new UpdatePermissionRootCommand(false, MockRequestFactory.makeRequest(), dv);
        result = ngn.submit(sut);

        assertFalse(result.isPermissionRoot());
        assertTrue(serviceBeanCalled);
    }


}
