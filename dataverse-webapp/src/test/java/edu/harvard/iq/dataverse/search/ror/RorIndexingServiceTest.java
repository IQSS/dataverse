package edu.harvard.iq.dataverse.search.ror;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.ror.RorConverter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@ExtendWith(MockitoExtension.class)
class RorIndexingServiceTest {

    @Mock
    private SolrClient solrClient;

    @Mock
    private RorConverter rorConverter;

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

    @Test
    void indexRorRecordsAsync() throws IOException, SolrServerException, ExecutionException, InterruptedException {
        //given

        final RorData rorData = new RorData();
        final RorData rorDataSecond = new RorData();

        //when
        Mockito.when(rorConverter.toSolrDto(Mockito.any())).thenReturn(new RorDto());

        rorIndexingService.indexRorRecordsAsync(Lists.newArrayList(rorData, rorDataSecond)).get();

        //then
        Mockito.verify(solrClient, Mockito.times(1)).addBeans(Mockito.anyCollection());
        Mockito.verify(solrClient, Mockito.times(1)).commit();
        Mockito.verify(rorConverter, Mockito.times(2)).toSolrDto(Mockito.any(RorData.class));

    }
}