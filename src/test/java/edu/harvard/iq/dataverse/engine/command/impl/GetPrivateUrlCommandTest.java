package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GetPrivateUrlCommandTest {

    private TestDataverseEngine testEngine;
    Dataset dataset;

    public GetPrivateUrlCommandTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
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

    @AfterEach
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
