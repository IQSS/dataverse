package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataverse;
import static edu.harvard.iq.dataverse.mocks.MockRequestFactory.makeRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author michael
 */
public class AbstractDatasetCommandTest {


    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testNullDataset() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractDatasetCommandImpl(makeRequest(), null));
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testNullDatasetNonNullParent() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractDatasetCommandImpl(makeRequest(), null, makeDataverse()));
    }

    /**
     * Test of getDataset method, of class AbstractDatasetCommand.
     */
    @Test
    public void testGetDataset() {
        Dataset ds = MocksFactory.makeDataset();
        AbstractDatasetCommand instance = new AbstractDatasetCommandImpl(makeRequest(), ds);
        assertEquals(ds, instance.getDataset());
    }

    /**
     * Test of getTimestamp method, of class AbstractDatasetCommand.
     */
    @Test
    public void testGetTimestamp() {
        Dataset ds = MocksFactory.makeDataset();
        AbstractDatasetCommand instance = new AbstractDatasetCommandImpl(makeRequest(), ds);
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(now - instance.getTimestamp().getTime()) < 20); // 20 milliseconds is equal enough.
    }

    public class AbstractDatasetCommandImpl extends AbstractDatasetCommand {

        public AbstractDatasetCommandImpl(DataverseRequest aRequest, Dataset aDataset, Dataverse parent) {
            super(aRequest, aDataset, parent);
        }

        public AbstractDatasetCommandImpl(DataverseRequest aRequest, Dataset aDataset) {
            super(aRequest, aDataset);
        }

        @Override
        public Object execute(CommandContext ctxt)  {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }


    }

}
