package edu.harvard.iq.dataverse.search.ror;

import com.google.common.collect.ImmutableList;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class RorIndexingServiceTest {

    @Mock
    private SolrClient solrClient;

    @InjectMocks
    private RorIndexingService rorIndexingService;

    @Test
    void indexRorRecord() throws IOException, SolrServerException {
        //given
        String rorId = "testRor";
        String name = "testName";
        String countryName = "Poland";
        String countryCode = "PL";
        final ImmutableList<String> aliases = ImmutableList.of("alias");
        final ImmutableList<String> acronyms = ImmutableList.of("acronym");
        final ImmutableList<String> labels = ImmutableList.of("label");

        final RorDto rorData = new RorDto(rorId, name, countryName, countryCode, "","", aliases, acronyms, labels);

        //when
        rorIndexingService.indexRorRecord(rorData);

        //then
        Mockito.verify(solrClient, Mockito.times(1)).addBean(rorData);
        Mockito.verify(solrClient, Mockito.times(1)).commit();

    }
}