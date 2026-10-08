package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Indexing is requested from inside the transaction that changes a dataset or its
 * permissions. The background job must not start before that transaction has
 * committed, otherwise it reads the database before the new rows are visible
 * (missing permissions, missing index time). The beans therefore only fire an
 * {@link IndexingRequest}; the observer starts the work after the commit. The
 * requests carry ids: the background job must not share entities with the
 * requesting thread. Batch reindexes are the exception: they index one dataset
 * at a time, right away, as there is no transaction to wait for.
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
        roleAssignment.setDefinitionPoint(dataset);
    }

    @Test
    void asyncIndexDatasetOnlyRequestsTheIndexing() {
        indexService.asyncIndexDataset(dataset, true);

        assertEquals(new IndexingRequest.IndexDataset(42L, true), firedRequest());
    }

    @Test
    void asyncIndexDatasetByIdOnlyRequestsTheIndexing() {
        indexService.asyncIndexDataset(42L, false);

        assertEquals(new IndexingRequest.IndexDataset(42L, false), firedRequest());
    }

    @Test
    void asyncIndexDatasetRejectsADatasetWithoutId() {
        Dataset unsaved = new Dataset();

        assertThrows(NullPointerException.class, () -> indexService.asyncIndexDataset(unsaved, true));

        verifyNoInteractions(indexingRequests);
    }

    @Test
    void asyncIndexDatasetListOnlyRequestsTheIndexing() {
        indexService.asyncIndexDatasetList(List.of(dataset), true);

        assertEquals(new IndexingRequest.IndexDatasets(List.of(42L), true), firedRequest());
    }

    @Test
    void indexRoleOnlyRequestsThePermissionIndexing() {
        indexAsync.indexRole(roleAssignment);

        assertEquals(new IndexingRequest.IndexPermissions(List.of(42L)), firedRequest());
    }

    @Test
    void indexRolesOnlyRequestsThePermissionIndexing() {
        Collection<DvObject> dvObjects = List.of(dataset);

        indexAsync.indexRoles(dvObjects);

        assertEquals(new IndexingRequest.IndexPermissions(List.of(42L)), firedRequest());
    }

    @Test
    void batchReindexIndexesTheDatasetRightAway() throws Exception {
        mockIndexing();

        indexService.indexDatasetInNewTransaction(42L);

        verify(indexService.self).indexDatasetNow(42L, false);
        verifyNoInteractions(indexingRequests);
    }

    @Test
    void backgroundJobIndexesTheDataset() throws Exception {
        mockIndexing();

        indexService.indexDatasetInBackground(42L, true);

        verify(indexService.self).indexDatasetNow(42L, true);
        verifyNoInteractions(indexingRequests);
    }

    @Test
    void observerStartsIndexingADataset() {
        observer.afterCommit(new IndexingRequest.IndexDataset(42L, true));

        verify(backgroundIndexService).indexDatasetInBackground(42L, true);
    }

    @Test
    void observerStartsIndexingADatasetList() {
        observer.afterCommit(new IndexingRequest.IndexDatasets(List.of(42L), true));

        verify(backgroundIndexService).indexDatasetListInBackground(List.of(42L), true);
    }

    @Test
    void observerRecordsTheIndexTime() {
        observer.afterCommit(new IndexingRequest.RecordIndexTime(42L));

        verify(backgroundIndexService).updateLastIndexedTime(42L);
    }

    @Test
    void observerReindexesThePermissions() {
        observer.afterCommit(new IndexingRequest.IndexPermissions(List.of(42L)));

        verify(backgroundIndexAsync).indexPermissionsInBackground(List.of(42L));
    }

    @Test
    void observerRunsOnlyAfterASuccessfulTransaction() throws Exception {
        Method observerMethod = IndexingRequestObserver.class.getMethod("afterCommit", IndexingRequest.class);
        Parameter request = observerMethod.getParameters()[0];
        Observes observes = request.getAnnotation(Observes.class);
        assertNotNull(observes, "the observer must observe IndexingRequest events");
        assertEquals(TransactionPhase.AFTER_SUCCESS, observes.during());
    }

    @Test
    void backgroundJobDoesNotFlushBeforeQueries() throws Exception {
        EntityManager em = mock(EntityManager.class);
        Field emField = IndexServiceBean.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(indexService, em);
        indexService.datasetService = mock(DatasetServiceBean.class);

        indexService.indexDatasetNow(42L, false);

        InOrder inOrder = inOrder(em, indexService.datasetService);
        inOrder.verify(em).setFlushMode(FlushModeType.COMMIT);
        inOrder.verify(indexService.datasetService).find(42L);
    }

    private void mockIndexing() {
        indexService.self = mock(IndexServiceBean.class);
        indexService.indexPermitWaitTimer = noOpTimer();
        indexService.indexTimer = noOpTimer();
    }

    private static Timer noOpTimer() {
        Timer timer = mock(Timer.class);
        when(timer.time()).thenReturn(mock(Timer.Context.class));
        return timer;
    }

    private IndexingRequest firedRequest() {
        ArgumentCaptor<IndexingRequest> captor = ArgumentCaptor.forClass(IndexingRequest.class);
        verify(indexingRequests).fire(captor.capture());
        return captor.getValue();
    }
}
