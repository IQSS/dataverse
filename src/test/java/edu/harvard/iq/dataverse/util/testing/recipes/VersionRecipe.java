package edu.harvard.iq.dataverse.util.testing.recipes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Recipe describing how to construct a dataset version fixture.
 *
 * <p>At this stage, a version recipe is mainly responsible for delegating to a
 * {@link FileRecipe}, which controls how files in that version are created.</p>
 *
 * <p>Later, this type can be extended with more version-level concerns such as:
 * draft/released state, timestamps, version numbering, or version-specific
 * metadata enrichment.</p>
 */
public interface VersionRecipe {

    /**
     * Returns the file recipes for this dataset version.
     *
     * @return recipes governing file creation for the version
     */
    List<FileRecipe> fileRecipes();

    /**
     * Creates a version recipe from a number of file recipes.
     *
     * @param fileRecipes recipes governing file creation
     * @return a version recipe
     */
    static VersionRecipe of(FileRecipe... fileRecipes) {
        Objects.requireNonNull(fileRecipes, "fileRecipes may not be null");
        for (FileRecipe fileRecipe : fileRecipes) {
            Objects.requireNonNull(fileRecipe, "fileRecipes must not contain null elements");
        }
        return new SimpleVersionRecipe(Arrays.asList(fileRecipes));
    }

    /**
     * Minimal immutable implementation of {@link VersionRecipe}.
     *
     * @param fileRecipe recipe governing file creation
     */
    record SimpleVersionRecipe(
            List<FileRecipe> fileRecipes
    ) implements VersionRecipe {
    }
}
