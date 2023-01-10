package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class GeoboxComponentValidatorTest {

    private FieldValidator validator = new GeoboxComponentValidator();

    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

    @ParameterizedTest(name = "[{index}] Validation results [X1: {0} ({4}), Y1: {1} ({5}), X2: {2} ({6}), Y2: {3} ({7})]")
    @CsvSource(delimiter = '|' , value = {
        //   X1 |    Y1 |     X2 | Y2 | Exp. X1 | Exp. Y1 | Exp. X2 | Exp. Y2
        "       |       |        |    |    true |    true |    true |    true", // Ok
        "     1 |     3 |      2 |  4 |    true |    true |    true |    true", // Ok
        "     1 |     3 |      2 |    |    true |    true |    true |   false", // Empty Y2
        "     1 |     3 |        |    |    true |    true |   false |   false", // Empty X2 & Y2
        "     1 |     4 |        |  3 |    true |   false |   false |   false", // Empty X2, Y1 > Y2
        "     a |     4 |        |  3 |   false |   false |   false |   false", // Empty X2, Y1 > Y2, X1 not a number
        "     a |     b |        |  3 |   false |   false |   false |    true", // Empty X2, X1 & Y1 not a number
        " 180.1 |  90.1 |        |  3 |   false |   false |   false |   false", // Empty X2, Y1 > Y2, X1 & Y1 out of range
        "  -180 |  90.1 |        |  3 |    true |   false |   false |   false", // Empty X2, Y1 > Y2, Y1 out of range
        "  -180 | -90.1 |        |  3 |    true |   false |   false |    true", // Empty X2, Y1 out of range
        "  -180 |   -90 |        |  3 |    true |    true |   false |    true", // Empty X2
        "  -180 |   -90 | 179.99 |  3 |    true |    true |    true |    true", // Ok
    })
    void isValid(String x1, String y1, String x2, String y2, boolean expX1, boolean expY1, boolean expX2, boolean expY2) {
        GeoboxFields[] components = new GeoboxFields[] { GeoboxFields.X1, GeoboxFields.Y1, GeoboxFields.X2, GeoboxFields.Y2 };
        boolean[] expected = new boolean[] { expX1, expY1, expX2, expY2 };

        // given
        DatasetField geobox = geoboxUtil.buildGeobox(x1, y1, x2, y2);

        for (int i = 0; i < expected.length; i++) {
            DatasetField datasetField = geoboxUtil.selectFromGeobox(components[i], geobox);

            // when
            ValidationResult result = validator.isValid(datasetField, Collections.emptyMap(), Collections.emptyMap());

            // then
            assertThat(result.isOk()).isEqualTo(expected[i]);
        }
    }
}