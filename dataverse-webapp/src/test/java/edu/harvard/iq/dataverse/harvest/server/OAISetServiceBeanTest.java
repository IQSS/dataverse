package edu.harvard.iq.dataverse.harvest.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecordRepository;
import edu.harvard.iq.dataverse.persistence.harvest.OAISet;
import edu.harvard.iq.dataverse.persistence.harvest.OAISetRepository;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAISetServiceBeanTest {

    private static final String SPEC_NAME = "spec";

    @InjectMocks
    private OAISetServiceBean oaiSetService;

    @Mock
    private OAISetRepository oaiSetRepository;

    @Mock
    private OAIRecordRepository oaiRecordRepository;

    @Mock
    private SolrClient solrServer;

    @Mock
    private SolrQuerySanitizer querySanitizer;

    @Captor
    private ArgumentCaptor<SolrQuery> solrQueryCaptor;
    
    private OAISet oaiSet = new OAISet();
    private OAISet oaiSet2 = new OAISet();


    @BeforeEach
    void beforeEach() {
        oaiSet.setId(3L);
        oaiSet.setSpec(SPEC_NAME);

        oaiSet2.setId(4L);
    }

    @Test
    void specExists_true() {
        // given
        when(oaiSetRepository.findBySpecName(SPEC_NAME)).thenReturn(Optional.of(oaiSet));
        // when & then
        assertThat(oaiSetService.specExists(SPEC_NAME)).isTrue();
    }

    @Test
    void specExists_false() {
        // given
        when(oaiSetRepository.findBySpecName(SPEC_NAME)).thenReturn(Optional.empty());
        // when & then
        assertThat(oaiSetService.specExists(SPEC_NAME)).isFalse();
    }

    @Test
    void findBySpec() {
        // given
        when(oaiSetRepository.findBySpecName(SPEC_NAME)).thenReturn(Optional.of(oaiSet));
        // when & then
        assertThat(oaiSetService.findBySpec(SPEC_NAME)).isEqualTo(oaiSet);
    }

    @Test
    void findDefaultSet() {
        // given
        when(oaiSetRepository.findBySpecName("")).thenReturn(Optional.of(oaiSet));
        // when & then
        assertThat(oaiSetService.findDefaultSet()).isEqualTo(oaiSet);
    }

    @Test
    void findAll() {
        // given
        when(oaiSetRepository.findAll()).thenReturn(Lists.newArrayList(oaiSet, oaiSet2));
        // when & then
        assertThat(oaiSetService.findAll()).containsExactlyInAnyOrder(oaiSet, oaiSet2);
    }

    @Test
    void findAllNamedSets() {
        // given
        when(oaiSetRepository.findAllBySpecNameNot("")).thenReturn(Lists.newArrayList(oaiSet, oaiSet2));
        // when & then
        assertThat(oaiSetService.findAllNamedSets()).containsExactlyInAnyOrder(oaiSet, oaiSet2);
    }

    @Test
    void remove() {
        // given
        when(oaiSetRepository.findById(oaiSet.getId())).thenReturn(Optional.of(oaiSet));
        // when
        oaiSetService.remove(oaiSet.getId());
        // then
        verify(oaiRecordRepository).deleteBySetName(SPEC_NAME);
        verify(oaiSetRepository).delete(oaiSet);
    }

    @Test
    void expandSetQuery() throws OaiSetException, SolrServerException, IOException {
        // given
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrDocumentList solrDocuments = new SolrDocumentList();
        solrDocuments.add(new SolrDocument(ImmutableMap.of("entityId", 100L)));
        solrDocuments.add(new SolrDocument(ImmutableMap.of("entityId", 101L)));
        when(queryResponse.getResults()).thenReturn(solrDocuments);
        
        when(querySanitizer.sanitizeQuery("field:someQuery")).thenReturn("field:sanitizedQuery");
        when(solrServer.query(any())).thenReturn(queryResponse);
        
        // when
        List<Long> datasetIds = oaiSetService.expandSetQuery("field:someQuery");
        
        // then
        assertThat(datasetIds).containsExactly(100L, 101L);
        verify(solrServer).query(solrQueryCaptor.capture());
        
        SolrQuery querySentToSolr = solrQueryCaptor.getValue();
        assertThat(querySentToSolr.getQuery()).isEqualTo("field:sanitizedQuery");
        assertThat(querySentToSolr.getFilterQueries()).containsExactlyInAnyOrder(
                "dvObjectType:datasets",
                "isHarvested:false",
                "publicationStatus:Published");
    }

    @Test
    void setUpdateInProgress() {
        // given
        when(oaiSetRepository.findById(oaiSet.getId())).thenReturn(Optional.of(oaiSet));
        // when
        oaiSetService.setUpdateInProgress(oaiSet.getId());
        // then
        assertThat(oaiSet.isUpdateInProgress()).isTrue();
        verify(oaiSetRepository).save(oaiSet);
    }

    @Test
    void setDeleteInProgress() {
        // given
        when(oaiSetRepository.findById(oaiSet.getId())).thenReturn(Optional.of(oaiSet));
        // when
        oaiSetService.setDeleteInProgress(oaiSet.getId());
        // then
        assertThat(oaiSet.isDeleteInProgress()).isTrue();
        verify(oaiSetRepository).save(oaiSet);
    }

    @Test
    void save() {
        // when
        oaiSetService.save(oaiSet);
        // then
        verify(oaiSetRepository).save(oaiSet);
    }
}
