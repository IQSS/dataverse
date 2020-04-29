package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;


import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SuggestionInputFieldRendererFactoryTest {

    @InjectMocks
    private SuggestionInputFieldRendererFactory suggestionInputFieldRendererFactory;

    // -------------------- TESTS --------------------


    @Test
    public void createRenderer__withEmptyOptions() {
        //given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();

        //when
        SuggestionInputFieldRenderer renderer = suggestionInputFieldRendererFactory.createRenderer(new DatasetFieldType(),
                                                                                                   rendererOptions);

        //then
        Assert.assertEquals(InputRendererType.SUGGESTION_TEXT, renderer.getType());
        Assert.assertFalse(renderer.isHidden());
        Assert.assertTrue(renderer.renderInTwoColumns());
    }

    @Test
    public void createRenderer__withInvalidFields() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'suggestionSourceClass':['notexisting']}").getAsJsonObject();

        // when
        Executable createRendererOperation = () -> suggestionInputFieldRendererFactory.createRenderer(new DatasetFieldType(), rendererOptions);

        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }
}