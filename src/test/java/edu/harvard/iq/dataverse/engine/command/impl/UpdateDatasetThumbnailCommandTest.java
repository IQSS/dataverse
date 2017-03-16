package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateDatasetThumbnailCommandTest {

    private TestDataverseEngine testEngine;
    private Dataset dataset;

    public UpdateDatasetThumbnailCommandTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        dataset = new Dataset();
        testEngine = new TestDataverseEngine(new TestCommandContext() {

            @Override
            public DataFileServiceBean files() {
                return new DataFileServiceBean() {

                    @Override
                    public DataFile find(Object alias) {
                        return new DataFile();
                    }

                };
            }

            @Override
            public SystemConfig systemConfig() {
                return new SystemConfig() {

                    @Override
                    public String getDataverseSiteUrl() {
                        return "https://dataverse.example.edu";
                    }

                };

            }

        }
        );
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDatasetNull() {
        dataset = null;
        String expected = "Can't update dataset thumbnail. Dataset is null.";
        String actual = null;
        DatasetThumbnail datasetThumbnail = null;
        try {
            datasetThumbnail = testEngine.submit(new UpdateDatasetThumbnailCommand(null, dataset, UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, Long.MIN_VALUE, null));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(datasetThumbnail);
    }

}
