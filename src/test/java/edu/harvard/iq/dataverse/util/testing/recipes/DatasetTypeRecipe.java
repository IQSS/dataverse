package edu.harvard.iq.dataverse.util.testing.recipes;

import edu.harvard.iq.dataverse.dataset.DatasetType;

/**
 * Recipe providing the {@link DatasetType} to assign to a generated dataset fixture.
 *
 * <p>Unlike structural recipes such as {@link VersionRecipe} or {@link FileRecipe},
 * this is not a construction recipe. It is a reference/creation provider — the
 * dataset type it produces is expected to be persisted before the dataset fixture
 * is committed to the database.</p>
 *
 * <p>Two factory styles are available:</p>
 * <ul>
 *   <li>{@link #of(String, String, String)} — fluent factory that creates a new
 *       {@link DatasetType} from scalar values. Use this when generating a single
 *       dataset fixture and you want a self-contained recipe.</li>
 *   <li>{@link #of(DatasetType)} — wraps a pre-existing instance. Use this when
 *       you want to share the same type across multiple dataset recipes, or when
 *       the type has already been persisted elsewhere.</li>
 * </ul>
 */
public interface DatasetTypeRecipe {
    
    /**
     * Returns the dataset type to assign to the generated dataset.
     *
     * <p>The returned instance may be newly created or pre-existing, depending on
     * the implementation. Either way, it must be persisted before the dataset
     * fixture is committed to the database.</p>
     *
     * @return dataset type instance
     */
    DatasetType datasetType();
    
    /**
     * Creates a recipe that builds a new {@link DatasetType} from the supplied
     * scalar values.
     *
     * <p>This is the preferred factory for single-dataset fixture scenarios where
     * the type does not need to be reused or pre-built externally. The resulting
     * type will need to be persisted before the dataset is committed.</p>
     *
     * @param name machine-readable name used in APIs and stored in the database
     * @param displayName human-readable name shown in the UI
     * @param description optional description of the dataset type
     * @return a dataset type recipe producing a new type from the supplied values
     */
    static DatasetTypeRecipe of(String name, String displayName, String description) {
        DatasetType datasetType = new DatasetType();
        datasetType.setName(name);
        datasetType.setDisplayName(displayName);
        datasetType.setDescription(description);
        return new FixedDatasetTypeRecipe(datasetType);
    }
    
    /**
     * Creates a recipe that wraps a pre-existing {@link DatasetType} instance.
     *
     * <p>Use this when the type has already been persisted, or when you want to
     * share the same type instance across multiple dataset recipes.</p>
     *
     * @param datasetType pre-existing dataset type to use
     * @return a dataset type recipe wrapping the supplied instance
     */
    static DatasetTypeRecipe of(DatasetType datasetType) {
        return new FixedDatasetTypeRecipe(datasetType);
    }
    
    /**
     * Creates a recipe using the standard {@value DatasetType#DATASET_TYPE_DATASET}
     * dataset type with sensible display defaults.
     *
     * <p>This is a convenience shortcut for the most common fixture scenario,
     * where you just need a valid persisted type and do not care about specific
     * type semantics.</p>
     *
     * @return a dataset type recipe for the default dataset type
     */
    static DatasetTypeRecipe dataset() {
        return of(DatasetType.DATASET_TYPE_DATASET, "Dataset", "Standard dataset type for fixtures");
    }
    
    /**
     * Minimal immutable recipe holding a fixed dataset type instance.
     *
     * @param datasetType dataset type to return
     */
    record FixedDatasetTypeRecipe(
        DatasetType datasetType
    ) implements DatasetTypeRecipe {
    }
}