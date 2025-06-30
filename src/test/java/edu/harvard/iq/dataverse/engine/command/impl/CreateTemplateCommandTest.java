package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
public class CreateTemplateCommandTest {

    private DataverseRequest dataverseRequestStub;

    @Mock
    private CommandContext contextMock;
    @Mock
    private TemplateServiceBean templateServiceBeanStub;
    @Mock
    private MetadataBlockServiceBean metadataBlockServiceBeanMock;
    @Mock
    private LicenseServiceBean licenseServiceBeanMock;
    @Mock
    private DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevelServiceBeanMock;

    @Spy
    private Template templateSpy;

    @Mock
    private Dataverse dataverseMock;

    @BeforeEach
    public void setUp() {
        dataverseRequestStub = Mockito.mock(DataverseRequest.class);
        when(contextMock.templates()).thenReturn(templateServiceBeanStub);
    }

    @Test
    public void execute_shouldSaveTemplate_noInitialization() throws CommandException {
        // Create the command with initialization set to false

        CreateTemplateCommand sut = new CreateTemplateCommand(templateSpy, dataverseRequestStub, dataverseMock, false);

        // Act

        sut.execute(contextMock);

        // Assert

        // Verify that only save was called, and no initialization methods were touched.
        verify(templateServiceBeanStub).save(templateSpy);
        verify(templateSpy, never()).setDataverse(any(Dataverse.class));
        verify(templateSpy, never()).setMetadataValueBlocks(any());
        verify(templateSpy, never()).setTermsOfUseAndAccess(any());
    }

    @Test
    @JvmSetting(key = JvmSettings.MDB_SYSTEM_KEY_FOR, value = "something", varArgs = "citation")
    public void execute_shouldInitializeAndSaveTemplate_withInitialization() throws CommandException {
        when(contextMock.metadataBlocks()).thenReturn(metadataBlockServiceBeanMock);
        when(contextMock.licenses()).thenReturn(licenseServiceBeanMock);
        when(contextMock.fieldTypeInputLevels()).thenReturn(fieldTypeInputLevelServiceBeanMock);

        when(dataverseMock.getId()).thenReturn(42L);
        when(dataverseMock.isMetadataBlockRoot()).thenReturn(true);

        // Mock system metadata blocks

        MetadataBlock citationBlock = new MetadataBlock();
        citationBlock.setName("citation");
        when(metadataBlockServiceBeanMock.listMetadataBlocks()).thenReturn(List.of(citationBlock));

        // Mock license

        License defaultLicense = new License();
        defaultLicense.setName("CC0");
        when(licenseServiceBeanMock.getDefault()).thenReturn(defaultLicense);

        // Mock DatasetFields for input level testing

        DatasetFieldType fieldTypeMock = Mockito.mock(DatasetFieldType.class);
        Mockito.when(fieldTypeMock.getId()).thenReturn(101L);
        DatasetField dsf = Mockito.mock(DatasetField.class);
        Mockito.when(dsf.getDatasetFieldType()).thenReturn(fieldTypeMock);
        when(templateSpy.getFlatDatasetFields()).thenReturn(Collections.singletonList(dsf));

        // Create the command with initialization set to true

        CreateTemplateCommand sut = new CreateTemplateCommand(templateSpy, dataverseRequestStub, dataverseMock, true);

        // Act

        sut.execute(contextMock);

        // Assert

        // 1. Verify dataverse was set on the template
        verify(templateSpy).setDataverse(dataverseMock);

        // 2. Verify system metadata blocks were set
        ArgumentCaptor<List<MetadataBlock>> mdbCaptor = ArgumentCaptor.forClass(List.class);
        verify(templateSpy).setMetadataValueBlocks(mdbCaptor.capture());
        assertEquals(1, mdbCaptor.getValue().size());
        assertEquals("citation", mdbCaptor.getValue().get(0).getName());

        // 3. Verify TermsOfUseAndAccess were created and set correctly
        ArgumentCaptor<TermsOfUseAndAccess> termsCaptor = ArgumentCaptor.forClass(TermsOfUseAndAccess.class);
        verify(templateSpy).setTermsOfUseAndAccess(termsCaptor.capture());
        TermsOfUseAndAccess capturedTerms = termsCaptor.getValue();
        assertTrue(capturedTerms.isFileAccessRequest());
        assertEquals("CC0", capturedTerms.getLicense().getName());

        // 4. Verify DatasetFieldInputLevels were checked
        verify(fieldTypeInputLevelServiceBeanMock).findByDataverseIdDatasetFieldTypeId(42L, 101L);

        // 5. Verify fields were tidied up
        DatasetFieldUtil.tidyUpFields(templateSpy.getDatasetFields(), false);

        // 6. Finally, verify the fully initialized template was saved
        verify(templateServiceBeanStub).save(templateSpy);
    }
}
