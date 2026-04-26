package edu.harvard.iq.dataverse.util.testing.fixtures;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableMetadataBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableMetadataRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builder/wiring layer that consumes fixture recipes and produces a fully wired
 * {@link Dataset} graph.
 *
 * <p>This class is intentionally responsible for relationship correctness and
 * collection initialization, while recipes are responsible for deciding graph
 * shape and populators are responsible for scalar-field initialization.</p>
 *
 * <p>Current scope:</p>
 * <ul>
 *   <li>one dataset</li>
 *   <li>one (current) dataset version</li>
 *   <li>files created according to {@link FileRecipe}</li>
 *   <li>tabular structure created according to {@link VariableSetRecipe}</li>
 *   <li>variable metadata created according to {@link VariableMetadataRecipe}</li>
 * </ul>
 */
public class DatasetFixtureBuilder {
    
    /**
     * Process-wide deterministic sequence used to identify each built fixture.
     *
     * <p>This is intentionally static so values are unique even across multiple
     * tests running in the same JVM. It is not meant to be reset between tests.</p>
     */
    private static final AtomicLong SEQUENCE = new AtomicLong(1);
    
    /**
     * Group index used for the single var group we currently create per tabular file.
     * <p>This will become recipe-driven once a {@code VarGroupRecipe} is introduced.</p>
     */
    private static final int FIRST_AND_ONLY_VAR_GROUP_INDEX = 0;
    
    private DatasetRecipe datasetRecipe;
    private FixturePopulator populator = FixturePopulator.minimal();
    
    /**
     * Creates a new builder instance.
     *
     * @return a fresh fixture builder
     */
    public static DatasetFixtureBuilder builder() {
        return new DatasetFixtureBuilder();
    }
    
    /**
     * Sets the recipe used to determine the graph shape.
     *
     * @param datasetRecipe dataset recipe to use
     * @return this builder for fluent chaining
     */
    public DatasetFixtureBuilder recipe(DatasetRecipe datasetRecipe) {
        this.datasetRecipe = Objects.requireNonNull(datasetRecipe);
        return this;
    }
    
    /**
     * Sets the scalar-field populator policy.
     *
     * @param populator populator to use
     * @return this builder for fluent chaining
     */
    public DatasetFixtureBuilder populator(FixturePopulator populator) {
        this.populator = Objects.requireNonNull(populator);
        return this;
    }
    
    /**
     * Builds a dataset fixture graph according to the configured recipe and populator.
     *
     * <p>The build process happens in clearly separated phases:</p>
     * <ol>
     *   <li>create the root {@link Dataset} and its current {@link DatasetVersion}</li>
     *   <li>iterate over the configured file recipes and build each file (and, where applicable,
     *       its tabular subgraph)</li>
     *   <li>collect everything that was created so the {@link DatasetFixture} can expose it</li>
     * </ol>
     *
     * @return generated dataset fixture
     */
    public DatasetFixture build() {
        if (datasetRecipe == null) {
            throw new IllegalStateException("A DatasetRecipe must be configured before building.");
        }
        Objects.requireNonNull(populator, "populator must not be null");
        
        // One context per build, so populators can use deterministic information about this fixture instance.
        BuildContext context = new BuildContext(SEQUENCE.getAndIncrement());
        
        // Create the top-level dataset and its current version, then wire them.
        Dataset dataset = createEmptyDataset(context);
        DatasetVersion currentVersion = createDatasetVersion(context);
        wireDatasetAndVersion(dataset, currentVersion);
        
        // Accumulator collects everything we generate so we can expose it in the fixture.
        BuildAccumulator accumulator = new BuildAccumulator();
        
        // Walk the file recipes and create files (plus tabular structure where applicable).
        buildVersionFiles(currentVersion, context, accumulator);
        
        return accumulator.toFixture(dataset, currentVersion);
    }
    
    /**
     * Creates a {@link Dataset} with no implicit versions.
     *
     * <p>{@code Dataset} normally creates an initial version automatically. For fixtures we want
     * full control over which versions exist, so we wipe that initial version before wiring.</p>
     *
     * @param context fixture build context
     * @return a freshly populated dataset with an empty version list
     */
    private Dataset createEmptyDataset(BuildContext context) {
        Dataset dataset = new Dataset();
        populator.populateDataset(dataset, context);
        dataset.setVersions(new ArrayList<>());
        return dataset;
    }
    
    /**
     * Creates a {@link DatasetVersion} populated by the configured populator.
     *
     * @param context fixture build context
     * @return a freshly populated dataset version
     */
    private DatasetVersion createDatasetVersion(BuildContext context) {
        DatasetVersion version = new DatasetVersion();
        populator.populateDatasetVersion(version, context);
        return version;
    }
    
    /**
     * Iterates over all file recipes for the current version and builds each file in order.
     *
     * <p>Each file gets a globally unique index across all file recipes in the version. That
     * keeps populator-generated values such as labels deterministic and unique across the
     * whole version.</p>
     *
     * @param currentVersion current dataset version receiving the files
     * @param context fixture build context
     * @param accumulator accumulator collecting all generated entities
     */
    private void buildVersionFiles(
        DatasetVersion currentVersion,
        BuildContext context,
        BuildAccumulator accumulator
    ) {
        VersionRecipe versionRecipe = datasetRecipe.currentVersionRecipe();
        List<FileRecipe> fileRecipes = versionRecipe.fileRecipes();
        
        // Files within a single version need globally unique indices, even though each recipe
        // describes its own count. We track that separately from the recipe-local index.
        int globalFileIndex = 0;
        
        for (FileRecipe fileRecipe : fileRecipes) {
            for (int fileIndex = 0; fileIndex < fileRecipe.fileCount(); fileIndex++, globalFileIndex++) {
                FileBuildContext fileContext = new FileBuildContext(fileRecipe, globalFileIndex);
                buildFile(currentVersion, fileRecipe, fileContext, context, accumulator);
            }
        }
    }
    
    /**
     * Builds a single file: a {@link DataFile} and its current {@link FileMetadata}, plus its
     * tabular subgraph if the recipe says the file is tabular.
     *
     * @param currentVersion owning dataset version
     * @param fileRecipe the file recipe describing this file
     * @param fileContext context describing this individual file
     * @param context fixture build context
     * @param accumulator accumulator collecting all generated entities
     */
    private void buildFile(
        DatasetVersion currentVersion,
        FileRecipe fileRecipe,
        FileBuildContext fileContext,
        BuildContext context,
        BuildAccumulator accumulator
    ) {
        // Always create the data file plus its current-version file metadata.
        DataFile dataFile = new DataFile();
        populator.populateDataFile(dataFile, fileContext, context);
        
        FileMetadata fileMetadata = new FileMetadata();
        populator.populateFileMetadata(fileMetadata, fileContext, context);
        
        wireFileMetadata(currentVersion, fileMetadata, dataFile);
        accumulator.addDataFile(dataFile);
        accumulator.addFileMetadata(fileMetadata);
        
        // Tabular structure is only created when the recipe says so.
        if (fileRecipe instanceof FileRecipe.Tabular tabularRecipe) {
            buildTabularStructure(tabularRecipe, fileContext, dataFile, fileMetadata, context, accumulator);
        }
    }
    
    /**
     * Builds the tabular structure for a file: one {@link DataTable}, its variables, the
     * variable metadata, and the var group.
     *
     * @param tabularRecipe tabular file recipe
     * @param fileContext file build context
     * @param dataFile owning data file
     * @param fileMetadata current-version file metadata of the data file
     * @param context fixture build context
     * @param accumulator accumulator collecting all generated entities
     */
    private void buildTabularStructure(
        FileRecipe.Tabular tabularRecipe,
        FileBuildContext fileContext,
        DataFile dataFile,
        FileMetadata fileMetadata,
        BuildContext context,
        BuildAccumulator accumulator
    ) {
        DataTable dataTable = new DataTable();
        populator.populateDataTable(dataTable, fileContext, context);
        wireDataTable(dataFile, dataTable);
        accumulator.addDataTable(dataTable);
        
        VariableSetBuildContext variableSetContext =
            new VariableSetBuildContext(tabularRecipe, fileContext.fileIndex());
        
        VariableSetRecipe variableSetRecipe = tabularRecipe.variableSetRecipe();
        int variableCount = variableSetRecipe.variableCount(variableSetContext);
        
        // Build all variables for this table, then optionally attach variable metadata
        // to (FileMetadata, DataVariable) pairs that the recipe says should have it.
        List<DataVariable> fileVariables = buildVariables(
            dataTable,
            variableSetContext,
            variableCount,
            context,
            accumulator
        );
        
        dataTable.setVarQuantity((long) variableCount);
        
        buildVariableMetadata(
            fileMetadata,
            fileVariables,
            tabularRecipe,
            variableSetRecipe.variableMetadataRecipe(),
            fileContext.fileIndex(),
            accumulator
        );
        
        // Currently every non-empty tabular file gets exactly one var group with all variables.
        // This will become recipe-driven once we introduce a dedicated VarGroupRecipe.
        if (!fileVariables.isEmpty()) {
            buildVarGroup(fileMetadata, fileVariables, fileContext, context, accumulator);
        }
    }
    
    /**
     * Creates the requested number of {@link DataVariable} entities and wires them to the table.
     *
     * @param dataTable owning data table
     * @param variableSetContext variable-set build context
     * @param variableCount number of variables to create
     * @param context fixture build context
     * @param accumulator accumulator collecting all generated entities
     * @return the variables created for this file/table, in order
     */
    private List<DataVariable> buildVariables(
        DataTable dataTable,
        VariableSetBuildContext variableSetContext,
        int variableCount,
        BuildContext context,
        BuildAccumulator accumulator
    ) {
        List<DataVariable> fileVariables = new ArrayList<>(variableCount);
        
        for (int variableIndex = 0; variableIndex < variableCount; variableIndex++) {
            DataVariable dataVariable = new DataVariable();
            populator.populateDataVariable(dataVariable, variableSetContext, variableIndex, context);
            wireDataVariable(dataTable, dataVariable);
            
            fileVariables.add(dataVariable);
            accumulator.addDataVariable(dataVariable);
        }
        
        return fileVariables;
    }
    
    /**
     * Creates {@link VariableMetadata} rows for the (file metadata, variable) pairs the recipe
     * says should receive metadata.
     *
     * <p>Each metadata entity links one {@link DataVariable} and one {@link FileMetadata}.
     * Because the schema enforces uniqueness on that pair, we create at most one metadata
     * row per variable for the given file metadata.</p>
     *
     * @param fileMetadata file metadata for the current version
     * @param fileVariables variables in the file's tabular structure
     * @param tabularRecipe tabular file recipe
     * @param metadataRecipe variable-metadata recipe deciding which pairs get metadata
     * @param fileIndex zero-based file index
     * @param accumulator accumulator collecting all generated entities
     */
    private void buildVariableMetadata(
        FileMetadata fileMetadata,
        List<DataVariable> fileVariables,
        FileRecipe.Tabular tabularRecipe,
        VariableMetadataRecipe metadataRecipe,
        int fileIndex,
        BuildAccumulator accumulator
    ) {
        for (int variableIndex = 0; variableIndex < fileVariables.size(); variableIndex++) {
            VariableMetadataBuildContext metadataContext =
                new VariableMetadataBuildContext(tabularRecipe, fileIndex, variableIndex);
            
            if (!metadataRecipe.createFor(metadataContext)) {
                continue;
            }
            
            DataVariable variable = fileVariables.get(variableIndex);
            VariableMetadata metadata = new VariableMetadata(variable, fileMetadata);
            populator.populateVariableMetadata(metadata, metadataContext);
            
            wireVariableMetadata(fileMetadata, variable, metadata);
            accumulator.addVariableMetadata(metadata);
        }
    }
    
    /**
     * Creates a {@link VarGroup} containing all variables of a tabular file and attaches it
     * to the file metadata.
     *
     * @param fileMetadata file metadata receiving the var group
     * @param fileVariables variables in the file's tabular structure
     * @param fileContext file build context
     * @param context fixture build context
     * @param accumulator accumulator collecting all generated entities
     */
    private void buildVarGroup(
        FileMetadata fileMetadata,
        List<DataVariable> fileVariables,
        FileBuildContext fileContext,
        BuildContext context,
        BuildAccumulator accumulator
    ) {
        VarGroup varGroup = new VarGroup();
        populator.populateVarGroup(varGroup, fileContext, FIRST_AND_ONLY_VAR_GROUP_INDEX, context);
        wireVarGroup(fileMetadata, varGroup, fileVariables);
        accumulator.addVarGroup(varGroup);
    }
    
    /**
     * Wires a dataset and its current version together.
     *
     * @param dataset dataset root
     * @param version current dataset version
     */
    private void wireDatasetAndVersion(Dataset dataset, DatasetVersion version) {
        version.setDataset(dataset);
        dataset.getVersions().add(version);
    }
    
    /**
     * Wires file metadata to its dataset version and underlying data file.
     *
     * @param datasetVersion owning dataset version
     * @param fileMetadata file metadata to wire
     * @param dataFile data file to wire
     */
    private void wireFileMetadata(DatasetVersion datasetVersion, FileMetadata fileMetadata, DataFile dataFile) {
        fileMetadata.setDatasetVersion(datasetVersion);
        fileMetadata.setDataFile(dataFile);
        datasetVersion.getFileMetadatas().add(fileMetadata);
        dataFile.getFileMetadatas().add(fileMetadata);
    }
    
    /**
     * Wires a data table to its data file.
     *
     * @param dataFile parent data file
     * @param dataTable child data table
     */
    private void wireDataTable(DataFile dataFile, DataTable dataTable) {
        dataTable.setDataFile(dataFile);
        dataFile.getDataTables().add(dataTable);
    }
    
    /**
     * Wires a data variable to its data table.
     *
     * @param dataTable parent data table
     * @param dataVariable child data variable
     */
    private void wireDataVariable(DataTable dataTable, DataVariable dataVariable) {
        dataVariable.setDataTable(dataTable);
        dataTable.getDataVariables().add(dataVariable);
    }
    
    /**
     * Wires a variable group to file metadata and assigns the supplied variables to that group.
     *
     * @param fileMetadata owning file metadata
     * @param varGroup variable group to wire
     * @param variables variables to include in the group
     */
    private void wireVarGroup(FileMetadata fileMetadata, VarGroup varGroup, List<DataVariable> variables) {
        varGroup.setFileMetadata(fileMetadata);
        varGroup.getVarsInGroup().addAll(variables);
        fileMetadata.getVarGroups().add(varGroup);
    }
    
    /**
     * Wires a variable metadata row into both inverse collections (file metadata and variable).
     *
     * @param fileMetadata file metadata side of the pair
     * @param variable variable side of the pair
     * @param metadata variable metadata to wire
     */
    private void wireVariableMetadata(FileMetadata fileMetadata, DataVariable variable, VariableMetadata metadata) {
        fileMetadata.getVariableMetadatas().add(metadata);
        variable.getVariableMetadatas().add(metadata);
    }
    
    /**
     * Internal accumulator collecting all generated entities so the fixture can expose them.
     *
     * <p>This keeps the build helper methods compact and avoids passing many lists around.</p>
     */
    private static final class BuildAccumulator {
        
        private final List<FileMetadata> fileMetadatas = new ArrayList<>();
        private final List<DataFile> dataFiles = new ArrayList<>();
        private final List<DataTable> dataTables = new ArrayList<>();
        private final List<DataVariable> dataVariables = new ArrayList<>();
        private final List<VarGroup> varGroups = new ArrayList<>();
        private final List<VariableMetadata> variableMetadata = new ArrayList<>();
        
        void addFileMetadata(FileMetadata fileMetadata) {
            fileMetadatas.add(fileMetadata);
        }
        
        void addDataFile(DataFile dataFile) {
            dataFiles.add(dataFile);
        }
        
        void addDataTable(DataTable dataTable) {
            dataTables.add(dataTable);
        }
        
        void addDataVariable(DataVariable dataVariable) {
            dataVariables.add(dataVariable);
        }
        
        void addVarGroup(VarGroup varGroup) {
            varGroups.add(varGroup);
        }
        
        void addVariableMetadata(VariableMetadata metadata) {
            variableMetadata.add(metadata);
        }
        
        DatasetFixture toFixture(Dataset dataset, DatasetVersion currentVersion) {
            return new DatasetFixture(
                dataset,
                currentVersion,
                fileMetadatas,
                dataFiles,
                dataTables,
                dataVariables,
                varGroups,
                variableMetadata
            );
        }
    }
}