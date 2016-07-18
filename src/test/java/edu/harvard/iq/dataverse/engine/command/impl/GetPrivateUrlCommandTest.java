package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetPrivateUrlCommandTest {

    private TestDataverseEngine testEngine;
    Dataset dataset;

    public GetPrivateUrlCommandTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        testEngine = new TestDataverseEngine(new TestCommandContext() {

            @Override
            public PrivateUrlServiceBean privateUrl() {
                return new PrivateUrlServiceBean() {

                    @Override
                    public PrivateUrl getPrivateUrlFromDatasetId(long datasetId) {
                        return null;
                    }

                };
            }

        });
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDatasetWithoutAnId() throws Exception {
        dataset = new Dataset();
        PrivateUrl privateUrl = testEngine.submit(new GetPrivateUrlCommand(null, dataset));
        assertNull(privateUrl);
    }

    @Test
    public void testDatasetWithAnId() throws Exception {
        dataset = new Dataset();
        dataset.setId(42l);
        PrivateUrl privateUrl = testEngine.submit(new GetPrivateUrlCommand(null, dataset));
        assertNull(privateUrl);
    }

}
