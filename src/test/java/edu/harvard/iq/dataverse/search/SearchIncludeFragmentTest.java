package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.MetadataBlock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SearchIncludeFragmentTest {


    @Test
    public void getFriendlyNamesFromFilterQuery_should_return_null_when_null_or_empty_filter_query() {
        SearchIncludeFragment target = new SearchIncludeFragment();

        MatcherAssert.assertThat(target.getFriendlyNamesFromFilterQuery(null), Matchers.nullValue());
        MatcherAssert.assertThat(target.getFriendlyNamesFromFilterQuery(""), Matchers.nullValue());
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_should_return_null_when_filter_query_does_not_have_key_value_pair_separated_by_colon() {
        SearchIncludeFragment target = new SearchIncludeFragment();

        MatcherAssert.assertThat(target.getFriendlyNamesFromFilterQuery("key_value_no_colon"), Matchers.nullValue());
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_key_should_return_key_when_filter_query_key_does_not_match_any_friendly_names() {
        SearchIncludeFragment target = new SearchIncludeFragment();

        List<String> result = target.getFriendlyNamesFromFilterQuery("key:value");
        MatcherAssert.assertThat(result.get(0), Matchers.is("key"));
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_key_should_return_friendly_name_when_filter_query_key_does_match_friendly_name() {
        SearchIncludeFragment target = new SearchIncludeFragment();
        target.datasetfieldFriendlyNamesBySolrField = Map.of("key", "KeyFriendlyName");

        List<String> result = target.getFriendlyNamesFromFilterQuery("key:value");
        MatcherAssert.assertThat(result.get(0), Matchers.is("KeyFriendlyName"));
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_key_should_return_static_friendly_name_when_filter_query_key_does_not_match_friendly_name_but_matches_static_names() {
        SearchIncludeFragment target = new SearchIncludeFragment();
        target.staticSolrFieldFriendlyNamesBySolrField = Map.of("key", "staticKeyFriendlyName");

        List<String> result = target.getFriendlyNamesFromFilterQuery("key:value");
        MatcherAssert.assertThat(result.get(0), Matchers.is("staticKeyFriendlyName"));
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_value_should_return_metadata_block_facet_label_when_key_is_metadata_types_and_value_matches_metadata_block_name() {
        SearchIncludeFragment target = new SearchIncludeFragment();
        Dataverse dataverse = Mockito.mock(Dataverse.class);
        MetadataBlock block = Mockito.mock(MetadataBlock.class);
        Mockito.when(block.getName()).thenReturn("metadata_block_name");
        Mockito.when(block.getLocaleDisplayFacet()).thenReturn("display_facet");
        DataverseMetadataBlockFacet blockFacet = new DataverseMetadataBlockFacet();
        blockFacet.setMetadataBlock(block);
        Mockito.when(dataverse.getMetadataBlockFacets()).thenReturn(Arrays.asList(blockFacet));
        target.setDataverse(dataverse);

        List<String> result = target.getFriendlyNamesFromFilterQuery(String.format("%s:\"metadata_block_name\"", SearchFields.METADATA_TYPES));
        MatcherAssert.assertThat(result.get(1), Matchers.is("display_facet"));

        Mockito.verify(dataverse, Mockito.times(2)).getMetadataBlockFacets();
        Mockito.verify(block).getName();
        Mockito.verify(block).getLocaleDisplayFacet();
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_value_should_return_value_when_key_is_metadata_types_and_value_does_not_matches_metadata_block_name() {
        SearchIncludeFragment target = new SearchIncludeFragment();
        Dataverse dataverse = Mockito.mock(Dataverse.class);
        MetadataBlock block = Mockito.mock(MetadataBlock.class);
        Mockito.when(block.getName()).thenReturn("metadata_block_name");
        Mockito.when(block.getLocaleDisplayFacet()).thenReturn("display_facet");
        DataverseMetadataBlockFacet blockFacet = new DataverseMetadataBlockFacet();
        blockFacet.setMetadataBlock(block);
        Mockito.when(dataverse.getMetadataBlockFacets()).thenReturn(Arrays.asList(blockFacet));
        target.setDataverse(dataverse);

        List<String> result = target.getFriendlyNamesFromFilterQuery(String.format("%s:\"no_match_block_name\"", SearchFields.METADATA_TYPES));
        MatcherAssert.assertThat(result.get(1), Matchers.is("no_match_block_name"));

        Mockito.verify(dataverse, Mockito.times(2)).getMetadataBlockFacets();
        Mockito.verify(block).getName();
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_value_should_return_value_when_key_is_metadata_types_and_dataverse_is_null() {
        SearchIncludeFragment target = new SearchIncludeFragment();

        List<String> result = target.getFriendlyNamesFromFilterQuery(String.format("%s:\"no_dataverse\"", SearchFields.METADATA_TYPES));
        MatcherAssert.assertThat(result.get(1), Matchers.is("no_dataverse"));
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_value_should_return_value_when_key_is_metadata_types_and_metadata_blocks_are_null() {
        SearchIncludeFragment target = new SearchIncludeFragment();
        Dataverse dataverse = Mockito.mock(Dataverse.class);
        Mockito.when(dataverse.getMetadataBlockFacets()).thenReturn(null);
        target.setDataverse(dataverse);

        List<String> result = target.getFriendlyNamesFromFilterQuery(String.format("%s:\"no_metadata_blocks\"", SearchFields.METADATA_TYPES));
        MatcherAssert.assertThat(result.get(1), Matchers.is("no_metadata_blocks"));

        Mockito.verify(dataverse).getMetadataBlockFacets();
    }

    @Test
    public void getFriendlyNamesFromFilterQuery_value_should_remove_quotes_from_beginning_and_end() {
        SearchIncludeFragment target = new SearchIncludeFragment();

        List<String> result = target.getFriendlyNamesFromFilterQuery("key:\"value_\"_with_quotes\"");
        MatcherAssert.assertThat(result.get(1), Matchers.is("value_\"_with_quotes"));
    }
}