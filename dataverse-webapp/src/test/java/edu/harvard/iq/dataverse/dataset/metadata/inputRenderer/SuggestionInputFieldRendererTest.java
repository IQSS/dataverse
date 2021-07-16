package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.SuggestionInputFieldRendererFactory.SuggestionDisplayType;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SuggestionInputFieldRendererTest {

    private SuggestionInputFieldRenderer suggestionInputFieldRenderer;

    @Mock
    private SuggestionHandler suggestionHandler;

    // -------------------- TESTS --------------------

    @Test
    public void createSuggestions_WithoutFilters() {
        //given
        suggestionInputFieldRenderer = new SuggestionInputFieldRenderer(suggestionHandler,
                new HashMap<>(),
                SuggestionDisplayType.SIMPLE,
                "fieldTypeName", "blockName");
        DatasetField datasetField = new DatasetField();
        String testQuery = "testQuery";
        ArrayList<Suggestion> suggestionsFromHandler = Lists.newArrayList(
                new Suggestion("suggestion1", "detail1"),
                new Suggestion("suggestion2", "detail2"));

        //when
        when(suggestionHandler.generateSuggestions(any(), any())).thenReturn(suggestionsFromHandler);
        List<Suggestion> suggestions = suggestionInputFieldRenderer.createSuggestions(datasetField, testQuery);

        //then
        Assert.assertEquals(suggestionsFromHandler, suggestions);
        verify(suggestionHandler).generateSuggestions(new HashMap<>(), testQuery);
    }

    @Test
    public void createSuggestions_WithFilters() {
        //given
        String dsftName = "grantNumber";
        String suggestionFilterName = "grantNumberSuggestionFilter";
        suggestionInputFieldRenderer = new SuggestionInputFieldRenderer(suggestionHandler,
                ImmutableMap.of(dsftName, suggestionFilterName),
                SuggestionDisplayType.SIMPLE,
                "fieldTypeName", "blockName");

        DatasetField datasetField = generateDsfFamily(dsftName, "testValue");
        ArrayList<Suggestion> suggestionsFromHandler = Lists.newArrayList(
                new Suggestion("suggestion1", "detail1"),
                new Suggestion("suggestion2", "detail2"));
        String testQuery = "testQuery";

        //when
        when(suggestionHandler.generateSuggestions(any(), any())).thenReturn(suggestionsFromHandler);
        List<Suggestion> suggestions = suggestionInputFieldRenderer.createSuggestions(datasetField, testQuery);

        //then
        Assert.assertEquals(suggestionsFromHandler, suggestions);
        verify(suggestionHandler).generateSuggestions(
                ImmutableMap.of(suggestionFilterName, "testValue"), testQuery);
    }

    // -------------------- PRIVATE --------------------

    public DatasetField generateDsfFamily(String dsftName, String fieldValue) {
        DatasetField parentField = MocksFactory.makeEmptyDatasetField(MocksFactory.makeDatasetFieldType(), 0);
        DatasetField sourceField = MocksFactory.makeEmptyDatasetField(MocksFactory.makeDatasetFieldType(), 0);
        DatasetField valueField = MocksFactory.makeEmptyDatasetField(MocksFactory.makeDatasetFieldType(dsftName,
                                                                                                       FieldType.TEXT,
                                                                                                       false,
                                                                                                       new MetadataBlock()),
                                                                     0);
        valueField.setFieldValue(fieldValue);

        sourceField.setDatasetFieldParent(parentField);
        parentField.setDatasetFieldsChildren(Lists.newArrayList(valueField, sourceField));

        return sourceField;
    }
}