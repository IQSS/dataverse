package edu.harvard.iq.dataverse.util.testing.recipes;

import java.util.Objects;

/**
 * Top-level recipe describing how to construct a {@code Dataset} fixture.
 *
 * <p>This is intentionally rooted at the dataset level rather than the dataset
 * version level, so the fixture system can later support scenarios involving
 * multiple versions, different current-version shapes, and dataset-level
 * performance tests.</p>
 *
 * <p>For the initial implementation, a dataset recipe exposes exactly one
 * "current version" recipe. This keeps the model simple while leaving room
 * to evolve later.</p>
 */
public interface DatasetRecipe {
    
    /**
     * Returns the dataset type recipe providing the type to assign.
     *
     * @return dataset type recipe
     */
    DatasetTypeRecipe datasetTypeRecipe();
    
    /**
     * Returns the recipe describing the current version of the dataset.
     *
     * @return recipe for the current dataset version
     */
    VersionRecipe currentVersionRecipe();
    
    /**
     * Creates a dataset recipe with the supplied type and version recipes.
     *
     * @param datasetTypeRecipe recipe providing the dataset type
     * @param currentVersionRecipe recipe for the current dataset version
     * @return a dataset recipe
     */
    static DatasetRecipe of(DatasetTypeRecipe datasetTypeRecipe, VersionRecipe currentVersionRecipe) {
        Objects.requireNonNull(datasetTypeRecipe, "datasetTypeRecipe must not be null");
        Objects.requireNonNull(currentVersionRecipe, "currentVersionRecipe must not be null");
        return new SimpleDatasetRecipe(datasetTypeRecipe, currentVersionRecipe);
    }
    
    /**
     * Minimal immutable implementation of {@link DatasetRecipe}.
     *
     * @param datasetTypeRecipe recipe providing the dataset type
     * @param currentVersionRecipe recipe for the current dataset version
     */
    record SimpleDatasetRecipe(
        DatasetTypeRecipe datasetTypeRecipe,
        VersionRecipe currentVersionRecipe
    ) implements DatasetRecipe {
    }
}
