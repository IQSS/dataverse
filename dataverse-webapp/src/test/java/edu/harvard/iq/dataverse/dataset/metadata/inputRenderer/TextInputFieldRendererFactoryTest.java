package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction.FieldButtonActionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.enterprise.inject.Instance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TextInputFieldRendererFactoryTest {

    @InjectMocks
    private TextInputFieldRendererFactory inputFieldRendererFactory;
    
    @Mock
    private FieldButtonActionHandler actionHandler1;
    
    @Mock
    private FieldButtonActionHandler actionHandler2;
    
    @Mock
    private Instance<FieldButtonActionHandler> actionHandlers;
    
    @BeforeEach
    public void beforeEach() {
        when(actionHandler1.getName()).thenReturn("actionHandler1");
        when(actionHandler2.getName()).thenReturn("actionHandler2");
        when(actionHandlers.iterator()).thenReturn(IteratorUtils.arrayIterator(actionHandler1, actionHandler2));
        
        inputFieldRendererFactory.postConstruct();
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    public void createRenderer__withEmptyOptions() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        TextInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertTrue(renderer.renderInTwoColumns());
        assertFalse(renderer.hasActionButton());
    }
    
    @Test
    public void createRenderer__withActionButton() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'buttonActionHandler':'actionHandler2',"
                + "'buttonActionTextKey':'dataset.AddReplication',"
                + "'actionForOperations':['CREATE_DATASET']}").getAsJsonObject();
        
        // when
        TextInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(rendererOptions);
        
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertTrue(renderer.renderInTwoColumns());
        assertTrue(renderer.hasActionButton());
        assertTrue(renderer.showActionButtonForOperation("CREATE_DATASET"));
        assertFalse(renderer.showActionButtonForOperation("EDIT_DATASET"));
        assertFalse(renderer.showActionButtonForOperation("CREATE_TEMPLATE"));
        assertFalse(renderer.showActionButtonForOperation("EDIT_TEMPLATE"));
        assertEquals("Add \"Replication Data for\" to Title", renderer.getActionButtonText());
    }
    
    @Test
    public void createRenderer__notExistingActionHandler() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'buttonActionHandler':'notexisting'}").getAsJsonObject();
        
        // when
        Executable createRendererOperation = () -> inputFieldRendererFactory.createRenderer(rendererOptions);
        
        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }
    
    @Test
    public void createRenderer__invalidOptions() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'buttonActionHandler':'actionHandler2',"
                + "'actionForOperations':'invalid'}").getAsJsonObject();
        
        // when
        Executable createRendererOperation = () -> inputFieldRendererFactory.createRenderer(rendererOptions);
        
        
        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }
    
    @Test
    public void executeButtonAction__correctActionHandler() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'buttonActionHandler':'actionHandler2',"
                + "'buttonActionTextKey':'dataset.AddReplication',"
                + "'actionForOperations':['CREATE_DATASET']}").getAsJsonObject();
        
        DatasetField datasetField = Mockito.mock(DatasetField.class);
        
        @SuppressWarnings("unchecked")
        List<DatasetFieldsByType> allBlockFields = Mockito.mock(List.class);
        
        
        // when
        TextInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(rendererOptions);
        renderer.executeButtonAction(datasetField, allBlockFields);
        
        
        // then
        verify(actionHandler1, never()).handleAction(any(), any());
        verify(actionHandler2).handleAction(datasetField, allBlockFields);
    }
}
