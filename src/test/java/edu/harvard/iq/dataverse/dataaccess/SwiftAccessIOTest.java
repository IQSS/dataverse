/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author oscardssmith
 */
public class SwiftAccessIOTest {

    private SwiftAccessIO<Dataset> datasetAccess;
    private Dataset dataset;

    public SwiftAccessIOTest() {
    }

    @Before
    public void setUpClass() throws IOException {
        dataset = MocksFactory.makeDataset();
        datasetAccess = new SwiftAccessIO<>(dataset);
    }

    /**
     * Test of canRead, canWrite, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testPerms() throws IOException {
        assertEquals(false, datasetAccess.canRead());
        assertEquals(false, datasetAccess.canWrite());
    }
}
