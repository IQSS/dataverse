package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateDatasetLicenseCommandTest {

    @Mock
    private DataverseRequest dataverseRequestStub;
    @Mock
    private UpdateDatasetVersionCommand updateDatasetVersionCommandStub;
    @Mock
    private Dataset datasetMock;
    @Mock
    private DatasetVersion datasetVersionMock;
    @Spy
    private TermsOfUseAndAccess termsOfUseAndAccessSpy = new TermsOfUseAndAccess();
    @Mock
    private DataverseEngine dataverseEngineMock;
    @Mock
    private CommandContext commandContextMock;

    private License activeLicense;
    private License inactiveLicense;

    @BeforeEach
    public void setUp() throws CommandException {
        MockitoAnnotations.openMocks(this);

        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccessSpy);
        when(dataverseEngineMock.submit(updateDatasetVersionCommandStub)).thenReturn(datasetMock);
        when(commandContextMock.engine()).thenReturn(dataverseEngineMock);

        activeLicense = new License();
        activeLicense.setActive(true);
        activeLicense.setName("activeLicense");

        inactiveLicense = new License();
        inactiveLicense.setActive(false);
        inactiveLicense.setName("inactiveLicense");
    }

    @Test
    public void execute_shouldUpdateLicenseAndSetVersionStateToDraft() throws CommandException {
        // Arrange
        UpdateDatasetLicenseCommand sut = new UpdateDatasetLicenseCommand(dataverseRequestStub, datasetMock, activeLicense);

        // Act
        sut.execute(commandContextMock);

        // Assert
        assertEquals(activeLicense, termsOfUseAndAccessSpy.getLicense());
        verify(datasetVersionMock).setVersionState(DatasetVersion.VersionState.DRAFT);
        verify(commandContextMock).engine();
    }

    @Test
    public void execute_shouldThrowException_whenLicenseIsNotActive() {
        // Arrange
        UpdateDatasetLicenseCommand sut = new UpdateDatasetLicenseCommand(dataverseRequestStub, datasetMock, inactiveLicense);
        String expectedMessage = BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.licenseNotActive", List.of(inactiveLicense.getName()));

        // Act & Assert
        InvalidCommandArgumentsException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(commandContextMock));
        assertEquals(expectedMessage, exception.getMessage());
    }
}
