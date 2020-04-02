package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HiddenVocabInputFieldRendererFactoryTest {

    @InjectMocks
    private HiddenVocabInputFieldRendererFactory inputFieldRendererFactory;
    
    // -------------------- TESTS --------------------
    
    @Test
    public void createRenderer__withEmptyOptions() {
        // given
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setControlledVocabularyValues(Lists.newArrayList());
        
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        HiddenVocabInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertEquals(0, renderer.getDefaultVocabValues().size());
    }
    
    @Test
    public void createRenderer__withDefaultValues() {
        // given
        DatasetFieldType fieldType = new DatasetFieldType();
        ControlledVocabularyValue vocabValue1 = new ControlledVocabularyValue(1L, "val1", null);
        ControlledVocabularyValue vocabValue2 = new ControlledVocabularyValue(2L, "val2", null);
        ControlledVocabularyValue vocabValue3 = new ControlledVocabularyValue(2L, "val3", null);
        fieldType.setControlledVocabularyValues(Lists.newArrayList(vocabValue1, vocabValue2, vocabValue3));
        
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{'defaultValues': ['val1', 'val2']}").getAsJsonObject();
        
        // when
        HiddenVocabInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertThat(renderer.getDefaultVocabValues(), contains(vocabValue1, vocabValue2));
    }
    
    @Test
    public void createRenderer__invalidOptions() {
        // given
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setControlledVocabularyValues(Lists.newArrayList());
        
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'defaultValues':'notAnArray'}").getAsJsonObject();
        
        // when
        Executable createRendererOperation = () -> inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        
        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }
}
