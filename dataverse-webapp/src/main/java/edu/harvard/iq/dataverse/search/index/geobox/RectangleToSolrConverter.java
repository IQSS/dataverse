package edu.harvard.iq.dataverse.search.index.geobox;

public class RectangleToSolrConverter {
    private static final String POLY_TEMPLATE = "POLYGON((%s))";
    private static final String POINT_TEMPLATE = "POINT(%s)";
    private static final String LINE_TEMPLATE = "LINESTRING(%s)";

    // -------------------- LOGIC --------------------

    /**
     * Converts a rectangle to Solr shape: POINT, LINESTRING or POLYGON.
     * <br><br>
     * Input rectangle should be result of earlier call to {@link Rectangle#cutIfNeeded()},
     * in order to divide rectangles of span greater than 180 degrees into
     * smaller ones (as Solr does not handle those of a span greater than
     * 180 degrees – it inverts them, wrapping around other side of the globe,
     * to produce smaller rectangle, which is not what we want).
     */
    public String toSolrPolygon(Rectangle rect) {
        switch (rect.getDegenerationType()) {
            case NONE:
                String data = String.join(",",
                        point(rect.x1, rect.y1),
                        point(rect.x2, rect.y1),
                        point(rect.x2, rect.y2),
                        point(rect.x1, rect.y2),
                        point(rect.x1, rect.y1));
                return String.format(POLY_TEMPLATE, data);
            case POINT:
                return String.format(POINT_TEMPLATE, point(rect.x1, rect.y1));
            case LINE:
                return String.format(LINE_TEMPLATE,
                        String.join(",", point(rect.x1, rect.y1), point(rect.x2, rect.y2)));
            default:
                throw new IllegalStateException("Unrecognized degeneration type " + rect.getDegenerationType());
        }
    }

    // -------------------- PRIVATE --------------------

    private String point(String x, String y) {
        return x + " " + y;
    }
}
