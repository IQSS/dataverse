package edu.harvard.iq.dataverse.search.query.filter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.search.response.FilterQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GeospatialFilterCreatorTest {

    private static final DatasetFieldType TEST_TYPE = new DatasetFieldType() {
        @Override public String getName() {
            return "TestType";
        }

        @Override public String getDisplayName() {
            return "Test Type";
        }

        @Override public FieldType getFieldType() {
            return FieldType.GEOBOX;
        }
    };

    @Mock
    private DatasetFieldTypeRepository repository;

    @Mock
    private SpecialFilterService specialFilterService;

    @InjectMocks
    private GeospatialFilterCreator creator;

    // -------------------- TESTS --------------------

    @Test
    void create__normalCase() {
        // given
        Mockito.when(repository.findByName(Mockito.anyString()))
                .thenReturn(Optional.of(TEST_TYPE));
        String initialQuery = "[GEO[TestType|1.4W|2.3E|4.2N|3.1S]]";

        // when
        SpecialFilter result = creator.create(initialQuery, "TestType", "1.4W", "2.3E", "4.2N", "3.1S");

        // then
        assertThat(result.solrQuery).isEqualTo("{!field f=dsf_geobox_TestType}Intersects(POLYGON((1.4 3.1,2.3 3.1,2.3 4.2,1.4 4.2,1.4 3.1)))");
        assertThat(result.filterQuery)
                .extracting(FilterQuery::getQuery, FilterQuery::getFriendlyFieldName, FilterQuery::getFriendlyFieldValue)
                .containsExactly(initialQuery, "Test Type", "1.4W, 2.3E, 4.2N, 3.1S");
    }

    @Test
    void create__noDatasetFieldTypeFound() {
        // given
        Mockito.when(repository.findByName(Mockito.anyString()))
                .thenReturn(Optional.empty());

        // when
        SpecialFilter result = creator.create("[GEO[TestType|1W|2E|4N|3S]]", "TestType", "1W", "2E", "4N", "3S");

        // then
        assertThat(result).isEqualTo(SpecialFilter.EMPTY);
    }

    static Stream<Arguments> create__wrongNumberOfParams() {
        return Stream.of(
                Arguments.of((Object) new String[] {"TestType", "1W", "2E", "4N"}),
                Arguments.of((Object) new String[] {"TestType", "1W", "2E", "4N", ""}),
                Arguments.of((Object) new String[] {"TestType", "1W", "2E", "4N", "3S", "ABC"}),
                Arguments.of((Object) new String[] {"1W", "2E", "4N", "3S"}));
    }

    @ParameterizedTest
    @MethodSource
    void create__wrongNumberOfParams(String[] params) {
        // given & when
        SpecialFilter result = creator.create(String.format("[GEO[%s]]", String.join("|", params)), params);

        // then
        assertThat(result).isEqualTo(SpecialFilter.EMPTY);
    }

    @Test
    void create__malformedParams() {
        // given
        Mockito.when(repository.findByName(Mockito.anyString()))
                .thenReturn(Optional.of(TEST_TYPE));

        // when & then
        assertThatThrownBy(() -> creator.create("[GEO[TestType|1.43|2.3E|4.2N|3.1S]]",
                "TestType", "1.43", "2.3E", "4.2N", "3.1S"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}