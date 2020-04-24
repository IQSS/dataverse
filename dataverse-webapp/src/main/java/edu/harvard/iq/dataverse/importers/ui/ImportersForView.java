package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.importer.metadata.ImporterConstants;
import edu.harvard.iq.dataverse.importer.metadata.MetadataImporter;
import edu.harvard.iq.dataverse.importer.metadata.SafeBundleWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportersForView implements MapForView<MetadataImporter, ImportersForView.ImporterItem> {
    private Map<MetadataImporter, ImporterItem> items = new HashMap<>();

    // -------------------- GETTERS --------------------

    public List<MetadataImporter> getImportersView() {
        return new ArrayList<>(items.keySet()); // TODO: Implement some kind of sorting later
    }

    @Override
    public Map<MetadataImporter, ImporterItem> getUnderlyingMap() {
        return items;
    }

    // -------------------- LOGIC --------------------

    public static ImportersForView createInitialized(Dataset dataset,
                                                     Map<String, MetadataImporter> importers, Locale locale) {
        ImportersForView instance = new ImportersForView();
        instance.initializeItems(dataset, importers, locale);
        return instance;
    }

    public void initializeItems(Dataset dataset, Map<String, MetadataImporter> importers, Locale locale) {
        if (dataset == null || locale == null) {
            throw new IllegalStateException("Null dataset and/or locale received");
        }

        Set<String> metadataBlockNames = collectMetadataBlockNamesForCurrentDataset(dataset);

        this.items = importers.entrySet().stream()
                .filter(e -> metadataBlockNames.contains(e.getValue().getMetadataBlockName()))
                .collect(Collectors.toMap(Map.Entry::getValue, e -> new ImporterItem(e, locale)));
    }

    // -------------------- PRIVATE --------------------

    private Set<String> collectMetadataBlockNamesForCurrentDataset(Dataset dataset) {
        Dataverse owner = dataset.getOwner();
        return owner.getRootMetadataBlocks().stream()
                .map(MetadataBlock::getName)
                .collect(Collectors.toSet());
    }

    // -------------------- INNER CLASSES --------------------

    public static class ImporterItem {
        private String name;
        private String description;

        // -------------------- CONSTRUCTORS --------------------

        public ImporterItem(Map.Entry<String, MetadataImporter> importer, Locale locale) {
            SafeBundleWrapper bundle = new SafeBundleWrapper(importer.getValue(), locale);
            this.name = bundle.getString(ImporterConstants.IMPORTER_NAME);
            this.description = bundle.getString(ImporterConstants.IMPORTER_DESCRIPTION);
        }

        // -------------------- GETTERS --------------------

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
