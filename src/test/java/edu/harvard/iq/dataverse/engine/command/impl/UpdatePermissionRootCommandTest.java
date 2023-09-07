package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author michael
 */
public class UpdatePermissionRootCommandTest {
    
    private DataverseServiceBean mockBean;
    TestCommandContext testCommandContext;
    boolean serviceBeanCalled;
    
    @BeforeEach
    public void setUp() {
        mockBean = new DataverseServiceBean() {
            @Override
            public Dataverse save( Dataverse dv ) {
                serviceBeanCalled = true;
                return dv;
            }
            @Override
            public boolean index(Dataverse dataverse, boolean indexPermisions) {
                    // no-op. The superclass accesses databases which we don't have.
                    return true;
            }            
        };
        testCommandContext = new TestCommandContext() {
            @Override
            public DataverseServiceBean dataverses() {
                return mockBean;
            }
        };
        serviceBeanCalled = false;
    }
    
    @Test
    public void testNoChange() throws CommandException {
        Dataverse dv = MocksFactory.makeDataverse();
        DataverseEngine ngn = new TestDataverseEngine(testCommandContext);
        dv.setPermissionRoot( false );
        
        UpdatePermissionRootCommand sut = new UpdatePermissionRootCommand(false, MocksFactory.makeRequest(), dv);
        Dataverse result = ngn.submit(sut);
        
        assertFalse( result.isPermissionRoot() );
        assertFalse( serviceBeanCalled );
        
        dv.setPermissionRoot( true );
        
        sut = new UpdatePermissionRootCommand( true, MocksFactory.makeRequest(), dv );
        result = ngn.submit(sut);
        
        assertTrue( result.isPermissionRoot() );
        assertFalse( serviceBeanCalled );
    }
    
    @Test
    public void testChange() throws CommandException {
        Dataverse dv = MocksFactory.makeDataverse();
        DataverseEngine ngn = new TestDataverseEngine(testCommandContext);
        dv.setPermissionRoot( false );
        
        UpdatePermissionRootCommand sut = new UpdatePermissionRootCommand(true, MocksFactory.makeRequest(), dv);
        Dataverse result = ngn.submit(sut);
        
        assertTrue(result.isPermissionRoot() );
        assertTrue(serviceBeanCalled );
        
        dv.setPermissionRoot( true );
        
        sut = new UpdatePermissionRootCommand( false, MocksFactory.makeRequest(), dv );
        result = ngn.submit(sut);
        
        assertFalse( result.isPermissionRoot() );
        assertTrue(serviceBeanCalled );
    }
    
    
}
