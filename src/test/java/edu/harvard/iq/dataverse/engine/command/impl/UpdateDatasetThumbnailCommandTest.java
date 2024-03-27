package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateDatasetThumbnailCommandTest {

    private TestDataverseEngine testEngine;
    private Dataset dataset;
    private Long unfindableFile = 1l;
    private Long thumbnailUnexpectedlyAbsent = 2l;

    public UpdateDatasetThumbnailCommandTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        dataset = new Dataset();
        testEngine = new TestDataverseEngine(new TestCommandContext() {

            @Override
            public DataFileServiceBean files() {
                return new DataFileServiceBean() {

                    @Override
                    public DataFile find(Object object) {
                        if (object == unfindableFile) {
                            return null;
                        } else if (object == thumbnailUnexpectedlyAbsent) {
                            return new DataFile();
                        } else {
                            return null;
                        }
                    }

                };
            }

            @Override
            public DatasetServiceBean datasets() {
                return new DatasetServiceBean() {

                    @Override
                    public Dataset setDatasetFileAsThumbnail(Dataset dataset, DataFile datasetFileThumbnailToSwitchTo) {
                        return dataset;
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

    @AfterEach
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

    @Test
    public void testIntentNull() {
        String expected = "No changes to save.";
        String actual = null;
        DatasetThumbnail datasetThumbnail = null;
        try {
            datasetThumbnail = testEngine.submit(new UpdateDatasetThumbnailCommand(null, dataset, null, Long.MIN_VALUE, null));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(datasetThumbnail);
    }

    @Test
    public void testSetDatasetFileAsThumbnailFileNull() {
        String expected = "A file was not selected to be the new dataset thumbnail.";
        String actual = null;
        DatasetThumbnail datasetThumbnail = null;
        try {
            datasetThumbnail = testEngine.submit(new UpdateDatasetThumbnailCommand(null, dataset, UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, null, null));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(datasetThumbnail);
    }

    @Test
    public void testSetDatasetFileAsThumbnailFileNotFound() {
        String expected = "Could not find file based on id supplied: 1.";
        String actual = null;
        DatasetThumbnail datasetThumbnail = null;
        try {
            datasetThumbnail = testEngine.submit(new UpdateDatasetThumbnailCommand(null, dataset, UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, unfindableFile, null));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(datasetThumbnail);
    }

    @Test
    public void testSetDatasetFileAsThumbnailFileThumbnailUnexpectedlyAbsent() {
        String expected = "Dataset thumbnail is unexpectedly absent.";
        String actual = null;
        DatasetThumbnail datasetThumbnail = null;
        try {
            datasetThumbnail = testEngine.submit(new UpdateDatasetThumbnailCommand(null, dataset, UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, thumbnailUnexpectedlyAbsent, null));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(datasetThumbnail);
    }

}
