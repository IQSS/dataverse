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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Indexing is requested from inside the transaction that changes a dataset or its
 * permissions. The background job must not start before that transaction has
 * committed, otherwise it reads the database before the new rows are visible
 * (missing permissions, missing index time). The beans therefore only fire an
 * {@link IndexingRequest}; the observer starts the work after the commit. The
 * requests carry ids: the background job must not share entities with the
 * requesting thread.
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

    private IndexingRequest firedRequest() {
        ArgumentCaptor<IndexingRequest> captor = ArgumentCaptor.forClass(IndexingRequest.class);
        verify(indexingRequests).fire(captor.capture());
        return captor.getValue();
    }
}
