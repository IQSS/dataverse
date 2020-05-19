package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class MetadataFormLookupTest {

    Supplier<Map<MetadataBlock, List<DatasetFieldsByType>>> metadataSupplier =
            TestMetadataCreator::createTestMetadata;

    @Test
    public void shouldCreateProperParentLookup() {
        // given & when
        Map<String, DatasetFieldsByType> lookup =
                MetadataFormLookup.create(BLOCK_NAME, metadataSupplier).getLookup();

        // then
        Set<String> keyNames = lookup.keySet();
        assertThat(keyNames,
                containsInAnyOrder(PARENT + SIMPLE, PARENT + VOCABULARY,
                        PARENT + COMPOUND, PARENT + SECOND + COMPOUND));
    }

    @Test
    public void shouldCreateProperChildrenLookup() {
        // given & when
        Map<String, DatasetFieldType> childrenLookup =
                MetadataFormLookup.create(BLOCK_NAME, metadataSupplier).getChildrenLookup();

        // then
        Set<String> keyNames = childrenLookup.keySet();
        assertThat(keyNames,
                containsInAnyOrder(CHILD + SIMPLE, CHILD + VOCABULARY, CHILD + SIMPLE + OF + SECOND));
    }
}