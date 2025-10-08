package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertSame;
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
    private CommandContext ctxt;

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
    private TermsOfUseAndAccess termsOfUseAndAccessMock;

    @Mock
    private DatasetVersion datasetVersionMock;

    private Dataset dataset = new Dataset();
    private TermsOfUseAndAccess terms = new TermsOfUseAndAccess();

    private UpdateDatasetTermsOfAccessCommand command;

    @BeforeEach
    public void setUp() throws CommandException {
        MockitoAnnotations.openMocks(this);
        when(dataverseEngineMock.submit(updateDatasetVersionCommand)).thenReturn(datasetMock);
        when(commandContextMock.engine()).thenReturn(dataverseEngineMock);
        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccessMock);
        dataset = new Dataset();
        dataset.getOrCreateEditVersion().setTermsOfUseAndAccess(new TermsOfUseAndAccess());
        terms = new TermsOfUseAndAccess();

        command = new UpdateDatasetTermsOfAccessCommand(dataset, terms, request, updateDatasetVersionCommand);
    }

    @Test
    public void testExecute_SubmitsUpdateCommandAndUpdatesTerms() throws Exception {
        when(ctxt.engine()).thenReturn(dataverseEngineMock);
        Dataset expectedDataset = new Dataset();
        when(dataverseEngineMock.submit(updateDatasetVersionCommand)).thenReturn(expectedDataset);
        when(datasetMock.getOrCreateEditVersion()).thenReturn(datasetVersionMock);
        when(datasetVersionMock.getTermsOfUseAndAccess()).thenReturn(termsOfUseAndAccessMock);

        Dataset result = command.execute(ctxt);

        DatasetVersion version = dataset.getOrCreateEditVersion();
        assertEquals(terms, version.getTermsOfUseAndAccess());
        assertEquals(DatasetVersion.VersionState.DRAFT, version.getVersionState());

        verify(dataverseEngineMock).submit(updateDatasetVersionCommand);
        assertSame(expectedDataset, result);
    }

}
