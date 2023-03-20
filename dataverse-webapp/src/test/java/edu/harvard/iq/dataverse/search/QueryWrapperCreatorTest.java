package edu.harvard.iq.dataverse.search;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.QueryWrapperCreator;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.LicenseCheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SelectOneSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryWrapper;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class QueryWrapperCreatorTest {

    QueryWrapperCreator queryWrapperCreator = new QueryWrapperCreator();

    static Stream<Arguments> constructQuery() {
        return Stream.of(
                Arguments.of(createTextSearchFields(), "text1:testValue1 AND text2:testValue2"),
                Arguments.of(createLongTextSearchFields(), "text1:very AND text1:long AND text1:text AND text1:one AND " +
                        "text2:very AND text2:long AND text2:text AND text2:two"),
                Arguments.of(createBothNumbersSearchFields(), "number1:[1 TO 2] AND number2:[3.1 TO 4.1]"),
                Arguments.of(createOneNumberSearchFields(), "number1:[1 TO *] AND number2:[* TO 4.1]"),
                Arguments.of(createBothDatesSearchFields(), "date1:[2022-03-01 TO 2022-03-07]"),
                Arguments.of(createOneDateSearchFields(), "date1:[2022-02-28 TO *] AND date2:[* TO 2022-07-31]"),
                Arguments.of(createCheckboxSearchFields(), "checkboxValues:\"checkboxValue1\" AND checkboxValues:\"checkboxValue2\""),
                Arguments.of(createSelectOneSearchFields(), "selectOneValue1:\"checkedFieldValue1\" AND selectOneValue2:\"checkedFieldValue2\""),
                Arguments.of(createCheckboxLicenseSearchFields(), "(license:\"License 1\" OR license:\"License 2\") AND dvObjectType:\"files\""));
    }

    @ParameterizedTest(name = "{0} should produce \"{1}\"")
    @MethodSource
    void constructQuery(List<SearchField> searchFields, String expectedResult) {
        // given
        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", searchFields);

        // when
        String result = queryWrapperCreator.constructQueryWrapper(Collections.singletonList(searchBlock)).getQuery();

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void constructQuery__combinedQuery() {
        // given
        List<SearchField> searchFields = Stream.of(createBothNumbersSearchFields(), createOneNumberSearchFields(),
                createCheckboxSearchFields(), createTextSearchFields(),
                createDateSearchFields(), createSelectOneSearchFields(), createGeoboxField())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        SearchBlock searchBlock = new SearchBlock("TEST", "TEST", searchFields);

        // when
        QueryWrapper result = queryWrapperCreator.constructQueryWrapper(Lists.newArrayList((searchBlock)));

        // then
        assertThat(result.getQuery()).isEqualTo("number1:[1 TO 2] AND number2:[3.1 TO 4.1] AND number1:[1 TO *] AND" +
                                    " number2:[* TO 4.1] AND checkboxValues:\"checkboxValue1\" AND checkboxValues:\"checkboxValue2\" AND" +
                                    " text1:testValue1 AND text2:testValue2 AND date1:[2022-01-01 TO *] AND date2:[* TO 2022-02-07] " +
                                    "AND date3:[2022-07-07 TO 2022-12-31] AND selectOneValue1:\"checkedFieldValue1\" " +
                                    "AND selectOneValue2:\"checkedFieldValue2\"");
        assertThat(result.getFilters())
                .containsExactly("[GEO[GoespatialBox|7W|77S|8E|88N]]");
    }

    // -------------------- PRIVATE --------------------

    private static List<SearchField> createBothNumbersSearchFields() {
        NumberSearchField testValue1 = new NumberSearchField(createType("number1", "number1", "desc"));
        testValue1.setMinimum("1");
        testValue1.setMaximum("2");

        NumberSearchField testValue2 = new NumberSearchField(createType("number2", "number2", "desc"));
        testValue2.setMinimum("3.1");
        testValue2.setMaximum("4.1");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private static List<SearchField> createOneNumberSearchFields() {
        NumberSearchField testValue1 = new NumberSearchField(createType("number1", "number1", "desc"));
        testValue1.setMinimum("1");

        NumberSearchField testValue2 = new NumberSearchField(createType("number2", "number2", "desc"));
        testValue2.setMaximum("4.1");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private static List<SearchField> createBothDatesSearchFields() {
        DateSearchField testValue1 = new DateSearchField(createType("date1", "date1", "desc"));
        testValue1.setLowerLimit("2022-03-01");
        testValue1.setUpperLimit("2022-03-07");

        return Lists.newArrayList(testValue1);
    }

    private static List<SearchField> createOneDateSearchFields() {
        DateSearchField testValue1 = new DateSearchField(createType("date1", "date1", "desc"));
        testValue1.setLowerLimit("2022-02-28");

        DateSearchField testValue2 = new DateSearchField(createType("date2", "date2", "desc"));
        testValue2.setUpperLimit("2022-07-31");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private static List<SearchField> createCheckboxSearchFields() {
        CheckboxSearchField testValue1 = new CheckboxSearchField(createType("checkboxValues", "checkboxValues", "desc"));
        testValue1.getCheckedFieldValues().addAll(Lists.newArrayList("checkboxValue1", "checkboxValue2"));

        return Lists.newArrayList(testValue1);
    }

    private static List<SearchField> createCheckboxLicenseSearchFields() {
        Map<Long, String> licenseNames = new HashMap<>();
        licenseNames.put(1L, "License 1");
        licenseNames.put(2L, "License 2");
        LicenseCheckboxSearchField testValue1 = new LicenseCheckboxSearchField("license", "checkboxLicenseValues", "desc", licenseNames);
        testValue1.getCheckedFieldValues().addAll(Lists.newArrayList("license:1", "license:2", "license:3"));

        return Lists.newArrayList(testValue1);
    }

    private static List<SearchField> createSelectOneSearchFields() {
        SelectOneSearchField testValue1 = new SelectOneSearchField(createType("selectOneValue1", "selectOneDisplayValue1", "desc1"));
        testValue1.setCheckedFieldValue("checkedFieldValue1");

        SelectOneSearchField testValue2 = new SelectOneSearchField(createType("selectOneValue2", "selectOneDisplayValue2", "desc2"));
        testValue2.setCheckedFieldValue("checkedFieldValue2");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private static List<SearchField> createTextSearchFields() {
        TextSearchField testValue1 = new TextSearchField(createType("text1", "text1", "desc"));
        testValue1.setFieldValue("testValue1");

        TextSearchField testValue2 = new TextSearchField(createType("text2", "text2", "desc"));
        testValue2.setFieldValue("testValue2");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private static List<SearchField> createDateSearchFields() {
        DateSearchField testValue1 = new DateSearchField(createType("date1", "date1", "desc"));
        testValue1.setLowerLimit("2022-01-01");

        DateSearchField testValue2 = new DateSearchField(createType("date2", "date2", "desc"));
        testValue2.setUpperLimit("2022-02-07");

        DateSearchField testValue3 = new DateSearchField(createType("date3", "date3", "desc"));
        testValue3.setLowerLimit("2022-07-07");
        testValue3.setUpperLimit("2022-12-31");

        return Lists.newArrayList(testValue1, testValue2, testValue3);
    }

    private static List<SearchField> createLongTextSearchFields() {
        TextSearchField testValue1 = new TextSearchField(createType("text1", "text1", "desc"));
        testValue1.setFieldValue("very long text one");

        TextSearchField testValue2 = new TextSearchField(createType("text2", "text2", "desc"));
        testValue2.setFieldValue("very long text two");

        return Lists.newArrayList(testValue1, testValue2);
    }

    private static List<SearchField> createGeoboxField() {
        return new GeoboxTestUtil().buildGeoboxSearchField("7", "77", "8", "88").getChildren();
    }

    private static DatasetFieldType createType(String name, String displayName, String description) {
        DatasetFieldType type = new DatasetFieldType() {
            @Override
            public String getDisplayName() { return displayName; }
        };
        type.setName(name);
        type.setDescription(description);
        return type;
    }
}