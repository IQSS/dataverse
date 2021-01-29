package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand.UserIntent;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateDatasetThumbnailCommandTest {

    private TestDataverseEngine testEngine;
    
    @Mock
    private DataFileServiceBean dataFileService;
    @Mock
    private DatasetDao datasetDao;
    @Mock
    private DatasetThumbnailService datasetThumbnailService;
    
    private Dataset dataset = new Dataset();
    private Long unfindableFile = 1l;
    private Long thumbnailUnexpectedlyAbsent = 2l;


    @BeforeEach
    public void setUp() {
        
        when(dataFileService.find(thumbnailUnexpectedlyAbsent)).thenReturn(new DataFile());
        when(datasetDao.setDatasetFileAsThumbnail(any(), any())).thenAnswer((invocation) -> invocation.getArgument(0));
        
        testEngine = new TestDataverseEngine(new TestCommandContext() {

            @Override
            public DataFileServiceBean files() {
                return dataFileService;
            }

            @Override
            public DatasetDao datasets() {
                return datasetDao;
            }
            
            @Override
            public DatasetThumbnailService datasetThumailService() {
                return datasetThumbnailService;
            }
        }
        );
    }

    // -------------------- TESTS --------------------

    @Test
    public void testDatasetNull() {
        // given
        dataset = null;

        // when
        Executable updateThumbnailOperation = () -> testEngine.submit(
                new UpdateDatasetThumbnailCommand(null, dataset, UserIntent.setDatasetFileAsThumbnail, Long.MIN_VALUE, null));
        
        // then
        CommandException exception = assertThrows(CommandException.class, updateThumbnailOperation);

        assertEquals("Can't update dataset thumbnail. Dataset is null.", exception.getMessage());
    }

    @Test
    public void testIntentNull() {

        // when
        Executable updateThumbnailOperation = () -> testEngine.submit(
                new UpdateDatasetThumbnailCommand(null, dataset, null, Long.MIN_VALUE, null));
        
        // then
        CommandException exception = assertThrows(CommandException.class, updateThumbnailOperation);
        assertEquals("No changes to save.", exception.getMessage());
        
    }

    @Test
    public void testSetDatasetFileAsThumbnailFileNull() {

        // when
        Executable updateThumbnailOperation = () -> testEngine.submit(
                new UpdateDatasetThumbnailCommand(null, dataset, UserIntent.setDatasetFileAsThumbnail, null, null));
        
        // then
        CommandException exception = assertThrows(CommandException.class, updateThumbnailOperation);
        assertEquals("A file was not selected to be the new dataset thumbnail.", exception.getMessage());
    }

    @Test
    public void testSetDatasetFileAsThumbnailFileNotFound() {

        // when
        Executable updateThumbnailOperation = () -> testEngine.submit(
                new UpdateDatasetThumbnailCommand(null, dataset, UserIntent.setDatasetFileAsThumbnail, unfindableFile, null));
        
        // then
        CommandException exception = assertThrows(CommandException.class, updateThumbnailOperation);
        assertEquals("Could not find file based on id supplied: 1.", exception.getMessage());
    }

    @Test
    public void testSetDatasetFileAsThumbnailFileThumbnailUnexpectedlyAbsent() {

        // when
        Executable updateThumbnailOperation = () -> testEngine.submit(
                new UpdateDatasetThumbnailCommand(null, dataset, UserIntent.setDatasetFileAsThumbnail, thumbnailUnexpectedlyAbsent, null));
        
        // then
        CommandException exception = assertThrows(CommandException.class, updateThumbnailOperation);
        assertEquals("Dataset thumbnail is unexpectedly absent.", exception.getMessage());
    }

}
