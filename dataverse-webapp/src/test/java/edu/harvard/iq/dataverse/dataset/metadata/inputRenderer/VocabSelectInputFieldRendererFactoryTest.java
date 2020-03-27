package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VocabSelectInputFieldRendererFactoryTest {

    private VocabSelectInputFieldRendererFactory inputFieldRendererFactory = new VocabSelectInputFieldRendererFactory();
    
    
    // -------------------- TESTS --------------------
    
    @Test
    public void createRenderer__withEmptyOptions() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertTrue(renderer.renderInTwoColumns());
    }
}
