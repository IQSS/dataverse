package edu.harvard.iq.dataverse.search.index.geobox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Rectangle {
    private static final BigDecimal DEG_360 = new BigDecimal(360);
    private static final BigDecimal DEG_180 = new BigDecimal(180);

    private static final String PLUS_180 = "180";
    private static final String MINUS_180 = "-180";
    private static final String ZERO = "0";

    public final String x1;
    public final String y1;
    public final String x2;
    public final String y2;

    // -------------------- CONSTRUCTORS --------------------

    public Rectangle(String x1, String y1, String x2, String y2) {
        this.x1 = Objects.requireNonNull(x1);
        this.y1 = Objects.requireNonNull(y1);
        this.x2 = Objects.requireNonNull(x2);
        this.y2 = Objects.requireNonNull(y2);
    }

    // -------------------- LOGIC --------------------

    /**
     * Divides geobox if the span is greater than 180 degrees
     * (as Solr does not handle rectangles of span greater than that).
     */
    public List<Rectangle> cutIfNeeded() {
        CutType cutType = getCutType();
        if (cutType == CutType.NO_CUT) {
            return Collections.singletonList(this);
        } else {
            List<Rectangle> cut = new ArrayList<>();
            cut.add(new Rectangle(x1, y1, cutType == CutType.BY_0 ? ZERO : PLUS_180, y2));
            cut.add(new Rectangle(cutType == CutType.BY_0 ? ZERO : MINUS_180, y1, x2, y2));
            return cut;
        }
    }

    /**
     * Checks whether rectangle is regular, or degenerates to a point or a line.
     * We have to know that for proper conversion to Solr shape, as – for example –
     * we cannot use polygon to represent a point.
     */
    public DegenerationType getDegenerationType() {
        boolean sameX = new BigDecimal(x1).compareTo(new BigDecimal(x2)) == 0;
        boolean sameY = new BigDecimal(y1).compareTo(new BigDecimal(y2)) == 0;

        if (sameX && sameY) {
            return DegenerationType.POINT;
        } else if (sameX || sameY) {
            return DegenerationType.LINE;
        } else {
            return DegenerationType.NONE;
        }
    }

    // -------------------- PRIVATE --------------------

    private CutType getCutType() {
        BigDecimal _x1 = new BigDecimal(x1);
        BigDecimal _x2 = new BigDecimal(x2);
        boolean wrappedAroundAntimeridian = _x1.compareTo(_x2) > 0;
        BigDecimal span = wrappedAroundAntimeridian ? DEG_360.subtract(_x1.subtract(_x2)) : _x2.subtract(_x1);
        return span.compareTo(DEG_180) > 0
                ? wrappedAroundAntimeridian
                    ? CutType.BY_180
                    : CutType.BY_0
                : CutType.NO_CUT;
    }

    // -------------------- equals & hashCode --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rectangle rectangle = (Rectangle) o;
        return x1.equals(rectangle.x1)
                && y1.equals(rectangle.y1)
                && x2.equals(rectangle.x2)
                && y2.equals(rectangle.y2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x1, y1, x2, y2);
    }

    // -------------------- INNER CLASSES --------------------

    private enum CutType {
        NO_CUT,
        BY_0,
        BY_180
    }

    public enum DegenerationType {
        NONE,
        LINE,
        POINT
    }
}
