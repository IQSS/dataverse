package edu.harvard.iq.dataverse.util.testing.fixtures;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.util.testing.recipes.FileBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableMetadataBuildContext;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetBuildContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public final class MinimalPopulator implements FixturePopulator {
    
    /**
     * Populates basic dataset scalar fields.
     *
     * @param dataset dataset being initialized
     * @param context fixture build context
     */
    @Override
    public void populateDataset(Dataset dataset, BuildContext context) {
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072");
        dataset.setIdentifier("fixture-dataset-" + context.sequence());
        dataset.setStorageIdentifier("fixture-storage-" + context.sequence());
        dataset.setDatasetType(new DatasetType());
    }
    
    /**
     * Populates basic dataset-version scalar fields, timestamps, and terms.
     *
     * @param version dataset version being initialized
     * @param context fixture build context
     */
    @Override
    public void populateDatasetVersion(DatasetVersion version, BuildContext context) {
        Date now = new Date();
        version.setVersionNumber(1L);
        version.setMinorVersionNumber(0L);
        version.setVersionState(DatasetVersion.VersionState.DRAFT);
        version.setVersionNote("fixture-version");
        version.setCreateTime(now);
        version.setLastUpdateTime(now);
        version.setTermsOfUseAndAccess(new TermsOfUseAndAccess());
    }
    
    /**
     * Populates basic file-metadata scalar fields.
     *
     * @param fileMetadata file metadata being initialized
     * @param fileBuildContext file build context
     * @param context fixture build context
     */
    @Override
    public void populateFileMetadata(FileMetadata fileMetadata, FileBuildContext fileBuildContext, BuildContext context) {
        fileMetadata.setLabel("file-" + fileBuildContext.fileIndex() + ".tab");
        fileMetadata.setDescription("Fixture file " + fileBuildContext.fileIndex());
        fileMetadata.setVarGroups(new ArrayList<>());
        fileMetadata.setVariableMetadatas(new ArrayList<>());
    }
    
    /**
     * Populates basic data-file scalar fields and null-sensitive defaults.
     *
     * @param dataFile data file being initialized
     * @param fileBuildContext file build context
     * @param context fixture build context
     */
    @Override
    public void populateDataFile(DataFile dataFile, FileBuildContext fileBuildContext, BuildContext context) {
        dataFile.setContentType("text/tab-separated-values");
        dataFile.setChecksumType(DataFile.ChecksumType.SHA1);
        dataFile.setChecksumValue("fixture-checksum-" + fileBuildContext.fileIndex());
        dataFile.setFilesize(1024L + fileBuildContext.fileIndex());
        dataFile.setDataTables(new ArrayList<>());
        dataFile.setFileMetadatas(new ArrayList<>());
        dataFile.setTags(new ArrayList<>());
    }
    
    /**
     * Populates basic data-table scalar fields and variable collection defaults.
     *
     * @param dataTable data table being initialized
     * @param fileBuildContext file build context
     * @param context fixture build context
     */
    @Override
    public void populateDataTable(DataTable dataTable, FileBuildContext fileBuildContext, BuildContext context) {
        dataTable.setVarQuantity(0L);
        dataTable.setCaseQuantity(100L);
        dataTable.setRecordsPerCase(1L);
        dataTable.setUnf("UNF:fixture-table-" + fileBuildContext.fileIndex());
        dataTable.setDataVariables(new ArrayList<>());
        dataTable.setOriginalFileFormat("text/tab-separated-values");
        dataTable.setOriginalFileName("fixture-original-" + fileBuildContext.fileIndex() + ".tab");
        dataTable.setOriginalFileSize(2048L + fileBuildContext.fileIndex());
    }
    
    /**
     * Populates basic data-variable scalar fields and initializes collections
     * that are null-sensitive in serialization.
     *
     * @param dataVariable data variable being initialized
     * @param variableSetBuildContext larger context of the data variable being populated
     * @param variableIndex zero-based variable index within the file/table
     * @param context fixture build context
     */
    @Override
    public void populateDataVariable(
        DataVariable dataVariable,
        VariableSetBuildContext variableSetBuildContext,
        int variableIndex,
        BuildContext context
    ) {
        dataVariable.setName("var_" + variableSetBuildContext.fileIndex() + "_" + variableIndex);
        dataVariable.setLabel("Variable " + variableSetBuildContext.fileIndex() + "/" + variableIndex);
        dataVariable.setType(DataVariable.VariableType.NUMERIC);
        dataVariable.setFileOrder(variableIndex);
        dataVariable.setUnf("UNF:fixture-var-" + variableSetBuildContext.fileIndex() + "-" + variableIndex);
        dataVariable.setInvalidRanges(new ArrayList<>());
        dataVariable.setSummaryStatistics(new ArrayList<>());
        dataVariable.setCategories(new ArrayList<>());
        dataVariable.setVariableMetadatas(new ArrayList<>());
        dataVariable.setInvalidRangeItems(new ArrayList<>());
    }
    
    /**
     * Populates metadata for a data variable. Updates the label with a unique identifier
     * generated based on the provided build context.
     *
     * @param metadata the variable metadata object to be populated
     * @param variableMetadataBuildContext the context containing information about
     *        the variable, including file and variable indices
     */
    @Override
    public void populateVariableMetadata(VariableMetadata metadata, VariableMetadataBuildContext variableMetadataBuildContext) {
        metadata.setLabel("variable-metadata-" + variableMetadataBuildContext.fileIndex() +
            "-" + variableMetadataBuildContext.variableIndex());
    }
    
    /**
     * Populates basic variable-group scalar fields and initializes the backing
     * variable set.
     *
     * @param varGroup var group being initialized
     * @param fileBuildContext file build context
     * @param groupIndex zero-based group index within the file
     * @param context fixture build context
     */
    @Override
    public void populateVarGroup(
        VarGroup varGroup,
        FileBuildContext fileBuildContext,
        int groupIndex,
        BuildContext context
    ) {
        varGroup.setLabel("group-" + fileBuildContext.fileIndex() + "-" + groupIndex);
        varGroup.setVarsInGroup(new HashSet<>());
    }
    
}
