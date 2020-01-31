package edu.harvard.iq.dataverse.search.query;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchForTypesTest {

    // -------------------- TESTS --------------------

    @Test
    public void toggleType__remove_type() {
        // given
        SearchForTypes typesToSearch = SearchForTypes.byTypes(SearchObjectType.DATAVERSES, SearchObjectType.FILES);
        // when
        SearchForTypes resultTypesToSearch = typesToSearch.toggleType(SearchObjectType.FILES);
        // then
        assertThat(resultTypesToSearch.getTypes(), contains(SearchObjectType.DATAVERSES));
    }
    
    @Test
    public void toggleType__add_type() {
        // given
        SearchForTypes typesToSearch = SearchForTypes.byTypes(SearchObjectType.DATAVERSES, SearchObjectType.FILES);
        // when
        SearchForTypes resultTypesToSearch = typesToSearch.toggleType(SearchObjectType.DATASETS);
        // then
        assertThat(resultTypesToSearch.getTypes(), containsInAnyOrder(SearchObjectType.DATAVERSES, SearchObjectType.DATASETS, SearchObjectType.FILES));
    }
    
    @Test
    public void containsOnly() {
        // when & then
        assertTrue(SearchForTypes.byTypes(SearchObjectType.DATAVERSES).containsOnly(SearchObjectType.DATAVERSES));
        assertFalse(SearchForTypes.byTypes(SearchObjectType.DATAVERSES, SearchObjectType.FILES).containsOnly(SearchObjectType.DATAVERSES));
    }
    
    @Test
    public void byType() {
        // when
        SearchForTypes typesToSearch = SearchForTypes.byTypes(SearchObjectType.DATAVERSES, SearchObjectType.FILES);
        // then
        assertThat(typesToSearch.getTypes(), containsInAnyOrder(SearchObjectType.DATAVERSES, SearchObjectType.FILES));
    }

    @Test
    public void byType__multiple_same_types() {
        // when
        SearchForTypes typesToSearch = SearchForTypes.byTypes(SearchObjectType.DATAVERSES, SearchObjectType.DATAVERSES);
        // then
        assertThat(typesToSearch.getTypes(), contains(SearchObjectType.DATAVERSES));
    }

    @Test
    public void byType__no_type() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> SearchForTypes.byTypes());
    }

    @Test
    public void all() {
        // when
        SearchForTypes typesToSearch = SearchForTypes.all();
        // then
        assertThat(typesToSearch.getTypes(), containsInAnyOrder(
                SearchObjectType.DATAVERSES, SearchObjectType.DATASETS, SearchObjectType.FILES));
    }

}
