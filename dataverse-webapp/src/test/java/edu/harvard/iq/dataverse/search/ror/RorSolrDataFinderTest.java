package edu.harvard.iq.dataverse.search.ror;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class RorSolrDataFinderTest {

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrQuerySanitizer solrQuerySanitizer;

    @InjectMocks
    private RorSolrDataFinder solrDataFinder;

    @Captor
    private ArgumentCaptor<SolrQuery> solrQueryArgumentCaptor;

    @Test
    void findRorData_WithSingleValue() throws IOException, SolrServerException {
        //given
        String searchPhrase = "search";
        final QueryResponse queryResponse = new QueryResponse();
        queryResponse.setResponse(new NamedList<>(ImmutableMap.of("response", new SolrDocumentList())));

        //when
        Mockito.when(solrClient.query(Mockito.any())).thenReturn(queryResponse);
        Mockito.when(solrQuerySanitizer.sanitizeRorQuery(searchPhrase)).thenReturn(searchPhrase);
        solrDataFinder.findRorData(searchPhrase, 15);

        //then
        Mockito.verify(solrClient, Mockito.times(1)).query(solrQueryArgumentCaptor.capture());
        Assertions.assertThat(solrQueryArgumentCaptor.getValue().getQuery()).isEqualTo("search*");
        Assertions.assertThat(solrQueryArgumentCaptor.getValue().getRows()).isEqualTo(15);
    }

    @Test
    void findRorData_WithMultipleValues() throws IOException, SolrServerException {
        //given
        String searchPhrase = "search many phrases";
        final QueryResponse queryResponse = new QueryResponse();
        queryResponse.setResponse(new NamedList<>(ImmutableMap.of("response", new SolrDocumentList())));

        //when
        Mockito.when(solrClient.query(Mockito.any())).thenReturn(queryResponse);
        Mockito.when(solrQuerySanitizer.sanitizeRorQuery(searchPhrase)).thenReturn(searchPhrase);
        solrDataFinder.findRorData(searchPhrase, 15);

        //then
        Mockito.verify(solrClient, Mockito.times(1)).query(solrQueryArgumentCaptor.capture());
        Assertions.assertThat(solrQueryArgumentCaptor.getValue().getQuery()).isEqualTo("search* AND many* AND phrases*");
        Assertions.assertThat(solrQueryArgumentCaptor.getValue().getRows()).isEqualTo(15);
    }
}