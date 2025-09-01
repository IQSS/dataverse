package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.template.TemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        // Arrange
        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        // Act
        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        // Assert
        assertEquals(1, result.size());
        assertEquals(testTemplate, result.get(0));
    }

    @Test
    public void execute_shouldReturnTemplates_parentHasTemplates() throws CommandException {
        // Arrange
        Template parentTemplate = new Template();
        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Dataverse parentDataverseMock = Mockito.mock(Dataverse.class);
        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));
        Mockito.when(dataverseMock.getParentTemplates()).thenReturn(Collections.singletonList(parentTemplate));
        Mockito.when(dataverseMock.getOwner()).thenReturn(parentDataverseMock);

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        // Act
        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(testTemplate));
        assertTrue(result.contains(parentTemplate));
    }

    @Test
    public void execute_shouldReturnTemplates_parentHasNoTemplates() throws CommandException {
        // Arrange
        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Dataverse parentDataverseStub = Mockito.mock(Dataverse.class);
        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));
        Mockito.when(dataverseMock.getOwner()).thenReturn(parentDataverseStub);
        Mockito.when(dataverseMock.getParentTemplates()).thenReturn(Collections.emptyList());

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        // Act
        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(testTemplate));
    }


    @Test
    public void execute_shouldSetDefaultTemplate_whenDefaultIsPresent() throws CommandException {
        // Arrange
        String defaultTplName = "defaultTplName";
        String otherTplName = "otherTplName";

        Template defaultTpl = TemplateBuilder.aTemplate().withName(defaultTplName).isDefault(true).build();
        Template otherTpl = TemplateBuilder.aTemplate().withName(otherTplName).isDefault(false).build();

        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Mockito.when(dataverseMock.getTemplates()).thenReturn(Arrays.asList(defaultTpl, otherTpl));
        Mockito.when(dataverseMock.getDefaultTemplate()).thenReturn(defaultTpl);

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        // Act
        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        // Assert
        assertEquals(2, result.size());

        Template resultDefault = findTemplateByName(result, defaultTplName);
        assertNotNull(resultDefault, "Default template should be in the list");
        assertTrue(resultDefault.isIsDefaultForDataverse(),
                "The template with name " + defaultTplName + " should be marked as default");

        Template resultOther = findTemplateByName(result, otherTplName);
        assertNotNull(resultOther, "Other template should be in the list");
        assertFalse(resultOther.isIsDefaultForDataverse(), "The template with name " + otherTplName + " should NOT be marked as default");
    }

    @Test
    public void execute_shouldNotSetDefault_whenNoDefaultTemplateExists() throws CommandException {
        // Arrange
        testTemplate = TemplateBuilder.aTemplate().isDefault(false).build();
        Dataverse dataverseMock = Mockito.mock(Dataverse.class);
        Mockito.when(dataverseMock.getTemplates()).thenReturn(Collections.singletonList(testTemplate));
        Mockito.when(dataverseMock.getDefaultTemplate()).thenReturn(null);

        ListDataverseTemplatesCommand sut = new ListDataverseTemplatesCommand(dataverseRequestStub, dataverseMock);

        // Act
        List<Template> result = sut.execute(Mockito.mock(CommandContext.class));

        // Assert
        assertEquals(1, result.size());
        assertFalse(result.get(0).isIsDefaultForDataverse(), "No template should be marked as default");
    }

    private Template findTemplateByName(List<Template> templates, String name) {
        return templates.stream()
                .filter(t -> name.equals(t.getName()))
                .findFirst()
                .orElse(null);
    }
}
