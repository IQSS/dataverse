package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class GeoboxFillValidatorTest {

    private FieldValidator validator = new GeoboxFillValidator();

    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

    @ParameterizedTest(name = "[{index}] It''s {4} that X1 in geobox [X1: {0}, Y1: {1}, X2: {2}, Y2: {3}] is properly filled")
    @CsvSource(delimiter = '|' , value = {
           // X1 | Y1 | X2 | Y2 | Expected
            "  1 |  1 |  1 |  1 |     true",
            "    |    |    |    |     true",
            "  1 |    |    |    |     true",
            "  1 |    |  1 |    |     true",
            "    |    |  1 |    |    false",
            "    |  1 |    |  1 |    false"
    })
    void isValid(String x1, String y1, String x2, String y2, boolean expected) {
        // given
        DatasetField fieldX1 = geoboxUtil.selectFromGeobox(GeoboxFields.X1, geoboxUtil.buildGeobox(x1, y1, x2, y2));

        // when
        ValidationResult result = validator.isValid(fieldX1, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expected);
    }
}