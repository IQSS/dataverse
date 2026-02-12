/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.TemplateServiceBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author stephenkraffmiller
 */
@ExtendWith(MockitoExtension.class)
public class UpdateTemplateFieldsCommandTest {

    @Mock
    private CommandContext ctxt;
    @Mock
    private TemplateServiceBean templateService;
    @Mock
    private DataverseRequest request;
    @Mock
    private Dataverse dataverse;

    private Template template;
    private List<DatasetField> updatedFields;
    private Map<String, String> instructions;

    @BeforeEach
    void setUp() {
        template = new Template();
        template.setDatasetFields(new ArrayList<>());
        updatedFields = new ArrayList<>();
        instructions = new HashMap<>();

        lenient().when(ctxt.templates()).thenReturn(templateService);
        lenient().when(templateService.save(any(Template.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testExecute_AddNewField() throws Exception {
        // Arrange: Updated fields contains a field not in the template
        DatasetField newField = createPrimitiveField("title", "New Template Title");
        updatedFields.add(newField);

        UpdateTemplateFieldsCommand cmd = new UpdateTemplateFieldsCommand(template, dataverse, updatedFields, instructions, false, request);

        // Act
        Template result = cmd.execute(ctxt);

        // Assert
        assertEquals(1, result.getDatasetFields().size());
        assertEquals("New Template Title", result.getDatasetFields().get(0).getValue());
    }

    @Test
    void testExecute_ReplaceExistingData() throws Exception {
        // Arrange: Template has an existing field
        DatasetField existingField = createPrimitiveField("description", "Old Description");
 
        template.getDatasetFields().add(existingField);
        existingField.setTemplate(template);

        // New field with same type but different value
        DatasetField newField = createPrimitiveField("description", "New Description");
        updatedFields.add(newField);

        // replaceData = true
        UpdateTemplateFieldsCommand cmd = new UpdateTemplateFieldsCommand(template, dataverse, updatedFields, instructions, true, request);

        // Act
        cmd.execute(ctxt);

        // Assert
        assertEquals(1, template.getDatasetFields().size());
        assertEquals("New Description", template.getDatasetFields().get(0).getValue());
    }

    @Test
    void testUpdateInstructions_MergeLogic() throws Exception {
        // Arrange
        template.setInstructionsMap(new HashMap<>(Map.of("field1", "old instruction")));
        instructions.put("field2", "new instruction");

        // replaceData = false (Merge)
        UpdateTemplateFieldsCommand cmd = new UpdateTemplateFieldsCommand(template, dataverse, updatedFields, instructions, false, request);

        // Act
        cmd.execute(ctxt);

        // Assert
        Map<String, String> resultInstructions = template.getInstructionsMap();
        assertEquals(2, resultInstructions.size());
        assertTrue(resultInstructions.containsKey("field1"));
        assertTrue(resultInstructions.containsKey("field2"));
    }

    @Test

    void testExecute_CompoundField_Logic() throws Exception {
        // 1. Setup the Type
        DatasetFieldType compoundType = new DatasetFieldType();
        compoundType.setName("author");
        // Since we can't setCompound, we add a child type to make isCompound() return true
        DatasetFieldType childType = new DatasetFieldType();
        childType.setName("authorName");
        compoundType.setChildDatasetFieldTypes(List.of(childType));

        // 2. Setup existing field in Template
        DatasetField existingField = new DatasetField();
        existingField.setDatasetFieldType(compoundType);

        // Create an existing compound value
        DatasetFieldCompoundValue existingValue = new DatasetFieldCompoundValue();
        existingField.setDatasetFieldCompoundValues(new ArrayList<>(List.of(existingValue)));

        template.getDatasetFields().add(existingField);

        // 3. Setup the Update Command input
        DatasetField updatedField = new DatasetField();
        updatedField.setDatasetFieldType(compoundType);

        DatasetFieldCompoundValue newValue = new DatasetFieldCompoundValue();
        updatedField.setDatasetFieldCompoundValues(List.of(newValue));
        updatedFields.add(updatedField);

        // We need to spy or mock the display values because 
        // real display value calculation requires deep child-field nesting
        DatasetField spyExisting = spy(existingField);
        DatasetField spyUpdated = spy(updatedField);

        // Scenario: They are different, so newValue should be added
        doReturn("Author 1").when(spyExisting).getCompoundDisplayValue();
        doReturn("Author 2").when(spyUpdated).getCompoundDisplayValue();

        // Re-inject the spy into the template fields list
        template.getDatasetFields().clear();
        template.getDatasetFields().add(spyExisting);

        UpdateTemplateFieldsCommand cmd = new UpdateTemplateFieldsCommand(
                template, dataverse, List.of(spyUpdated), instructions, false, request
        );

        // Act
        cmd.execute(ctxt);

        // Assert
        assertEquals(2, spyExisting.getDatasetFieldCompoundValues().size(),
                "Should have added the new compound value since display values differed");
    }

    // Helper to build DatasetFields for testing
    private DatasetField createPrimitiveField(String typeName, String value) {
        DatasetField f = new DatasetField();
        DatasetFieldType type = new DatasetFieldType();
        type.setChildDatasetFieldTypes(new ArrayList<>());
        type.setName(typeName);
        f.setDatasetFieldType(type);
        f.setSingleValue(value);
        return f;
    }
}
