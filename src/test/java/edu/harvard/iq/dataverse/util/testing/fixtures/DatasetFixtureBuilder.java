package edu.harvard.iq.dataverse.util.testing.fixtures;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
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
 * </ul>
 */
public class DatasetFixtureBuilder {

    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    private DatasetRecipe recipe;
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
     * @param recipe dataset recipe to use
     * @return this builder for fluent chaining
     */
    public DatasetFixtureBuilder recipe(DatasetRecipe recipe) {
        this.recipe = Objects.requireNonNull(recipe);
        return this;
    }

    /**
     * Sets the scalar-field defaults policy.
     *
     * @param populator defaults policy to use
     * @return this builder for fluent chaining
     */
    public DatasetFixtureBuilder populator(FixturePopulator populator) {
        this.populator = Objects.requireNonNull(populator);
        return this;
    }

    /**
     * Builds a dataset fixture graph according to the configured recipe and populator.
     *
     * @return generated dataset fixture
     */
    public DatasetFixture build() {
        if (recipe == null) {
            throw new IllegalStateException("A DatasetRecipe must be configured before building.");
        }

        BuildContext context = new BuildContext(SEQUENCE.getAndIncrement());

        Dataset dataset = new Dataset();
        populator.populateDataset(dataset, context);

        DatasetVersion currentVersion = new DatasetVersion();
        populator.populateDatasetVersion(currentVersion, context);
        
        // The constructor of Dataset implicitely creates a new version. Get rid of it before we wire ours.
        dataset.setVersions(new ArrayList<>());
        wireDatasetAndVersion(dataset, currentVersion);

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        List<DataFile> dataFiles = new ArrayList<>();
        List<DataTable> dataTables = new ArrayList<>();
        List<DataVariable> dataVariables = new ArrayList<>();
        List<VarGroup> varGroups = new ArrayList<>();

        VersionRecipe versionRecipe = recipe.currentVersionRecipe();
        List<FileRecipe> fileRecipes = versionRecipe.fileRecipes();
        
        int globalFileIndex = 0;
        for (FileRecipe fileRecipe : fileRecipes) {
            for (int fileIndex = 0; fileIndex < fileRecipe.fileCount(); fileIndex++, globalFileIndex++) {
                FileBuildContext fileContext = new FileBuildContext(fileRecipe, globalFileIndex);
                
                DataFile dataFile = new DataFile();
                populator.populateDataFile(dataFile, fileContext, context);
                
                FileMetadata fileMetadata = new FileMetadata();
                populator.populateFileMetadata(fileMetadata, fileContext, context);
                
                wireFileMetadata(currentVersion, fileMetadata, dataFile);
                fileMetadatas.add(fileMetadata);
                dataFiles.add(dataFile);
                
                
                TODO
                    
                    - create and wire in variable metadata
                    - populate the variable metadata (extend interface, too)
                    - add lots more inline comments to this method, maybe split some
                    - add missing java docs for some classes
                    - create a version evolution, create builders and populators
                    - make datasetfixture respect versions when retrieving collections. add convenience methods pointing to current version.
                
                if (fileRecipe instanceof FileRecipe.Tabular tabularRecipe) {
                    DataTable dataTable = new DataTable();
                    populator.populateDataTable(dataTable, fileContext, context);
                    wireDataTable(dataFile, dataTable);
                    dataTables.add(dataTable);
                    
                    var variableSetContext = new VariableSetBuildContext(tabularRecipe, globalFileIndex);
                    
                    VariableSetRecipe variableSetRecipe = tabularRecipe.variableSetRecipe();
                    int variableCount = variableSetRecipe.variableCount(variableSetContext);
                    
                    List<DataVariable> fileVariables = new ArrayList<>(variableCount);
                    
                    for (int variableIndex = 0; variableIndex < variableCount; variableIndex++) {
                        DataVariable dataVariable = new DataVariable();
                        populator.populateDataVariable(dataVariable, variableSetContext, variableIndex, context);
                        wireDataVariable(dataTable, dataVariable);
                        fileVariables.add(dataVariable);
                        dataVariables.add(dataVariable);
                    }
                    
                    dataTable.setVarQuantity((long) variableCount);
                    
                    if (!fileVariables.isEmpty()) {
                        VarGroup varGroup = new VarGroup();
                        populator.populateVarGroup(varGroup, fileContext, 0, context);
                        wireVarGroup(fileMetadata, varGroup, fileVariables);
                        varGroups.add(varGroup);
                    }
                }
            }
        }

        return new DatasetFixture(
                dataset,
                currentVersion,
                fileMetadatas,
                dataFiles,
                dataTables,
                dataVariables,
                varGroups
        );
    }

    /**
     * Wires a dataset and its current version together.
     *
     * <p>This method centralizes the relationship setup between dataset and
     * version. If your concrete {@code Dataset} API maintains versions differently,
     * this is the place to adapt.</p>
     *
     * @param dataset dataset root
     * @param version current dataset version
     */
    private void wireDatasetAndVersion(Dataset dataset, DatasetVersion version) {
        version.setDataset(dataset);

        if (dataset.getVersions() == null) {
            dataset.setVersions(new ArrayList<>());
        }
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
     * Wires a variable group to file metadata and assigns the supplied variables
     * to that group.
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
     * Internal immutable build context shared across a single fixture build.
     *
     * @param sequence deterministic sequence number for the fixture instance
     */
    public record BuildContext(
            long sequence
    ) {
    }
}
