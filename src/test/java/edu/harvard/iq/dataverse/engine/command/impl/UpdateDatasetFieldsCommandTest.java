package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsValidator;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateDatasetFieldsCommandTest {

    @Mock
    private Dataset datasetMock;
    @Mock
    private List<DatasetField> updatedFields;
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
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(commandContextMock.datasetFieldsValidator()).thenReturn(datasetFieldsValidatorMock);
        sut = new UpdateDatasetFieldsCommand(datasetMock, updatedFields, true, dataverseRequestStub);
    }

    @Test
    public void execute_invalidFields_shouldThrowException() {
        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccessStub);
        when(datasetFieldsValidatorMock.validateFields(updatedFields, datasetVersionMock)).thenReturn("validation error");

        CommandException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(commandContextMock));

        assertEquals(
                BundleUtil.getStringFromBundle("updateDatasetFieldsCommand.api.processDatasetUpdate.parseError", List.of("validation error")),
                exception.getMessage()
        );
    }
}
