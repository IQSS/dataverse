package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsValidator;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateDatasetFieldsCommandTest {

    @Mock
    private UpdateDatasetVersionCommand updateDatasetVersionCommandStub;
    @Mock
    private DataverseEngine dataverseEngineMock;
    @Mock
    private Dataset datasetMock;
    @Mock
    private DataverseRequest dataverseRequestStub;
    @Mock
    private TermsOfUseAndAccess termsOfUseAndAccessStub;
    @Mock
    private CommandContext commandContextMock;
    @Mock
    private DatasetVersion datasetVersionMock;
    @Mock
    private DatasetFieldsValidator datasetFieldsValidatorMock;

    private UpdateDatasetFieldsCommand sut;

    @BeforeEach
    public void setUp() throws CommandException {
        MockitoAnnotations.openMocks(this);

        when(dataverseEngineMock.submit(updateDatasetVersionCommandStub)).thenReturn(datasetMock);
        when(commandContextMock.engine()).thenReturn(dataverseEngineMock);
        when(commandContextMock.datasetFieldsValidator()).thenReturn(datasetFieldsValidatorMock);
        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccessStub);
    }

    @Test
    public void execute_invalidFields_shouldThrowException() {
        List<DatasetField> testUpdatedFields = new ArrayList<>();
        sut = new UpdateDatasetFieldsCommand(datasetMock, testUpdatedFields, true, dataverseRequestStub);

        when(datasetFieldsValidatorMock.validateFields(testUpdatedFields, datasetVersionMock)).thenReturn("validation error");

        CommandException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(commandContextMock));

        assertEquals(
                BundleUtil.getStringFromBundle("updateDatasetFieldsCommand.api.processDatasetUpdate.parseError", List.of("validation error")),
                exception.getMessage()
        );
    }

    @Test
    public void execute_withValidFields_updatesFields() throws CommandException {
        List<DatasetField> testUpdatedFields = new ArrayList<>();
        sut = new UpdateDatasetFieldsCommand(datasetMock, testUpdatedFields, true, dataverseRequestStub, updateDatasetVersionCommandStub);

        when(datasetFieldsValidatorMock.validateFields(testUpdatedFields, datasetVersionMock)).thenReturn("");
        when(datasetVersionMock.getDatasetFields()).thenReturn(new ArrayList<>());

        Dataset result = sut.execute(commandContextMock);

        verify(commandContextMock).engine();
        assertNotNull(result);
    }
}
