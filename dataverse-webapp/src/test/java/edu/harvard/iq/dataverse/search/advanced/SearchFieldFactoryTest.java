package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SelectOneSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SearchFieldFactoryTest {

    private SearchFieldFactory factory = new SearchFieldFactory();

    static Stream<Arguments> create() {
        return Stream.of(
                Arguments.of(createGeobox(), GeoboxCoordSearchField.class),
                Arguments.of(createType(FieldType.TEXT), TextSearchField.class),
                Arguments.of(createType(FieldType.TEXTBOX), TextSearchField.class),
                Arguments.of(createType(FieldType.URL), TextSearchField.class),
                Arguments.of(createType(FieldType.EMAIL), TextSearchField.class),
                Arguments.of(createType(FieldType.INT), NumberSearchField.class),
                Arguments.of(createType(FieldType.FLOAT), NumberSearchField.class),
                Arguments.of(createVocabularyType(true), CheckboxSearchField.class),
                Arguments.of(createVocabularyType(false), SelectOneSearchField.class));
    }

    @ParameterizedTest
    @MethodSource
    void create(DatasetFieldType fieldTypeSupplier, Class<? extends SearchField> expectedType) {
        // given & when
        SearchField field = factory.create(fieldTypeSupplier);

        // then
        assertThat(field).isInstanceOf(expectedType);
    }

    // -------------------- PRIVATE --------------------

    private static DatasetFieldType createGeobox() {
        DatasetFieldType parentType = createType(FieldType.GEOBOX);
        DatasetFieldType type = createType(FieldType.TEXT);
        type.setParentDatasetFieldType(parentType);
        return type;
    }

    private static DatasetFieldType createType(FieldType type) {
        DatasetFieldType datasetFieldType = new DatasetFieldType() {
            @Override
            public String getDisplayName() { return ""; }
        };
        datasetFieldType.setControlledVocabularyValues(Collections.emptyList());
        datasetFieldType.setFieldType(type);
        return datasetFieldType;
    }

    private static DatasetFieldType createVocabularyType(boolean allowMultiples) {
        DatasetFieldType fieldType = createType(FieldType.NONE);
        Random random = new Random();
        List<ControlledVocabularyValue> values = Arrays.asList("a", "b", "c").stream()
                .map(v -> new ControlledVocabularyValue(random.nextLong(), v, fieldType))
                .collect(Collectors.toList());
        fieldType.setControlledVocabularyValues(values);
        fieldType.setAllowMultiples(allowMultiples);
        return fieldType;
    }
}