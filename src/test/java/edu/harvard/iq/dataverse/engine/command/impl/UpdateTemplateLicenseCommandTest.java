package edu.harvard.iq.dataverse.engine.command.impl;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.TemplateServiceBean;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.license.License;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateTemplateLicenseCommandTest {

    private UpdateTemplateLicenseCommand cmd;

    @Mock private CommandContext ctxt;
    @Mock private TemplateServiceBean templateService;
    @Mock private DataverseRequest request;
    @Mock private Dataverse dataverse;
    @Mock private Template template;
    
    // We use a real object for terms to verify field mapping easily
    private TermsOfUseAndAccess existingTerms;

    @BeforeEach
    void setUp() {
        existingTerms = new TermsOfUseAndAccess();
        lenient().when(ctxt.templates()).thenReturn(templateService);
        lenient().when(template.getTermsOfUseAndAccess()).thenReturn(existingTerms);
    }

    @Test
    void testExecute_SuccessWithLicense() throws Exception {
        // Arrange
        License activeLicense = mock(License.class);
        when(activeLicense.isActive()).thenReturn(true);
        when(templateService.save(template)).thenReturn(template);
        
        cmd = new UpdateTemplateLicenseCommand(request, template, dataverse, activeLicense);

        // Act
        Template result = cmd.execute(ctxt);

        // Assert
        assertEquals(activeLicense, existingTerms.getLicense());
        verify(templateService).save(template);
        assertNotNull(result);
    }

    @Test
    void testExecute_InactiveLicenseThrowsException() {
        // Arrange
        License inactiveLicense = mock(License.class);
        when(inactiveLicense.isActive()).thenReturn(false);
        when(inactiveLicense.getName()).thenReturn("CC BY-NC");
        
        cmd = new UpdateTemplateLicenseCommand(request, template, dataverse, inactiveLicense);

        // Act & Assert
        assertThrows(InvalidCommandArgumentsException.class, () -> cmd.execute(ctxt));
    }

    @Test
    void testExecute_SuccessWithCustomTerms() throws Exception {
        // Arrange
        TermsOfUseAndAccess customTerms = new TermsOfUseAndAccess();
        customTerms.setTermsOfUse("My Custom Rules");
        customTerms.setConfidentialityDeclaration("Top Secret");
        
        when(templateService.save(template)).thenReturn(template);
        
        cmd = new UpdateTemplateLicenseCommand(request, template, dataverse, customTerms);

        // Act
        cmd.execute(ctxt);

        // Assert
        assertEquals("My Custom Rules", existingTerms.getTermsOfUse());
        assertEquals("Top Secret", existingTerms.getConfidentialityDeclaration());
        verify(templateService).save(template);
    }

    @Test
    void testExecute_BlankCustomTermsThrowsException() {
        // Arrange
        TermsOfUseAndAccess blankTerms = new TermsOfUseAndAccess();
        blankTerms.setTermsOfUse("   "); // Blank string
        
        cmd = new UpdateTemplateLicenseCommand(request, template, dataverse, blankTerms);

        // Act & Assert
        assertThrows(InvalidCommandArgumentsException.class, () -> cmd.execute(ctxt));
    }
}
