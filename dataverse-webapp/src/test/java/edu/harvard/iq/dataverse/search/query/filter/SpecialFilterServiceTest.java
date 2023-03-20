package edu.harvard.iq.dataverse.search.query.filter;

import edu.harvard.iq.dataverse.search.response.FilterQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SpecialFilterServiceTest {

    @Mock
    private SpecialFilterCreator testCreator;

    private SpecialFilterService service = new SpecialFilterService();

    // -------------------- TESTS --------------------

    @Test
    void register() {
        // given
        Mockito.when(testCreator.getKey()).thenReturn("TEST");

        // when
        service.register(testCreator);

        // then
        service.createFromQuery("[TEST[a|b|c]]");
        Mockito.verify(testCreator, Mockito.times(1)).getKey();
    }

    @ParameterizedTest
    @CsvSource(value = {
            //                   Value | Expected
            "[GEO[geobox|1W|2E|4N|3S]] ,     true",
            "      [TEST[a|b|c|d|e|f]] ,     true",
            "     [[TEST|a|b|c|d|e|f]] ,    false",
            "            [a|b|c|d|e|f] ,    false",
            "          [[a|b|c|d|e|f]] ,    false",
            "                 [1 TO *] ,    false",
            "                someValue ,    false"
    })
    void isSpecialFilter(String query, boolean expected) {
        // given & when
        boolean result = service.isSpecialFilter(query);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should choose proper creator and return SpecialFilter")
    void createFromQuery() {
        // given
        service.register(createSpecialFilterCreator("Test"));
        service.register(createSpecialFilterCreator("Geo"));

        String query = "[TEST[a|b|c|d]]";

        // when
        SpecialFilter result = service.createFromQuery(query);

        // then
        assertThat(result.query).isEqualTo(query);
        assertThat(result.solrQuery).isEqualTo("a, b, c, d");
        assertThat(result.filterQuery)
                .extracting(FilterQuery::getQuery, FilterQuery::getFriendlyFieldName, FilterQuery::getFriendlyFieldValue)
                .containsExactly(query, "Test", "a, b, c, d");
    }

    // -------------------- PRIVATE --------------------

    private SpecialFilterCreator createSpecialFilterCreator(String name) {
        return new SpecialFilterCreator() {
            @Override public String getKey() {
                return name.toUpperCase();
            }

            @Override public SpecialFilter create(String query, String... params) {
                String value = String.join( ", ", params);
                return new SpecialFilter(query, value, new FilterQuery(query, name, value));
            }
        };
    }
}