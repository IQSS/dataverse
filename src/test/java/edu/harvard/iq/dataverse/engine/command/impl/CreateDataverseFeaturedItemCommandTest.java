package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateDataverseFeaturedItemCommandTest {
    @Mock
    private CommandContext contextStub;

    @Mock
    private DataverseFeaturedItemServiceBean dataverseFeaturedItemServiceStub;

    @InjectMocks
    private CreateDataverseFeaturedItemCommand sut;

    private Dataverse testDataverse;
    private NewDataverseFeaturedItemDTO testNewDataverseFeaturedItemDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testDataverse = new Dataverse();
        testDataverse.setId(123L);

        testNewDataverseFeaturedItemDTO = new NewDataverseFeaturedItemDTO();
        testNewDataverseFeaturedItemDTO.setImageFileName("test.png");
        testNewDataverseFeaturedItemDTO.setContent("test content");
        testNewDataverseFeaturedItemDTO.setDisplayOrder(0);
        testNewDataverseFeaturedItemDTO.setDvObject(null, null);
        testNewDataverseFeaturedItemDTO.setImageFileInputStream(mock(InputStream.class));

        when(contextStub.dataverseFeaturedItems()).thenReturn(dataverseFeaturedItemServiceStub);
        sut = new CreateDataverseFeaturedItemCommand(makeRequest(), testDataverse, testNewDataverseFeaturedItemDTO);
    }

    @Test
    void execute_imageFileProvidedAndValid_savesFeaturedItem() throws Exception {
        DataverseFeaturedItem expectedFeaturedItem = new DataverseFeaturedItem();
        expectedFeaturedItem.setDataverse(testDataverse);
        expectedFeaturedItem.setType(testNewDataverseFeaturedItemDTO.getType());
        expectedFeaturedItem.setDvObject(testNewDataverseFeaturedItemDTO.getDvObject());
        expectedFeaturedItem.setImageFileName(testNewDataverseFeaturedItemDTO.getImageFileName());
        expectedFeaturedItem.setDisplayOrder(testNewDataverseFeaturedItemDTO.getDisplayOrder());
        expectedFeaturedItem.setContent(testNewDataverseFeaturedItemDTO.getContent());

        when(dataverseFeaturedItemServiceStub.save(any(DataverseFeaturedItem.class))).thenReturn(expectedFeaturedItem);

        DataverseFeaturedItem result = sut.execute(contextStub);

        assertNotNull(result);

        assertEquals(testNewDataverseFeaturedItemDTO.getImageFileName(), result.getImageFileName());
        assertEquals(testNewDataverseFeaturedItemDTO.getDisplayOrder(), result.getDisplayOrder());
        assertEquals(testNewDataverseFeaturedItemDTO.getContent(), result.getContent());
        assertEquals(testDataverse, result.getDataverse());
        assertEquals(testNewDataverseFeaturedItemDTO.getDvObject(), result.getDvObject());
        assertEquals(testNewDataverseFeaturedItemDTO.getType(), result.getType());

        verify(dataverseFeaturedItemServiceStub).save(any(DataverseFeaturedItem.class));
        verify(dataverseFeaturedItemServiceStub).saveDataverseFeaturedItemImageFile(
                testNewDataverseFeaturedItemDTO.getImageFileInputStream(),
                testNewDataverseFeaturedItemDTO.getImageFileName(),
                testDataverse.getId()
        );
    }

    @Test
    void execute_noImageFileProvided_featuredItemSavedWithoutImage() throws Exception {
        testNewDataverseFeaturedItemDTO.setImageFileName(null);

        DataverseFeaturedItem expectedFeaturedItem = new DataverseFeaturedItem();
        when(dataverseFeaturedItemServiceStub.save(any(DataverseFeaturedItem.class))).thenReturn(expectedFeaturedItem);

        DataverseFeaturedItem result = sut.execute(contextStub);

        assertNotNull(result);
        verify(dataverseFeaturedItemServiceStub).save(any(DataverseFeaturedItem.class));
        verify(dataverseFeaturedItemServiceStub, never()).saveDataverseFeaturedItemImageFile(any(), any(), any());
    }

    @Test
    void execute_imageFileProcessingFails_throwsCommandException() throws IOException, DataverseFeaturedItemServiceBean.InvalidImageFileException {
        testNewDataverseFeaturedItemDTO.setImageFileName("invalid.png");
        InputStream inputStreamMock = mock(InputStream.class);
        testNewDataverseFeaturedItemDTO.setImageFileInputStream(inputStreamMock);

        doThrow(new IOException("File processing failed"))
                .when(dataverseFeaturedItemServiceStub)
                .saveDataverseFeaturedItemImageFile(any(InputStream.class), any(String.class), any(Long.class));

        CommandException exception = assertThrows(CommandException.class, () -> sut.execute(contextStub));
        assertTrue(exception.getMessage().contains("File processing failed"));
    }

    @Test
    void execute_invalidFileTypeProvided_throwsInvalidCommandArgumentsException() throws IOException, DataverseFeaturedItemServiceBean.InvalidImageFileException {
        testNewDataverseFeaturedItemDTO.setImageFileName("invalid.type");
        InputStream inputStreamMock = mock(InputStream.class);
        testNewDataverseFeaturedItemDTO.setImageFileInputStream(inputStreamMock);

        doThrow(new DataverseFeaturedItemServiceBean.InvalidImageFileException("Invalid file type"))
                .when(dataverseFeaturedItemServiceStub).saveDataverseFeaturedItemImageFile(any(InputStream.class), any(String.class), any(Long.class));

        InvalidCommandArgumentsException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(contextStub));
        assertTrue(exception.getMessage().contains("Invalid file type"));
    }

    @Test
    void execute_contentIsNull_throwsInvalidCommandArgumentsException() {
        assertContentShouldBeProvidedInvalidCommandArgumentsException(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", ""})
    void execute_contentIsEmpty_throwsInvalidCommandArgumentsException(String content) {
        assertContentShouldBeProvidedInvalidCommandArgumentsException(content);
    }

    @Test
    void execute_contentExceedsLimit_throwsInvalidCommandArgumentsException() {
        testNewDataverseFeaturedItemDTO.setContent(createContentExceedingMaxLength());
        InputStream inputStreamMock = mock(InputStream.class);
        testNewDataverseFeaturedItemDTO.setImageFileInputStream(inputStreamMock);

        InvalidCommandArgumentsException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(contextStub));
        assertEquals(
                MessageFormat.format(
                        BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.contentExceedsLengthLimit"),
                        List.of(DataverseFeaturedItem.MAX_FEATURED_ITEM_CONTENT_SIZE)
                ),
                exception.getMessage()
        );
    }

    private void assertContentShouldBeProvidedInvalidCommandArgumentsException(String content) {
        testNewDataverseFeaturedItemDTO.setContent(content);
        InputStream inputStreamMock = mock(InputStream.class);
        testNewDataverseFeaturedItemDTO.setImageFileInputStream(inputStreamMock);

        InvalidCommandArgumentsException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(contextStub));
        assertEquals(
                BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.contentShouldBeProvided"),
                exception.getMessage()
        );
    }

    private String createContentExceedingMaxLength() {
        return "a".repeat(Math.max(0, DataverseFeaturedItem.MAX_FEATURED_ITEM_CONTENT_SIZE + 1));
    }
}
