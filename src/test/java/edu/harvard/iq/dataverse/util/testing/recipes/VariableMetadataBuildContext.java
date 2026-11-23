package edu.harvard.iq.dataverse.util.testing.recipes;

/**
 * Context object supplied while deciding whether a variable should receive
 * {@link edu.harvard.iq.dataverse.datavariable.VariableMetadata}.
 *
 * <p>A variable metadata entry belongs to a specific pair of:</p>
 * <ul>
 *   <li>a file's metadata</li>
 *   <li>a variable in that file's tabular structure</li>
 * </ul>
 *
 * <p>For now this context only exposes file and variable indices. It can grow
 * later as fixture requirements become more sophisticated.</p>
 *
 * @param fileIndex zero-based file index
 * @param variableIndex zero-based variable index within the file/table
 */
public record VariableMetadataBuildContext(
    FileRecipe.Tabular tabularRecipe,
    int fileIndex,
    int variableIndex
) {
}
