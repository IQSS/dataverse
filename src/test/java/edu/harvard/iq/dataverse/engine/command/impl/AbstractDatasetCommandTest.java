package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author michael
 */
public class AbstractDatasetCommandTest {
    
   
    @Test
    void testNullDataset() {
        DataverseRequest request = makeRequest();
        assertThrows(IllegalArgumentException.class, () -> new AbstractDatasetCommandImpl(request, null));
    }
    
    @Test
    void testNullDatasetNonNullParent() {
        DataverseRequest request = makeRequest();
        Dataverse dataverse = makeDataverse();
        assertThrows(IllegalArgumentException.class,
            () -> new AbstractDatasetCommandImpl(request, null, dataverse));
    }
    
    /**
     * Test of getDataset method, of class AbstractDatasetCommand.
     */
    @Test
    public void testGetDataset() {
        Dataset ds = MocksFactory.makeDataset();
        AbstractDatasetCommand instance = new AbstractDatasetCommandImpl(makeRequest(), ds);
        assertEquals( ds, instance.getDataset() );
    }

    /**
     * Test of getTimestamp method, of class AbstractDatasetCommand.
     */
    @Test
    public void testGetTimestamp() {
        Dataset ds = MocksFactory.makeDataset();
        AbstractDatasetCommand instance = new AbstractDatasetCommandImpl(makeRequest(), ds);
        long now = System.currentTimeMillis();
        assertTrue( Math.abs(now-instance.getTimestamp().getTime()) < 20 ); // 20 milliseconds is equal enough.
    }

    public class AbstractDatasetCommandImpl extends AbstractDatasetCommand {

        public AbstractDatasetCommandImpl(DataverseRequest aRequest, Dataset aDataset, Dataverse parent) {
            super(aRequest, aDataset, parent);
        }

        public AbstractDatasetCommandImpl(DataverseRequest aRequest, Dataset aDataset) {
            super(aRequest, aDataset);
        }

        @Override
        public Object execute(CommandContext ctxt) throws CommandException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }


    }
    
}
