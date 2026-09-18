package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfAccess;
import edu.harvard.iq.dataverse.TermsOfUseOrLicense;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author stephenkraffmiller
 */
public class UpdateDatasetTermsOfAccessCommandTest {


    @Mock
    private DataverseEngine dataverseEngineMock;

    @Mock
    private UpdateDatasetVersionCommand updateDatasetVersionCommand;

    @Mock
    private Dataset datasetMock;

    @Mock
    private DataverseRequest request;

    @Mock
    private CommandContext commandContextMock;
    
    @Mock
    private TermsOfAccess termsOfAccessMock;

    @Mock
    private DatasetVersion datasetVersionMock;

    private Dataset dataset = new Dataset();
    private TermsOfAccess termsOA = new TermsOfAccess();
    private TermsOfUseOrLicense termsOUAL = new TermsOfUseOrLicense();

    private UpdateDatasetTermsOfAccessCommand command;

    @BeforeEach
    public void setUp() throws CommandException {
        
        MockitoAnnotations.openMocks(this);
        when(dataverseEngineMock.submit(updateDatasetVersionCommand)).thenReturn(datasetMock);
        when(commandContextMock.engine()).thenReturn(dataverseEngineMock);
        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfAccess()).thenReturn(termsOfAccessMock);
        when(datasetVersionMock.getTermsOfUseOrLicense()).thenReturn(termsOUAL.copyTermsOfUseOrLicense());
        dataset = new Dataset();
        dataset.getOrCreateEditVersion().setTermsOfAccess(new TermsOfAccess());
        dataset.getOrCreateEditVersion().setTermsOfUseOrLicense(new TermsOfUseOrLicense());
        termsOA = new TermsOfAccess();
        termsOUAL = new TermsOfUseOrLicense();

        command = new UpdateDatasetTermsOfAccessCommand(datasetMock, termsOA, request, updateDatasetVersionCommand);
    }
    
    @Test
    public void execute_shouldUpdateRequestAndSetVersionStateToDraft() throws CommandException {
        // Arrange
        UpdateDatasetTermsOfAccessCommand sut = new UpdateDatasetTermsOfAccessCommand(datasetMock, termsOA, request, updateDatasetVersionCommand);

        // Act
        sut.execute(commandContextMock);

        // Assert
        assertEquals(termsOA, datasetVersionMock.getTermsOfAccess());
        assertEquals(termsOUAL, datasetVersionMock.getTermsOfUseOrLicense());
        verify(dataverseEngineMock).submit(updateDatasetVersionCommand);
        verify(datasetVersionMock).setVersionState(DatasetVersion.VersionState.DRAFT);
        verify(commandContextMock).engine();
    }


}
