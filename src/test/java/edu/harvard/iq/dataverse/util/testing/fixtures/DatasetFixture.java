package edu.harvard.iq.dataverse.util.testing.fixtures;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VarGroup;

import java.util.List;

/**
 * Immutable holder for a generated dataset fixture graph.
 *
 * <p>This object gives tests convenient access not only to the root
 * {@link Dataset}, but also to the current {@link DatasetVersion} and all major
 * generated child entities. That makes it easier to inspect, persist, or tweak
 * the graph after building it.</p>
 *
 * @param dataset root dataset
 * @param currentVersion current dataset version
 * @param fileMetadatas generated file metadata objects
 * @param dataFiles generated data files
 * @param dataTables generated data tables
 * @param dataVariables generated data variables
 * @param varGroups generated var groups
 */
public record DatasetFixture(
    Dataset dataset,
    DatasetVersion currentVersion,
    List<FileMetadata> fileMetadatas,
    List<DataFile> dataFiles,
    List<DataTable> dataTables,
    List<DataVariable> dataVariables,
    List<VarGroup> varGroups
) {
    
    /**
     * Compact constructor performing defensive copies of collection components.
     */
    public DatasetFixture {
        fileMetadatas = List.copyOf(fileMetadatas);
        dataFiles = List.copyOf(dataFiles);
        dataTables = List.copyOf(dataTables);
        dataVariables = List.copyOf(dataVariables);
        varGroups = List.copyOf(varGroups);
    }
}