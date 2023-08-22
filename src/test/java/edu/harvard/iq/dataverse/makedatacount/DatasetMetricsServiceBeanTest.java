package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ejb.EJBException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatasetMetricsServiceBeanTest {

    private Query query;
    private Dataset dataset;
    private DatasetMetricsServiceBean serviceBean;

    @BeforeEach
    void setup() {
        serviceBean = new DatasetMetricsServiceBean();
        serviceBean.em = mock(EntityManager.class);

        dataset = mock(Dataset.class);
        when(dataset.getId()).thenReturn(1L);

        query = mock(Query.class);
    }

    @Test
    void testGetDatasetMetricsByDatasetMonthCountry_withoutResults() {
        when(query.getResultList()).thenReturn(new ArrayList());
        when(this.serviceBean.em.createQuery(ArgumentMatchers.anyString())).thenReturn(query);

        assertNull(serviceBean.getDatasetMetricsByDatasetMonthCountry(dataset, "01-01", "CH"));
    }

    @Test
    void testGetDatasetMetricsByDatasetMonthCountry_throwsForMultipleResults() {
        when(query.getResultList()).thenReturn(Arrays.asList(1, 2));
        when(this.serviceBean.em.createQuery(ArgumentMatchers.anyString())).thenReturn(query);

        assertThrows(EJBException.class, () -> {
            serviceBean.getDatasetMetricsByDatasetMonthCountry(dataset, "01-01", "CH");
        });
    }

    @Test
    void testGetDatasetMetricsByDatasetMonthCountry_aggregatesForSingleResult() {
        DatasetMetrics datasetMetrics = new DatasetMetrics();
        datasetMetrics.initCounts();
        datasetMetrics.setViewsTotalRegular(1L);
        datasetMetrics.setViewsTotalMachine(2L);
        datasetMetrics.setViewsUniqueRegular(3L);
        datasetMetrics.setViewsUniqueMachine(4L);
        datasetMetrics.setDownloadsTotalRegular(5L);
        datasetMetrics.setDownloadsTotalMachine(6L);
        datasetMetrics.setDownloadsUniqueRegular(7L);
        datasetMetrics.setDownloadsUniqueMachine(8L);

        when(query.getResultList()).thenReturn(Arrays.asList(datasetMetrics));
        when(this.serviceBean.em.createQuery(ArgumentMatchers.anyString())).thenReturn(query);

        DatasetMetrics result = serviceBean.getDatasetMetricsByDatasetMonthCountry(dataset, "04.2019", "CH");

        assertEquals(3L, (long) result.getViewsTotal(), "total views");
        assertEquals(7L, (long) result.getViewsUnique(), "unique views");
        assertEquals(11L, (long) result.getDownloadsTotal(), "total downloads");
        assertEquals(15L, (long) result.getDownloadsUnique(), "unique downloads");
    }
}
