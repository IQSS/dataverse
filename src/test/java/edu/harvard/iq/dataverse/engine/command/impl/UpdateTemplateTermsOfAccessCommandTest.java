package edu.harvard.iq.dataverse.engine.command.impl;

/**
 *
 * @author stephenkraffmiller
 */
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.TermsOfAccess;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;

import edu.harvard.iq.dataverse.TemplateServiceBean;
import edu.harvard.iq.dataverse.TermsOfAccess;
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
    private TermsOfAccess targetTermsOfAccess;
    private TermsOfAccess sourceTermsOfAccess;

    @BeforeEach
    void setUp() {
        targetTermsOfAccess = new TermsOfAccess();
        sourceTermsOfAccess = new TermsOfAccess();

        // Setup the context to return our mocked service
        lenient().when(ctxt.templates()).thenReturn(templateService);
        // Setup the template to return our "target" terms object
        lenient().when(template.getTermsOfAccess()).thenReturn(targetTermsOfAccess);
    }

    @Test
    void testExecute_Success() throws Exception {
        // Arrange: Populate the source with data
        sourceTermsOfAccess.setFileAccessRequest(true);
        sourceTermsOfAccess.setTermsOfAccess("Special restricted access only.");
        sourceTermsOfAccess.setDataAccessPlace("Secure Data Lab");
        
        when(templateService.save(template)).thenReturn(template);
        
        cmd = new UpdateTemplateTermsOfAccessCommand(request, template, dataverse, sourceTermsOfAccess);

        // Act
        Template result = cmd.execute(ctxt);

        // Assert: Verify the target object was updated with source values
        assertTrue(targetTermsOfAccess.isFileAccessRequest());
        assertEquals("Special restricted access only.", targetTermsOfAccess.getTermsOfAccess());
        assertEquals("Secure Data Lab", targetTermsOfAccess.getDataAccessPlace());
        
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