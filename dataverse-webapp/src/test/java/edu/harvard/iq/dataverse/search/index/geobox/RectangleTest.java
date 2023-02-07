package edu.harvard.iq.dataverse.search.index.geobox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RectangleTest {

    // -------------------- TESTS --------------------

    @ParameterizedTest(name = "Span from X1={0} to X2={1} should be cut: {2}")
    @CsvSource(delimiter = '|', value = {
        //  X1 |   X2 | Cut expected
        "    0 |  180 |        false",
        "    1 | -179 |        false",
        "   10 |   10 |        false",
        " -169 |   11 |        false",
        "  180 | -180 |        false",
        " -150 |  150 |        true",
        "   -1 |  -10 |        true",
        "   80 |   40 |        true",
        " -180 |  180 |        true"
    })
    void cutIfNeeded__shouldCut(String x1, String x2, boolean cutExpected) {
        // given
        Rectangle rectangle = new Rectangle(x1, "1", x2, "10");

        // when
        List<Rectangle> cut = rectangle.cutIfNeeded();

        // then
        assertThat(cut).hasSize(cutExpected ? 2 : 1);
    }

    @ParameterizedTest(name = "Rectangle [{0}, {1}, {2}, {3}] should be cut " +
            "to rectangles [{4}, {1}, {5}, {3}] and [{6}, {1}, {7}, {3}]")
    @CsvSource(delimiter = '|', value = {
        // X1 |  Y1 |  X2 |  Y2 | X1 of 1st | X2 of 1st | X1 of 2nd | X2 of 2nd
        //    |     |     |     | rectangle | rectangle | rectangle | rectangle
        "-150 |  -1 | 150 |   1 |      -150 |         0 |         0 |       150",
        "  -1 | -12 | -10 |  90 |        -1 |       180 |      -180 |       -10",
        "  80 |   1 |  40 |  10 |        80 |       180 |      -180 |        40",
        "-180 |   3 | 180 |   4 |      -180 |         0 |         0 |       180",
        "-180 |   7 | 180 |   7 |      -180 |         0 |         0 |       180"
    })
    void cutIfNeeded__checkCuts(String x1, String y1, String x2, String y2,
                                String fx1, String fx2, String sx1, String sx2) {
        // given
        Rectangle rectangle = new Rectangle(x1, y1, x2, y2);

        // when
        List<Rectangle> cut = rectangle.cutIfNeeded();

        // then
        assertThat(cut).containsExactlyInAnyOrder(
                new Rectangle(fx1, y1, fx2, y2),
                new Rectangle(sx1, y1, sx2, y2));
    }

    @ParameterizedTest(name = "Rectangle [{0}, {1}, {2}, {3}] has degeneration of type: {4}")
    @CsvSource(delimiter = '|', value = {
            // X1 |   Y1 |     X2 |   Y2 |     Expected
            //    |      |        |      | degeneration
            "   1 |    2 |      3 |    4 |         NONE",
            " 1.0 | 2.00 |  1.000 |    2 |        POINT",
            "   1 |    2 |     32 |  2.0 |         LINE",
            "-180 |  -90 | -180.0 |   90 |         LINE",
            " 180 |    1 |   -180 |    2 |         NONE", // This is not degenerated from Solr POV (as a rectangle)
            " 180 |    0 |   -180 |    0 |         LINE"  // This also is not degenerated (as a line)
    })
    void getDegenerationType(String x1, String y1, String x2, String y2, Rectangle.DegenerationType expectedDegeneration) {
        // given
        Rectangle rectangle = new Rectangle(x1, y1, x2, y2);

        // when
        Rectangle.DegenerationType degenerationType = rectangle.getDegenerationType();

        // then
        assertThat(degenerationType).isEqualTo(expectedDegeneration);
    }
}