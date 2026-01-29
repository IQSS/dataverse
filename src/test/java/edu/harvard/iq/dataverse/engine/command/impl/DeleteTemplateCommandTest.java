package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import jakarta.persistence.EntityManager;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DeleteTemplateCommandTest {

    @Mock
    private CommandContext ctxt;

    @Mock
    private EntityManager em;

    @Mock
    private Dataverse editedDv;

    @Mock
    private Dataverse otherDv;

    @Mock
    private Template doomed;
    
    @Mock
    private DataverseRequest dataverseRequest;



    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(ctxt.em()).thenReturn(em);

        List<Dataverse> dvWDefaultTemplate = new ArrayList<>();
        DeleteTemplateCommand sut = new DeleteTemplateCommand(dataverseRequest, editedDv,doomed, dvWDefaultTemplate);
        // Simulate that dvWDefaultTemplate has one dataverse with a template

    }

    @Test
    void testExecute_MergesAndRemovesCorrectly() throws Exception {
        // Arrange
        Dataverse mergedDv = mock(Dataverse.class);
        Template mergedTemplate = mock(Template.class);
        List<Dataverse> dvWDefaultTemplate = new ArrayList<>();
        dvWDefaultTemplate.add(otherDv);

        when(em.merge(editedDv)).thenReturn(mergedDv);
        when(em.merge(doomed)).thenReturn(mergedTemplate);
        DeleteTemplateCommand sut = new DeleteTemplateCommand(dataverseRequest, editedDv, doomed, dvWDefaultTemplate);
 
        // Act
        Dataverse result = sut.execute(ctxt);

        // Assert
        verify(em).merge(editedDv);          // should merge edited dataverse
        verify(em).merge(otherDv);           // should merge dvWDefaultTemplate items after clearing default template
        verify(otherDv).setDefaultTemplate(null);
        verify(em).merge(doomed);            // should merge doomed template
        verify(em).remove(mergedTemplate);   // should remove merged template
        assertEquals(mergedDv, result);      // return value is the merged dataverse
    }

    @Test
    void testExecute_WhenEditedDvIsNull() throws Exception {
        // Arrange
        Dataverse nullDv = null;
        List<Dataverse> dvWDefaultTemplate = new ArrayList<>();
        DeleteTemplateCommand sut = new DeleteTemplateCommand(dataverseRequest, nullDv, doomed, dvWDefaultTemplate);
 
        Template mergedTemplate = mock(Template.class);
        when(em.merge(doomed)).thenReturn(mergedTemplate);

        // Act
        Dataverse result = sut.execute(ctxt);

        // Assert
        verify(em, never()).merge(any(Dataverse.class));
        verify(em).merge(doomed);
        verify(em).remove(mergedTemplate);
        assertNull(result);
    }
}

