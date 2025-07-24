package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListDataverseTemplatesCommandTest {

    private DataverseRequest dataverseRequestStub;
    private Template testTemplate;

    @BeforeEach
    public void setUp() {
        dataverseRequestStub = Mockito.mock(DataverseRequest.class);
        testTemplate = new Template();
    }

    @Test
    public void execute_shouldReturnTemplates_noParent() throws CommandException {
        Dataverse dataverseMock = Mockito.mock(Dataverse.class);

        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        assertEquals(1, result.size());
        assertEquals(testTemplate, result.get(0));
    }

    @Test
    public void execute_shouldReturnTemplates_parentHasTemplates() throws CommandException {
        Template parentTemplate = new Template();

        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Dataverse parentDataverseMock = Mockito.mock(Dataverse.class);

        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));
        Mockito.when(dataverseMock.getParentTemplates()).thenReturn(Collections.singletonList(parentTemplate));
        Mockito.when(dataverseMock.getOwner()).thenReturn(parentDataverseMock);

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        assertEquals(2, result.size());
        assertTrue(result.contains(testTemplate));
        assertTrue(result.contains(parentTemplate));
    }

    @Test
    public void execute_shouldReturnTemplates_parentHasNoTemplates() throws CommandException {
        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Dataverse parentDataverseStub = Mockito.mock(Dataverse.class);

        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));
        Mockito.when(dataverseMock.getOwner()).thenReturn(parentDataverseStub);
        Mockito.when(dataverseMock.getParentTemplates()).thenReturn(Collections.emptyList());

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        assertEquals(1, result.size());
        assertTrue(result.contains(testTemplate));
    }
}
