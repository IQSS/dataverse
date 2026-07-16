package edu.harvard.iq.dataverse.util.testing.recipes;

/**
 * Context object supplied while deciding how to build a file fixture.
 *
 * <p>For now this context only exposes the file index and the recipe which ordered the creation of the file.
 * It exists as a dedicated type, so the API can grow later without constantly changing method signatures.</p>
 *
 * @param fileIndex zero-based index of the file being created within a version
 */
public record FileBuildContext(
    FileRecipe fileRecipe,
    int fileIndex
) {
}
