package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.search.SearchFields;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class OAIServiceTest {

    private OAISetServiceBean oaiSetServiceBean;
    private OAIRecordServiceBean oaiRecordServiceBean;

    @Mock
    private DatasetServiceBean datasetService;

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<OAIRecord> typedQuery;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        oaiSetServiceBean = new OAISetServiceBean();
        
        oaiRecordServiceBean = new OAIRecordServiceBean();
        // Since it's an EJB, we manually inject mocks
        oaiRecordServiceBean.datasetService = datasetService;
        oaiRecordServiceBean.em = em;

        when(em.createQuery(anyString(), eq(OAIRecord.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>());
    }

    @Test
    public void testOAISetQueryRestrictions() {
        String query = "authorName:king";
        String restrictedQuery = oaiSetServiceBean.addQueryRestrictions(query);
        
        assertTrue(restrictedQuery.contains("-" + SearchFields.LOCALLY_FAIR + ":true"), 
            "Query should contain Locally FAIR restriction: " + restrictedQuery);
    }

    @Test
    public void testOAIRecordFiltering() {
        // given
        Long normalDatasetId = 1L;
        Long locallyFairDatasetId = 2L;

        Dataset normalDataset = mock(Dataset.class);
        when(normalDataset.isReleased()).thenReturn(true);
        when(normalDataset.isDeaccessioned()).thenReturn(false);
        when(normalDataset.isLocallyFAIR()).thenReturn(false);

        Dataset locallyFairDataset = mock(Dataset.class);
        when(locallyFairDataset.isReleased()).thenReturn(true);
        when(locallyFairDataset.isDeaccessioned()).thenReturn(false);
        when(locallyFairDataset.isLocallyFAIR()).thenReturn(true);

        when(datasetService.find(normalDatasetId)).thenReturn(normalDataset);
        when(datasetService.find(locallyFairDatasetId)).thenReturn(locallyFairDataset);

        List<Long> datasetIds = Arrays.asList(normalDatasetId, locallyFairDatasetId);

        // OAIRecordServiceBean.updateOaiRecords calls updateOaiRecordForDataset which is in the same bean.
        // We want to verify that updateOaiRecordForDataset is called for the normal dataset but NOT for the locally fair one.
        // However, since it's the same object, we can't easily verify the call unless we spy it.
        OAIRecordServiceBean spyBean = spy(oaiRecordServiceBean);
        
        // when
        spyBean.updateOaiRecords("testSet", datasetIds, new Date(), false, true, Logger.getLogger("test"));

        // then
        verify(spyBean, times(1)).updateOaiRecordForDataset(eq(normalDataset), eq("testSet"), anyMap(), any(Logger.class));
        verify(spyBean, times(0)).updateOaiRecordForDataset(eq(locallyFairDataset), eq("testSet"), anyMap(), any(Logger.class));
    }
}
