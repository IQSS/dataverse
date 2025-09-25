/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsValidator;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author stephenkraffmiller
 */
public class UpdateDatasetTermsOfUseCommandTest {
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
    
}
