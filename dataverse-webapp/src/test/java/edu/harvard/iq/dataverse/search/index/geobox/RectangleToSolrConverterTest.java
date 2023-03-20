package edu.harvard.iq.dataverse.search.index.geobox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RectangleToSolrConverterTest {

    private RectangleToSolrConverter converter = new RectangleToSolrConverter();

    // -------------------- TESTS --------------------

    @ParameterizedTest(name = "Rectangle [{0}, {1}, {2}, {3}] should produce {4}")
    @CsvSource(delimiter = '|', value = {
            // X1 |  Y1 |   X2 |     Y2 | Expected shape
            "   1 |   2 |    3 |      4 | POLYGON((1 2,3 2,3 4,1 4,1 2))",
            "  -1 |   2 |  1.1 |      4 | POLYGON((-1 2,1.1 2,1.1 4,-1 4,-1 2))",
            "-180 |   0 |    0 | 89.321 | POLYGON((-180 0,0 0,0 89.321,-180 89.321,-180 0))",
            "   1 |   0 |    1 |      0 | POINT(1 0)",
            "-180 |   0 |    0 |      0 | LINESTRING(-180 0,0 0)",
            "   1 | -90 |    1 |     90 | LINESTRING(1 -90,1 90)",
            " 180 |   0 | -180 |      1 | POLYGON((180 0,-180 0,-180 1,180 1,180 0))", // From Solr POV this is a valid polygon
            " 180 |   0 | -180 |      0 | LINESTRING(180 0,-180 0)" // From Solr POV this is a valid line
    })
    void toSolrPolygon(String x1, String y1, String x2, String y2, String expectedShape) {
        // given
        Rectangle rectangle = new Rectangle(x1, y1, x2, y2);

        // when
        String shape = converter.toSolrPolygon(rectangle);

        // then
        assertThat(shape).isEqualTo(expectedShape);
    }

    static Stream<Arguments> wrapIfNeeded() {
        return Stream.of(
                Arguments.of(Arrays.asList(
                        new Rectangle("1", "2", "3", "4"),
                        new Rectangle("1", "0", "1", "0"),
                        new Rectangle("180", "0", "0", "0")),
                    "GEOMETRYCOLLECTION(POLYGON((1 2,3 2,3 4,1 4,1 2)),POINT(1 0),LINESTRING(180 0,0 0))"),
                Arguments.of(Collections.singletonList(new Rectangle("11", "22", "33", "44")),
                    "POLYGON((11 22,33 22,33 44,11 44,11 22))"));
    }

    @ParameterizedTest
    @MethodSource
    void wrapIfNeeded(List<Rectangle> rects, String expected) {
        // given & when
        String result = converter.wrapIfNeeded(rects);

        // then
        assertThat(result).isEqualTo(expected);
    }
}