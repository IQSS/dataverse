package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HtmlMarkupInputFieldRendererFactoryTest {

    private HtmlMarkupInputFieldRendererFactory inputFieldRendererFactory = new HtmlMarkupInputFieldRendererFactory();
    
    @Test
    public void createRenderer__withEmptyOptions() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        HtmlMarkupInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertFalse(renderer.renderInTwoColumns());
    }
}
