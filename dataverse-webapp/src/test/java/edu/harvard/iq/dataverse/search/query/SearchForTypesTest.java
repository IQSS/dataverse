package edu.harvard.iq.dataverse.search.query;

import static edu.harvard.iq.dataverse.search.query.SearchObjectType.DATASETS;
import static edu.harvard.iq.dataverse.search.query.SearchObjectType.DATAVERSES;
import static edu.harvard.iq.dataverse.search.query.SearchObjectType.FILES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class SearchForTypesTest {

    @Test
    public void emptySearch_containsNoElements() {

        SearchForTypes search = SearchForTypes.empty();

        assertFalse(search.containsDatasets());
        assertFalse(search.containsDataverse());
        assertFalse(search.containsFiles());

        assertFalse(search.contains(DATASETS));
        assertFalse(search.contains(DATAVERSES));
        assertFalse(search.contains(FILES));

        assertFalse(search.containsOnly(DATASETS));
        assertFalse(search.containsOnly(DATAVERSES));
        assertFalse(search.containsOnly(FILES));

        assertEquals(0L, search.types().count());
    }

    @Test
    public void fullSearch_containsAllElements() {

        SearchForTypes search = SearchForTypes.all();

        assertTrue(search.containsDatasets());
        assertTrue(search.containsDataverse());
        assertTrue(search.containsFiles());

        assertTrue(search.contains(DATASETS));
        assertTrue(search.contains(DATAVERSES));
        assertTrue(search.contains(FILES));

        assertFalse(search.containsOnly(DATASETS));
        assertFalse(search.containsOnly(DATAVERSES));
        assertFalse(search.containsOnly(FILES));

        assertEquals(3L, search.types().count());
    }

    @Test
    public void specified_containsOnlySepacifiedElements() {

        SearchForTypes search = SearchForTypes.byTypes(FILES);

        assertFalse(search.containsDatasets());
        assertFalse(search.containsDataverse());
        assertTrue(search.containsFiles());

        assertFalse(search.contains(DATASETS));
        assertFalse(search.contains(DATAVERSES));
        assertTrue(search.contains(FILES));

        assertFalse(search.containsOnly(DATASETS));
        assertFalse(search.containsOnly(DATAVERSES));
        assertTrue(search.containsOnly(FILES));

        assertEquals(1L, search.types().count());

        search = SearchForTypes.byTypes(FILES, DATASETS);

        assertTrue(search.containsDatasets());
        assertFalse(search.containsDataverse());
        assertTrue(search.containsFiles());

        assertTrue(search.contains(DATASETS));
        assertFalse(search.contains(DATAVERSES));
        assertTrue(search.contains(FILES));

        assertFalse(search.containsOnly(DATASETS));
        assertFalse(search.containsOnly(DATAVERSES));
        assertFalse(search.containsOnly(FILES));

        assertEquals(2L, search.types().count());
    }

    @Test
    public void contains_returnsFalse_forNull() {

        assertFalse(SearchForTypes.all().contains(null));
    }

    @Test
    public void containsOnly_returnsFalse_forNull() {

        assertFalse(SearchForTypes.all().containsOnly(null));
    }

    @Test
    public void togglingDisabledType_enablesIt() {

        SearchForTypes search = SearchForTypes.byTypes(FILES);

        SearchForTypes modified = search.toggleType(DATASETS);

        assertTrue(modified.containsDatasets());
        assertFalse(modified.containsDataverse());
        assertTrue(modified.containsFiles());

        assertTrue(modified.contains(DATASETS));
        assertFalse(modified.contains(DATAVERSES));
        assertTrue(modified.contains(FILES));

        assertEquals(2L, modified.types().count());

        // original object hasn't been hanged
        assertFalse(search.containsDatasets());
        assertFalse(search.containsDataverse());
        assertTrue(search.containsFiles());

        assertFalse(search.contains(DATASETS));
        assertFalse(search.contains(DATAVERSES));
        assertTrue(search.contains(FILES));

        assertFalse(search.containsOnly(DATASETS));
        assertFalse(search.containsOnly(DATAVERSES));
        assertTrue(search.containsOnly(FILES));

        assertEquals(1L, search.types().count());
    }

    @Test
    public void togglingEsabledType_DisablesIt() {

        SearchForTypes search = SearchForTypes.byTypes(FILES);

        SearchForTypes modified = search.toggleType(FILES);

        assertFalse(modified.containsDatasets());
        assertFalse(modified.containsDataverse());
        assertFalse(modified.containsFiles());

        assertFalse(modified.contains(DATASETS));
        assertFalse(modified.contains(DATAVERSES));
        assertFalse(modified.contains(FILES));

        assertEquals(0L, modified.types().count());

        // original object hasn't been hanged
        assertFalse(search.containsDatasets());
        assertFalse(search.containsDataverse());
        assertTrue(search.containsFiles());

        assertFalse(search.contains(DATASETS));
        assertFalse(search.contains(DATAVERSES));
        assertTrue(search.contains(FILES));

        assertFalse(search.containsOnly(DATASETS));
        assertFalse(search.containsOnly(DATAVERSES));
        assertTrue(search.containsOnly(FILES));

        assertEquals(1L, search.types().count());
    }

    @Test
    public void toggleType_throwsNullPointer_whenGivenNull() {

        try {
            SearchForTypes.all().toggleType(null);
            fail();
        } catch (final NullPointerException e) {
            // good
        }
    }

    @Test
    public void takeInverse() {

        SearchForTypes types = SearchForTypes.all().takeInverse();

        assertFalse(types.containsDatasets());
        assertFalse(types.containsDataverse());
        assertFalse(types.containsFiles());

        assertFalse(types.contains(DATASETS));
        assertFalse(types.contains(DATAVERSES));
        assertFalse(types.contains(FILES));

        types = types.takeInverse();

        assertTrue(types.containsDatasets());
        assertTrue(types.containsDataverse());
        assertTrue(types.containsFiles());

        assertTrue(types.contains(DATASETS));
        assertTrue(types.contains(DATAVERSES));
        assertTrue(types.contains(FILES));

        types = SearchForTypes.byTypes(DATAVERSES, DATASETS).takeInverse();

        assertFalse(types.containsDatasets());
        assertFalse(types.containsDataverse());
        assertTrue(types.containsFiles());

        assertFalse(types.contains(DATASETS));
        assertFalse(types.contains(DATAVERSES));
        assertTrue(types.contains(FILES));
    }

    @Test
    public void multipleSameValues_areIgnored_duringSearchCreation() {

        SearchForTypes search = SearchForTypes.byTypes(FILES, FILES);

        assertFalse(search.containsDatasets());
        assertFalse(search.containsDataverse());
        assertTrue(search.containsFiles());

        assertFalse(search.contains(DATASETS));
        assertFalse(search.contains(DATAVERSES));
        assertTrue(search.contains(FILES));
    }
    
    @Test
    public void byTypes_varArg_throwsNullPointer_whenGivenNulls() {
        
        try {
            SearchForTypes.byTypes(null, null);
            fail();
        } catch (final NullPointerException e) {
            // good
        }
        
        try {
            SearchForTypes.byTypes(Arrays.asList(FILES, null));
            fail();
        } catch (final NullPointerException e) {
            // good
        }
    }
}
