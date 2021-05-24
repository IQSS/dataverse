package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetCitationsCount;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetCitationsCountRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatasetCitationsCountUpdaterTest {

    @InjectMocks
    private DatasetCitationsCountUpdater citationsCountUpdater;

    @Mock
    private DatasetRepository datasetRepository;
    @Mock
    private DatasetCitationsCountRepository datasetCitationsCountRepository;
    @Mock
    private DataCiteRestApiClient dataCiteRestApiClient;
    @Captor
    private ArgumentCaptor<DatasetCitationsCount> citationsCountCaptor;

    // -------------------- TESTS --------------------

    @Test
    void updateCitationCount_citation_count_was_not_present() throws IOException {
        // given
        Dataset dataset = buildDataset(5L, Timestamp.from(Instant.now()),
                                       new GlobalId(GlobalId.DOI_PROTOCOL, "10.1020", "ABCD"));
        
        DataCiteFindDoiResponse dataCiteResponse = new DataCiteFindDoiResponse();
        dataCiteResponse.setCitationCount(3);
        
        when(datasetRepository.findIdsByNullHarvestedFrom()).thenReturn(Lists.newArrayList(5L));
        when(datasetRepository.getById(5L)).thenReturn(dataset);
        when(datasetCitationsCountRepository.findByDatasetId(5L)).thenReturn(Optional.empty());
        when(dataCiteRestApiClient.findDoi("10.1020", "ABCD")).thenReturn(dataCiteResponse);

        // when
        citationsCountUpdater.updateCitationCount();

        // then
        verify(datasetCitationsCountRepository).save(citationsCountCaptor.capture());
        assertThat(citationsCountCaptor.getValue())
            .extracting(DatasetCitationsCount::getDataset, DatasetCitationsCount::getCitationsCount)
            .containsExactly(dataset, 3);
    }
    
    @Test
    void updateCitationCount_citation_count_was_already_present() throws IOException {
        // given
        Dataset dataset = buildDataset(5L, Timestamp.from(Instant.now()),
                                       new GlobalId(GlobalId.DOI_PROTOCOL, "10.1020", "ABCD"));
        
        DatasetCitationsCount citationCount = new DatasetCitationsCount();
        citationCount.setDataset(dataset);
        citationCount.setCitationsCount(2);
        
        DataCiteFindDoiResponse dataCiteResponse = new DataCiteFindDoiResponse();
        dataCiteResponse.setCitationCount(3);
        
        when(datasetRepository.findIdsByNullHarvestedFrom()).thenReturn(Lists.newArrayList(5L));
        when(datasetRepository.getById(5L)).thenReturn(dataset);
        when(datasetCitationsCountRepository.findByDatasetId(5L)).thenReturn(Optional.of(citationCount));
        when(dataCiteRestApiClient.findDoi("10.1020", "ABCD")).thenReturn(dataCiteResponse);

        // when
        citationsCountUpdater.updateCitationCount();

        // then
        verify(datasetCitationsCountRepository).save(citationsCountCaptor.capture());
        assertThat(citationsCountCaptor.getValue())
            .isSameAs(citationCount)
            .extracting(DatasetCitationsCount::getDataset, DatasetCitationsCount::getCitationsCount)
            .containsExactly(dataset, 3);
    }
    
    @Test
    void updateCitationCount_dataset_with_non_doi_protocol() throws IOException {
        // given
        Dataset dataset = buildDataset(5L, Timestamp.from(Instant.now()),
                                       new GlobalId("hdl", "10.1020", "ABCD"));
        
        DataCiteFindDoiResponse dataCiteResponse = new DataCiteFindDoiResponse();
        dataCiteResponse.setCitationCount(3);
        
        when(datasetRepository.findIdsByNullHarvestedFrom()).thenReturn(Lists.newArrayList(5L));
        when(datasetRepository.getById(5L)).thenReturn(dataset);
        
        // when
        citationsCountUpdater.updateCitationCount();
        
        // then
        verifyZeroInteractions(datasetCitationsCountRepository);
    }
    
    @Test
    void updateCitationCount_non_released_dataset() throws IOException {
        // given
        Dataset dataset = buildDataset(5L, null,
                                       new GlobalId(GlobalId.DOI_PROTOCOL, "10.1020", "ABCD"));
        
        DataCiteFindDoiResponse dataCiteResponse = new DataCiteFindDoiResponse();
        dataCiteResponse.setCitationCount(3);
        
        when(datasetRepository.findIdsByNullHarvestedFrom()).thenReturn(Lists.newArrayList(5L));
        when(datasetRepository.getById(5L)).thenReturn(dataset);
        
        // when
        citationsCountUpdater.updateCitationCount();
        
        // then
        verifyZeroInteractions(datasetCitationsCountRepository);
    }
    
    @Test
    void updateCitationCount_datacite_error() throws IOException {
        // given
        Dataset dataset = buildDataset(5L, Timestamp.from(Instant.now()),
                                       new GlobalId(GlobalId.DOI_PROTOCOL, "10.1020", "ABCD"));
        
        when(datasetRepository.findIdsByNullHarvestedFrom()).thenReturn(Lists.newArrayList(5L));
        when(datasetRepository.getById(5L)).thenReturn(dataset);
        when(dataCiteRestApiClient.findDoi("10.1020", "ABCD")).thenThrow(new HttpResponseException(404, "Not found"));
        
        // when
        citationsCountUpdater.updateCitationCount();
        
        // then
        verifyZeroInteractions(datasetCitationsCountRepository);
    }

    // -------------------- PRIVATE --------------------

    private Dataset buildDataset(Long id, Timestamp publicationDate, GlobalId globalId) {
        Dataset dataset = new Dataset();
        dataset.setId(id);
        dataset.setPublicationDate(publicationDate);
        dataset.setProtocol(globalId.getProtocol());
        dataset.setAuthority(globalId.getAuthority());
        dataset.setIdentifier(globalId.getIdentifier());
        return dataset;
    }
    
    
}
