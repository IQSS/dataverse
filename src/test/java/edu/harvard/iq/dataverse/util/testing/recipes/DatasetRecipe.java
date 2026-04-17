package edu.harvard.iq.dataverse.util.testing.recipes;

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
     * Returns the recipe describing the current version of the dataset.
     *
     * @return recipe for the current dataset version
     */
    VersionRecipe currentVersionRecipe();

    /**
     * Creates a dataset recipe with a single current version recipe.
     *
     * @param currentVersionRecipe the recipe for the current dataset version
     * @return a dataset recipe
     */
    static DatasetRecipe of(VersionRecipe currentVersionRecipe) {
        return new SimpleDatasetRecipe(currentVersionRecipe);
    }

    /**
     * Minimal immutable implementation of {@link DatasetRecipe}.
     *
     * @param currentVersionRecipe the recipe for the current dataset version
     */
    record SimpleDatasetRecipe(
            VersionRecipe currentVersionRecipe
    ) implements DatasetRecipe {
    }
}
