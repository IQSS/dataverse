package edu.harvard.iq.dataverse.engine.command.impl;

/**
 *
 * @author stephenkraffmiller
 */
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;

import edu.harvard.iq.dataverse.TemplateServiceBean;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateTemplateTermsOfAccessCommandTest {

    private UpdateTemplateTermsOfAccessCommand cmd;

    @Mock private CommandContext ctxt;
    @Mock private TemplateServiceBean templateService;
    @Mock private DataverseRequest request;
    @Mock private Dataverse dataverse;
    @Mock private Template template;

    // We'll use real POJOs for the terms to verify the data transfer logic
    private TermsOfUseAndAccess targetTerms;
    private TermsOfUseAndAccess sourceTerms;

    @BeforeEach
    void setUp() {
        targetTerms = new TermsOfUseAndAccess();
        sourceTerms = new TermsOfUseAndAccess();
        
        // Setup the context to return our mocked service
        lenient().when(ctxt.templates()).thenReturn(templateService);
        // Setup the template to return our "target" terms object
        lenient().when(template.getTermsOfUseAndAccess()).thenReturn(targetTerms);
    }

    @Test
    void testExecute_Success() throws Exception {
        // Arrange: Populate the source with data
        sourceTerms.setFileAccessRequest(true);
        sourceTerms.setTermsOfAccess("Special restricted access only.");
        sourceTerms.setDataAccessPlace("Secure Data Lab");
        
        when(templateService.save(template)).thenReturn(template);
        
        cmd = new UpdateTemplateTermsOfAccessCommand(request, template, dataverse, sourceTerms);

        // Act
        Template result = cmd.execute(ctxt);

        // Assert: Verify the target object was updated with source values
        assertTrue(targetTerms.isFileAccessRequest());
        assertEquals("Special restricted access only.", targetTerms.getTermsOfAccess());
        assertEquals("Secure Data Lab", targetTerms.getDataAccessPlace());
        
        // Verify interaction
        verify(templateService).save(template);
        assertNotNull(result);
    }

    @Test
    void testExecute_NullTermsThrowsException() {
        // Arrange
        cmd = new UpdateTemplateTermsOfAccessCommand(request, template, dataverse, null);

        // Act & Assert
        assertThrows(InvalidCommandArgumentsException.class, () -> cmd.execute(ctxt));
    }
}