package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileMetadataOrderTest {

    private AtomicLong id;
    private List<FileMetadata> order;

    @Before
    public void setup() {
        id = new AtomicLong();
    }

    @Test
    public void shouldReturnEmptyChanges() {
        // given
        order = newArrayList();

        // when
        List<FileMetadata> changes = FileMetadataOrder.reorderDisplayOrder(order);

        // then
        verifyNoChanges(changes);
    }

    @Test
    public void shouldChangeDisplayOrdersByIndex() {
        // given
        order = newArrayList(
                makeFileMetadata("file1.png", 1),
                makeFileMetadata("file2.png", 1),
                makeFileMetadata("file3.png", 5));

        // when
        List<FileMetadata> changes = FileMetadataOrder.reorderDisplayOrder(order);

        // then
        verifyDisplayOrder(changes, "file1.png", 0);
        verifyDisplayOrder(changes, "file3.png", 2);

        // and
        verifyChangesSize(changes, 2);
    }

    @Test
    public void shouldCopyDisplayOrder() {
        // given
        FileMetadata fileMetadata = makeFileMetadata("file1.png", 7);

        // when
        FileMetadata fileMetadataCopy = fileMetadata.createCopy();

        // then
        assertEquals(7, fileMetadataCopy.getDisplayOrder());
    }

    private FileMetadata makeFileMetadata(String label, int displayOrder) {
        return MocksFactory.makeFileMetadata(id.incrementAndGet(), label, displayOrder);
    }

    private void verifyDisplayOrder(List<FileMetadata> changes, String label, int displayOrder) {
        FileMetadata fileMetadata = changes.stream()
                .filter(fm -> fm.getLabel().equals(label))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        assertEquals(displayOrder, fileMetadata.getDisplayOrder(),"Wrong display order for: " + label);
    }

    private void verifyChangesSize(List<FileMetadata> changes, int size) {
        assertEquals(size, changes.size());
    }

    private void verifyNoChanges(List<FileMetadata> changes) {
        assertTrue(changes.isEmpty());
    }
}