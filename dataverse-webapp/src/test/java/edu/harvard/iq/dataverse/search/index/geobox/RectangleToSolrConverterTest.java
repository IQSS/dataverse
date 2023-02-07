package edu.harvard.iq.dataverse.search.index.geobox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}