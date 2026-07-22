package edu.harvard.iq.dataverse.util.testing.recipes;

/**
 * Context object supplied while deciding how many variables to create for a
 * tabular file or table.
 *
 * <p>At present this only carries the file index. It is intentionally separated
 * from {@link FileBuildContext} because variable population decisions may later
 * need different context, such as table index, dataset version information,
 * recipe seed, or file type details.</p>
 *
 * @param fileIndex zero-based index of the file for which variables are being created
 */
public record VariableSetBuildContext(
    FileRecipe.Tabular tabularRecipe,
    int fileIndex
) {
}
