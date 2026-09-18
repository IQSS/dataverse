package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Indexing is requested from inside the transaction that changes a dataset or its
 * permissions. The background job must not start before that transaction has
 * committed, otherwise it reads the database before the new rows are visible
 * (missing permissions, missing index time). The beans therefore only fire an
 * {@link IndexingRequest}; the observer starts the work after the commit.
 */
class IndexingAfterCommitTest {

    private IndexServiceBean indexService;
    private IndexAsync indexAsync;
    private Event<IndexingRequest> indexingRequests;
    private IndexServiceBean backgroundIndexService;
    private IndexAsync backgroundIndexAsync;
    private IndexingRequestObserver observer;
    private Dataset dataset;
    private RoleAssignment roleAssignment;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        indexingRequests = mock(Event.class);
        indexService = new IndexServiceBean();
        indexService.indexingRequests = indexingRequests;
        indexAsync = new IndexAsync();
        indexAsync.indexingRequests = indexingRequests;
        backgroundIndexService = mock(IndexServiceBean.class);
        backgroundIndexAsync = mock(IndexAsync.class);
        observer = new IndexingRequestObserver(backgroundIndexService, backgroundIndexAsync);
        dataset = new Dataset();
        dataset.setId(42L);
        roleAssignment = new RoleAssignment();
    }

    @Test
    void asyncIndexDatasetOnlyRequestsTheIndexing() {
        indexService.asyncIndexDataset(dataset, true);

        assertEquals(new IndexingRequest.IndexDataset(dataset, true), firedRequest());
    }

    @Test
    void asyncIndexDatasetByIdOnlyRequestsTheIndexing() {
        indexService.asyncIndexDataset(42L, false);

        assertEquals(new IndexingRequest.IndexDatasetById(42L, false), firedRequest());
    }

    @Test
    void asyncIndexDatasetListOnlyRequestsTheIndexing() {
        List<Dataset> datasets = List.of(dataset);

        indexService.asyncIndexDatasetList(datasets, true);

        assertEquals(new IndexingRequest.IndexDatasets(datasets, true), firedRequest());
    }

    @Test
    void indexRoleOnlyRequestsTheIndexing() {
        indexAsync.indexRole(roleAssignment);

        assertEquals(new IndexingRequest.IndexRole(roleAssignment), firedRequest());
    }

    @Test
    void indexRolesOnlyRequestsTheIndexing() {
        Collection<DvObject> dvObjects = List.of(dataset);

        indexAsync.indexRoles(dvObjects);

        assertEquals(new IndexingRequest.IndexRoles(dvObjects), firedRequest());
    }

    @Test
    void observerStartsIndexingADataset() {
        observer.afterCommit(new IndexingRequest.IndexDataset(dataset, true));

        verify(backgroundIndexService).indexDatasetInBackground(dataset, true);
    }

    @Test
    void observerStartsIndexingADatasetById() {
        observer.afterCommit(new IndexingRequest.IndexDatasetById(42L, false));

        verify(backgroundIndexService).indexDatasetInBackground(42L, false);
    }

    @Test
    void observerStartsIndexingADatasetList() {
        List<Dataset> datasets = List.of(dataset);

        observer.afterCommit(new IndexingRequest.IndexDatasets(datasets, true));

        verify(backgroundIndexService).indexDatasetListInBackground(datasets, true);
    }

    @Test
    void observerRecordsTheIndexTime() {
        observer.afterCommit(new IndexingRequest.RecordIndexTime(42L));

        verify(backgroundIndexService).updateLastIndexedTime(42L);
    }

    @Test
    void observerReindexesTheRolePermissions() {
        observer.afterCommit(new IndexingRequest.IndexRole(roleAssignment));

        verify(backgroundIndexAsync).indexRoleInBackground(roleAssignment);
    }

    @Test
    void observerReindexesSeveralPermissions() {
        Collection<DvObject> dvObjects = List.of(dataset);

        observer.afterCommit(new IndexingRequest.IndexRoles(dvObjects));

        verify(backgroundIndexAsync).indexRolesInBackground(dvObjects);
    }

    @Test
    void observerRunsOnlyAfterASuccessfulTransaction() throws Exception {
        Method observerMethod = IndexingRequestObserver.class.getMethod("afterCommit", IndexingRequest.class);
        Parameter request = observerMethod.getParameters()[0];
        Observes observes = request.getAnnotation(Observes.class);
        assertNotNull(observes, "the observer must observe IndexingRequest events");
        assertEquals(TransactionPhase.AFTER_SUCCESS, observes.during());
    }

    private IndexingRequest firedRequest() {
        ArgumentCaptor<IndexingRequest> captor = ArgumentCaptor.forClass(IndexingRequest.class);
        verify(indexingRequests).fire(captor.capture());
        return captor.getValue();
    }
}
