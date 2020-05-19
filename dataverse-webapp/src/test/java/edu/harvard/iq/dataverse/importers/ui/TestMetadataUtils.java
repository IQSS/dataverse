package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItem;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItemsCreator;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.BLOCK_NAME;

public class TestMetadataUtils {

    public static List<ResultItem> createItems(List<ResultField> fields) {
        return new ResultItemsCreator(MetadataFormLookup.create(BLOCK_NAME, TestMetadataCreator::createTestMetadata))
                .createItemsForView(fields);
    }

    public static <T, V> List<T> extract(List<V> items, Function<V, T> valueSupplier) {
        return items.stream()
                .map(valueSupplier::apply)
                .collect(Collectors.toList());
    }
}
