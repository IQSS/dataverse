package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseOrLicense;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    private TermsOfUseOrLicense termsOfUseOrLicenseSpy = new TermsOfUseOrLicense();
    @Mock
    private TermsOfUseOrLicense customTermsOfUseOrLicenseMock;
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
        when(datasetVersionMock.getTermsOfUseOrLicense()).thenReturn(termsOfUseOrLicenseSpy);
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
        assertEquals(activeLicense, termsOfUseOrLicenseSpy.getLicense());
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

    @Test
    public void execute_shouldUpdateCustomTermsAndSetVersionStateToDraft() throws CommandException {
        // Arrange
        String termsOfUse = "custom terms";
        String confidentialityDeclaration = "confidentiality";
        String specialPermissions = "special permissions";
        String restrictions = "restrictions";
        String citationRequirements = "citation";
        String depositorRequirements = "depositor";
        String conditions = "conditions";
        String disclaimer = "disclaimer";

        when(customTermsOfUseOrLicenseMock.getTermsOfUse()).thenReturn(termsOfUse);
        when(customTermsOfUseOrLicenseMock.getConfidentialityDeclaration()).thenReturn(confidentialityDeclaration);
        when(customTermsOfUseOrLicenseMock.getSpecialPermissions()).thenReturn(specialPermissions);
        when(customTermsOfUseOrLicenseMock.getRestrictions()).thenReturn(restrictions);
        when(customTermsOfUseOrLicenseMock.getCitationRequirements()).thenReturn(citationRequirements);
        when(customTermsOfUseOrLicenseMock.getDepositorRequirements()).thenReturn(depositorRequirements);
        when(customTermsOfUseOrLicenseMock.getConditions()).thenReturn(conditions);
        when(customTermsOfUseOrLicenseMock.getDisclaimer()).thenReturn(disclaimer);

        termsOfUseOrLicenseSpy.setLicense(activeLicense);
        UpdateDatasetLicenseCommand sut = new UpdateDatasetLicenseCommand(dataverseRequestStub, datasetMock, customTermsOfUseOrLicenseMock);

        // Act
        sut.execute(commandContextMock);

        // Assert
        verify(datasetVersionMock).setVersionState(DatasetVersion.VersionState.DRAFT);

        assertEquals(termsOfUse, termsOfUseOrLicenseSpy.getTermsOfUse());
        assertEquals(confidentialityDeclaration, termsOfUseOrLicenseSpy.getConfidentialityDeclaration());
        assertEquals(specialPermissions, termsOfUseOrLicenseSpy.getSpecialPermissions());
        assertEquals(restrictions, termsOfUseOrLicenseSpy.getRestrictions());
        assertEquals(citationRequirements, termsOfUseOrLicenseSpy.getCitationRequirements());
        assertEquals(depositorRequirements, termsOfUseOrLicenseSpy.getDepositorRequirements());
        assertEquals(conditions, termsOfUseOrLicenseSpy.getConditions());
        assertEquals(disclaimer, termsOfUseOrLicenseSpy.getDisclaimer());
        assertEquals(null, termsOfUseOrLicenseSpy.getLicense());

        verify(datasetVersionMock).setTermsOfUseOrLicense(termsOfUseOrLicenseSpy);
        verify(commandContextMock).engine();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    public void execute_shouldThrowException_whenCustomTermsOfUseAreNullOrBlank(String invalidTerms) {
        // Arrange
        when(customTermsOfUseOrLicenseMock.getTermsOfUse()).thenReturn(invalidTerms);
        UpdateDatasetLicenseCommand sut = new UpdateDatasetLicenseCommand(dataverseRequestStub, datasetMock, customTermsOfUseOrLicenseMock);
        String expectedMessage = BundleUtil.getStringFromBundle("updateDatasetLicenseCommand.errors.customTermsOfUseNotProvided");

        // Act & Assert
        InvalidCommandArgumentsException exception = assertThrows(InvalidCommandArgumentsException.class, () -> sut.execute(commandContextMock));
        assertEquals(expectedMessage, exception.getMessage());
    }
}
