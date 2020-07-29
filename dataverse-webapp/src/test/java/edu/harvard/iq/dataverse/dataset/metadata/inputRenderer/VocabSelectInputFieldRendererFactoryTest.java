package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VocabSelectInputFieldRendererFactoryTest {

    private VocabSelectInputFieldRendererFactory inputFieldRendererFactory = new VocabSelectInputFieldRendererFactory();
    
    private DatasetFieldType fieldType = new DatasetFieldType();
    
    
    // -------------------- TESTS --------------------
    
    @Test
    public void createRenderer__withEmptyOptions() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertTrue(renderer.renderInTwoColumns());
        assertFalse(renderer.isSortByLocalisedStringsOrder());
    }
    
    @Test
    public void createRenderer__withRenderInTwoColumnsOption() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{'renderInTwoColumns':false}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertFalse(renderer.renderInTwoColumns());
    }
    
    @Test
    public void createRenderer__withSortByLocalisedStringsOrderOption() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{'sortByLocalisedStringsOrder':true}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertTrue(renderer.isSortByLocalisedStringsOrder());
    }
}
