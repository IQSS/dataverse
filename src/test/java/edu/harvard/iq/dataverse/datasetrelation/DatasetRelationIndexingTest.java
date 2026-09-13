package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vera Clemens (ZB MED)
 */
public class DatasetRelationIndexingTest {

    @Test
    void schedulesDefiningDatasetAndChangedInternalRelatedDatasetsForReindexing() {
        Dataset definingDataset = dataset(1L);
        Dataset removedRelatedDataset = dataset(2L);
        Dataset addedRelatedDataset = dataset(3L);
        CommandContext ctxt = mock(CommandContext.class);
        IndexServiceBean index = mock(IndexServiceBean.class);
        when(ctxt.index()).thenReturn(index);

        DatasetRelationIndexing.scheduleChanges(ctxt, definingDataset,
                List.of(internalRelation(removedRelatedDataset)),
                List.of(internalRelation(addedRelatedDataset)));

        ArgumentCaptor<List<Dataset>> datasets = ArgumentCaptor.forClass(List.class);
        verify(index).asyncIndexDatasetList(datasets.capture(), eq(true));
        assertEquals(List.of(removedRelatedDataset, addedRelatedDataset, definingDataset), datasets.getValue());
    }

    @Test
    void doesNotScheduleUnchangedInternalRelatedDatasetsForReindexing() {
        Dataset definingDataset = dataset(1L);
        Dataset relatedDataset = dataset(2L);
        CommandContext ctxt = mock(CommandContext.class);
        IndexServiceBean index = mock(IndexServiceBean.class);
        when(ctxt.index()).thenReturn(index);

        DatasetRelationIndexing.scheduleChanges(ctxt, definingDataset,
                List.of(internalRelation(relatedDataset)),
                List.of(internalRelation(relatedDataset)));

        ArgumentCaptor<List<Dataset>> datasets = ArgumentCaptor.forClass(List.class);
        verify(index).asyncIndexDatasetList(datasets.capture(), eq(true));
        assertEquals(List.of(definingDataset), datasets.getValue());
    }

    @Test
    void schedulesRelatedDatasetWhenItsRelationCountChanges() {
        Dataset definingDataset = dataset(1L);
        Dataset relatedDataset = dataset(2L);
        CommandContext ctxt = mock(CommandContext.class);
        IndexServiceBean index = mock(IndexServiceBean.class);
        when(ctxt.index()).thenReturn(index);

        DatasetRelationIndexing.scheduleChanges(ctxt, definingDataset,
                List.of(internalRelation(relatedDataset)),
                List.of(internalRelation(relatedDataset), internalRelation(relatedDataset)));

        ArgumentCaptor<List<Dataset>> datasets = ArgumentCaptor.forClass(List.class);
        verify(index).asyncIndexDatasetList(datasets.capture(), eq(true));
        assertEquals(List.of(relatedDataset, definingDataset), datasets.getValue());
    }

    private Dataset dataset(Long id) {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getId()).thenReturn(id);
        return dataset;
    }

    private DatasetRelation internalRelation(Dataset relatedDataset) {
        InternalDatasetRelation relation = mock(InternalDatasetRelation.class);
        when(relation.getRelatedDataset()).thenReturn(relatedDataset);
        return relation;
    }
}
