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
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Indexing is requested from inside the transaction that changes a dataset or its
 * permissions. The
 * background job must not start before that transaction has committed, otherwise it
 * reads the database before the new rows are visible (missing permissions, missing
 * index time).
 */
public class IndexingAfterCommitTest {

    private IndexServiceBean indexService;
    private IndexServiceBean self;
    private IndexAsync indexAsync;
    private IndexAsync indexAsyncSelf;
    private Event<IndexingRequest> indexingRequests;
    private Dataset dataset;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        indexService = new IndexServiceBean();
        self = Mockito.mock(IndexServiceBean.class);
        indexingRequests = Mockito.mock(Event.class);
        indexService.self = self;
        indexService.indexingRequests = indexingRequests;
        indexAsync = new IndexAsync();
        indexAsyncSelf = Mockito.mock(IndexAsync.class);
        indexAsync.self = indexAsyncSelf;
        indexAsync.indexingRequests = indexingRequests;
        dataset = new Dataset();
        dataset.setId(42L);
    }

    @Test
    void asyncIndexDatasetWaitsForTheTransactionToCommit() {
        indexService.asyncIndexDataset(dataset, true);

        verifyNoInteractions(self);
        firedRequest().run();
        verify(self).indexDatasetInBackground(dataset, true);
    }

    @Test
    void asyncIndexDatasetByIdWaitsForTheTransactionToCommit() {
        indexService.asyncIndexDataset(42L, false);

        verifyNoInteractions(self);
        firedRequest().run();
        verify(self).indexDatasetInBackground(42L, false);
    }

    @Test
    void asyncIndexDatasetListWaitsForTheTransactionToCommit() {
        List<Dataset> datasets = List.of(dataset);

        indexService.asyncIndexDatasetList(datasets, true);

        verifyNoInteractions(self);
        firedRequest().run();
        verify(self).indexDatasetListInBackground(datasets, true);
    }

    @Test
    void indexRoleWaitsForTheTransactionToCommit() {
        RoleAssignment roleAssignment = new RoleAssignment();

        indexAsync.indexRole(roleAssignment);

        verifyNoInteractions(indexAsyncSelf);
        firedRequest().run();
        verify(indexAsyncSelf).indexRoleInBackground(roleAssignment);
    }

    @Test
    void indexRolesWaitsForTheTransactionToCommit() {
        Collection<DvObject> dvObjects = List.of(dataset);

        indexAsync.indexRoles(dvObjects);

        verifyNoInteractions(indexAsyncSelf);
        firedRequest().run();
        verify(indexAsyncSelf).indexRolesInBackground(dvObjects);
    }

    @Test
    void observerRunsTheRequestOnlyAfterASuccessfulTransaction() throws Exception {
        Method observerMethod = IndexingRequestObserver.class.getMethod("afterCommit", IndexingRequest.class);
        Parameter request = observerMethod.getParameters()[0];
        Observes observes = request.getAnnotation(Observes.class);
        assertNotNull(observes, "the observer must observe IndexingRequest events");
        assertEquals(TransactionPhase.AFTER_SUCCESS, observes.during());

        AtomicInteger runs = new AtomicInteger();
        new IndexingRequestObserver().afterCommit(new IndexingRequest(runs::incrementAndGet));
        assertEquals(1, runs.get());
    }

    private IndexingRequest firedRequest() {
        ArgumentCaptor<IndexingRequest> captor = ArgumentCaptor.forClass(IndexingRequest.class);
        verify(indexingRequests).fire(captor.capture());
        return captor.getValue();
    }
}
