package edu.harvard.iq.dataverse.util.testing.fixtures;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.util.testing.recipes.FileBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableMetadataBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetBuildContext;

/**
 * Populator interface responsible for initializing scalar/non-relationship fields of
 * generated fixture entities.
 *
 * <p>The builder/wiring layer is responsible for graph structure and
 * relationship correctness. This population layer is responsible for making sure
 * entities are also "safe enough" to serialize and persist by filling required
 * or null-sensitive scalar fields and collections.</p>
 *
 * <p>This separation keeps shape decisions in recipes and scalar defaults here.</p>
 */
public interface FixturePopulator {

    /**
     * Populates scalar fields and safe defaults for a dataset.
     *
     * @param dataset dataset being initialized
     * @param context fixture build context
     */
    void populateDataset(Dataset dataset, DatasetFixtureBuilder.BuildContext context);

    /**
     * Populates scalar fields and safe defaults for a dataset version.
     *
     * @param version dataset version being initialized
     * @param context fixture build context
     */
    void populateDatasetVersion(DatasetVersion version, DatasetFixtureBuilder.BuildContext context);

    /**
     * Populates scalar fields and safe defaults for file metadata.
     *
     * @param fileMetadata file metadata being initialized
     * @param fileIndex zero-based file index
     * @param context fixture build context
     */
    void populateFileMetadata(FileMetadata fileMetadata, FileBuildContext fileBuildContext, DatasetFixtureBuilder.BuildContext context);

    /**
     * Populates scalar fields and safe defaults for a data file.
     *
     * @param dataFile data file being initialized
     * @param fileIndex zero-based file index
     * @param context fixture build context
     */
    void populateDataFile(DataFile dataFile, FileBuildContext fileBuildContext, DatasetFixtureBuilder.BuildContext context);

    /**
     * Populates scalar fields and safe defaults for a data table.
     *
     * @param dataTable data table being initialized
     * @param fileIndex zero-based file index
     * @param context fixture build context
     */
    void populateDataTable(DataTable dataTable, FileBuildContext fileBuildContext, DatasetFixtureBuilder.BuildContext context);

    /**
     * Populates scalar fields and safe defaults for a data variable.
     *
     * @param dataVariable data variable being initialized
     * @param fileIndex zero-based file index
     * @param variableIndex zero-based variable index within the file/table
     * @param context fixture build context
     */
    void populateDataVariable(
            DataVariable dataVariable,
            VariableSetBuildContext variableBuildContext,
            int variableIndex,
            DatasetFixtureBuilder.BuildContext context
    );
    
    /**
     * Populates scalar fields and safe defaults for metadata of a variable.
     *
     * @param metadata variable metadata being initialized
     * @param variableMetadataBuildContext variable metadata build context
     */
    void populateVariableMetadata(
        VariableMetadata metadata,
        VariableMetadataBuildContext variableMetadataBuildContext
    );

    /**
     * Populates scalar fields and safe defaults for a variable group.
     *
     * @param varGroup var group being initialized
     * @param fileIndex zero-based file index
     * @param groupIndex zero-based group index within the file
     * @param context fixture build context
     */
    void populateVarGroup(
            VarGroup varGroup,
            FileBuildContext fileBuildContext,
            int groupIndex,
            DatasetFixtureBuilder.BuildContext context
    );

    /**
     * Returns a deterministic, minimal-safe entity populator.
     *
     * <p>This implementation is intentionally conservative. It sets enough fields
     * for fixture graphs to be usable in persistence and serialization tests,
     * without trying to simulate realistic production content yet.</p>
     *
     * @return standard, minimalized, and deterministic field populator
     */
    static FixturePopulator minimal() {
        return new MinimalPopulator();
    }
}
