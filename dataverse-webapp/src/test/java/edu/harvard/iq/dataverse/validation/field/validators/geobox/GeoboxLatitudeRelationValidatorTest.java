package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class GeoboxLatitudeRelationValidatorTest {

    private FieldValidator validator = new GeoboxLatitudeRelationValidator();

    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

    @ParameterizedTest(name = "[{index}] It''s {3} that latitude relation between Y1: {1} and Y2: {2} for {0} is valid")
    @CsvSource(delimiter = '|' , value = {
        // Test |     Y1 |    Y2 | Expected
            "Y1 |     18 |    28 |     true",
            "Y1 |     18 |  1.ab |     true",
            "Y2 |     18 |    28 |     true",
            "Y2 |     0a |    28 |     true",
            "Y1 |        |   -20 |     true",
            "Y1 |        |   abc |     true",
            "Y2 |        |   -20 |     true",
            "Y1 |  23.45 |       |     true",
            "Y2 |  23.45 |       |     true",
            "Y2 |   0x0f |       |     true",
            "Y1 |        |       |     true",
            "Y2 |        |       |     true",
            "Y1 |     89 | -23.2 |    false",
            "Y2 |     89 | -23.2 |    false",
            "Y1 | -11.12 | -89.1 |    false",
            "Y2 | -11.12 | -89.1 |    false"
    })
    void isValid(GeoboxFields field, String y1, String y2, boolean expected) {

        // given
        DatasetField datasetField = geoboxUtil.selectFromGeobox(field, geoboxUtil.buildGeobox("1", y1, "1", y2));

        // when
        ValidationResult result = validator.isValid(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expected);
    }
}