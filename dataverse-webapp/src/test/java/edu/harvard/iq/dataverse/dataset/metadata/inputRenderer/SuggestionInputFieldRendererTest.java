package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.GrantSuggestionHandler;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SuggestionInputFieldRendererTest {

    private SuggestionInputFieldRenderer suggestionInputFieldRenderer;
    private GrantSuggestionHandler suggestionHandler;

    @BeforeEach
    public void setup() {
        suggestionHandler = Mockito.mock(GrantSuggestionHandler.class);
    }

    // -------------------- TESTS --------------------

    @Test
    public void createSuggestions_WithoutFilters() {
        //given
        suggestionInputFieldRenderer = new SuggestionInputFieldRenderer(suggestionHandler, new ArrayList<>(), "");
        DatasetField datasetField = new DatasetField();
        String testQuery = "testQuery";
        ArrayList<String> suggestionsFromDB = Lists.newArrayList("suggestion");

        //when
        Mockito.when(suggestionHandler.generateSuggestions(Mockito.any(),
                                                           Mockito.any(),
                                                           Mockito.eq(testQuery))).thenReturn(suggestionsFromDB);
        List<String> suggestions = suggestionInputFieldRenderer.createSuggestions(datasetField, testQuery);

        //then
        Assert.assertEquals(suggestionsFromDB, suggestions);
    }

    @Test
    public void createSuggestions_WithFilters() {
        //given
        String dsftName = "grantNumber";
        suggestionInputFieldRenderer = new SuggestionInputFieldRenderer(suggestionHandler,
                                                                        Lists.newArrayList(dsftName),
                                                                        "");
        DatasetField datasetField = generateDsfFamily(dsftName, "testValue");
        ArrayList<String> suggestionsFromDB = Lists.newArrayList(datasetField.getValue());
        String testQuery = "testQuery";

        //when
        Mockito.when(suggestionHandler.generateSuggestions(Mockito.any(),
                                                           Mockito.any(),
                                                           Mockito.eq(testQuery))).thenReturn(suggestionsFromDB);
        List<String> suggestions = suggestionInputFieldRenderer.createSuggestions(datasetField, testQuery);

        //then
        Assert.assertEquals(suggestionsFromDB, suggestions);
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