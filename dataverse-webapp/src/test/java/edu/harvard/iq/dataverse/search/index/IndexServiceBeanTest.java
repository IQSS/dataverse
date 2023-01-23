package edu.harvard.iq.dataverse.search.index;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexServiceBeanTest {

    @Test
    void getDataversePathsFromSegments() {
        // given
        List<String> segments = Arrays.asList("1", "23", "456", "7890");

        // when
        List<String> paths = new IndexServiceBean().getDataversePathsFromSegments(segments);

        // then
        assertThat(paths).containsExactlyInAnyOrder("/1", "/1/23", "/1/23/456", "/1/23/456/7890");
    }
}