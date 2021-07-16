package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.SuggestionInputFieldRendererFactory.SuggestionDisplayType;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.enterprise.inject.Instance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SuggestionInputFieldRendererFactoryTest {

    @InjectMocks
    private SuggestionInputFieldRendererFactory suggestionInputFieldRendererFactory;
    
    @Mock
    private Instance<SuggestionHandler> suggestionHandlers;
    @Mock
    private SuggestionHandler suggestionHandler1;
    @Mock
    private SuggestionHandler suggestionHandler2;

    private DatasetFieldType datasetFieldType;
    
    @BeforeEach
    void beforeEach() {
        when(suggestionHandler1.getName()).thenReturn("handler1");
        when(suggestionHandler2.getName()).thenReturn("handler2");
        when(suggestionHandler2.getAllowedFilters()).thenReturn(Lists.newArrayList("filterName1", "filterName2"));
        when(suggestionHandlers.iterator()).thenReturn(
                Lists.newArrayList(suggestionHandler1, suggestionHandler2).iterator());
        suggestionInputFieldRendererFactory.postConstruct();
        
        MetadataBlock block = new MetadataBlock();
        block.setName("custommetadata");
        datasetFieldType = new DatasetFieldType("fieldWithSuggestions", FieldType.TEXT, false);
        datasetFieldType.setMetadataBlock(block);
    }
    
    // -------------------- TESTS --------------------

    @Test
    public void createRenderer() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'suggestionSourceClass':'handler1', 'suggestionDisplayType': 'TWO_COLUMNS'}").getAsJsonObject();

        // when
        SuggestionInputFieldRenderer renderer = suggestionInputFieldRendererFactory
                .createRenderer(datasetFieldType, rendererOptions);

        // then
        Assert.assertEquals(InputRendererType.SUGGESTION_TEXT, renderer.getType());
        Assert.assertFalse(renderer.isHidden());
        Assert.assertTrue(renderer.renderInTwoColumns());
        Assert.assertEquals(SuggestionDisplayType.TWO_COLUMNS, renderer.getSuggestionDisplayType());
        Assert.assertEquals("Suggestion Value Header", renderer.getValueHeaderText());
        Assert.assertEquals("Suggestion Details Header", renderer.getDetailsHeaderText());
    }

    @Test
    public void createRenderer__withDefaultSuggestionDisplayTypes() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'suggestionSourceClass':'handler1'}").getAsJsonObject();

        // when
        SuggestionInputFieldRenderer renderer = suggestionInputFieldRendererFactory
                .createRenderer(datasetFieldType, rendererOptions);

        // then
        Assert.assertEquals(InputRendererType.SUGGESTION_TEXT, renderer.getType());
        Assert.assertFalse(renderer.isHidden());
        Assert.assertTrue(renderer.renderInTwoColumns());
        Assert.assertEquals(SuggestionDisplayType.SIMPLE, renderer.getSuggestionDisplayType());
    }

    @Test
    public void createRenderer__withEmptyOptions() {
        //given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();

        //when
        Executable createRendererOperation = () -> suggestionInputFieldRendererFactory
                .createRenderer(datasetFieldType, rendererOptions);

        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }

    @Test
    public void createRenderer__withInvalidOptionFields() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'suggestionSourceClass':['notexisting']}").getAsJsonObject();

        // when
        Executable createRendererOperation = () -> suggestionInputFieldRendererFactory
                .createRenderer(datasetFieldType, rendererOptions);

        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }

    @Test
    public void createRenderer__withInvalidSuggestionHandlerName() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'suggestionSourceClass':'handler99'}").getAsJsonObject();

        // when
        Executable createRendererOperation = () -> suggestionInputFieldRendererFactory
                .createRenderer(datasetFieldType, rendererOptions);

        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }

    @Test
    public void createRenderer__withNotSupportedFilterName() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(
                "{'suggestionSourceClass':'handler2', 'suggestionFilteredBy': ['fieldWithSuggestions:filterName1', 'fieldWithSuggestions:filterNameNotExisting']}")
                .getAsJsonObject();

        // when
        Executable createRendererOperation = () -> suggestionInputFieldRendererFactory
                .createRenderer(datasetFieldType, rendererOptions);

        // then
        assertThrows(InputRendererInvalidConfigException.class, createRendererOperation);
    }
}