package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxTestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeoboxIndexUtilTest {
    private GeoboxIndexUtil geoboxIndexUtil = new GeoboxIndexUtil();
    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

    @Test
    void geoboxFieldToSolr__noCut() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("1", "2", "3", "4");

        // when
        List<String> solrData = geoboxIndexUtil.geoboxFieldToSolr(field);

        // then
        assertThat(solrData).containsExactlyInAnyOrder("POLYGON((1 2,3 2,3 4,1 4,1 2))");
    }

    @Test
    void geoboxFieldToSolr__cut() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("-180", "-1", "180", "1");

        // when
        List<String> solrData = geoboxIndexUtil.geoboxFieldToSolr(field);

        // then
        assertThat(solrData).containsExactlyInAnyOrder(
                "POLYGON((-180 -1,0 -1,0 1,-180 1,-180 -1))",
                "POLYGON((0 -1,180 -1,180 1,0 1,0 -1))");
    }

    // We do not check another cases as they're covered by tests of geobox validators
    @Test
    void isIndexable__rejectInconsistentField() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("1", "-11", "20", "11");
        field.getDatasetFieldsChildren().remove(0); // Now not all coordinates are present

        // when
        boolean result = geoboxIndexUtil.isIndexable(field);

        // then
        assertThat(result).isFalse();
    }
}