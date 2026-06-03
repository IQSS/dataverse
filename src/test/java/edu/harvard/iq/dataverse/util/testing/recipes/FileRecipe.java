package edu.harvard.iq.dataverse.util.testing.recipes;

public interface FileRecipe {

    /**
     * Returns the total number of files to create.
     *
     * @return number of files in the generated dataset version
     */
    int fileCount();
    
    static FileRecipe tabular(int fileCount, VariableSetRecipe recipe) {
        return new Tabular(fileCount, recipe);
    }
    
    static FileRecipe regular(int fileCount) {
        return new Regular(fileCount);
    }
    
    record Tabular (
        int fileCount,
        VariableSetRecipe variableSetRecipe
    ) implements FileRecipe {}
    
    record Regular (
        int fileCount
    ) implements FileRecipe {}
}
