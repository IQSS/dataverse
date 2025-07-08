package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import jakarta.json.JsonArray;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class InternalExportProviderTest {

    /**
     * this is a unit-test for seeing why the directory-labels are
     * not showing in the exported data and how best to remedy the case
     */
    @Test
    public void getDatasetFileDetailsVanilla() throws Exception {

        DatasetVersion mockDV = Mockito.mock(DatasetVersion.class);
        List<FileMetadata> metadataList = new ArrayList<>();
        FileMetadata metadata = new FileMetadata();
        metadata.setId(1L);
        metadata.setDirectoryLabel("some/directory/label");
        metadata.setLabel("some-silly-label");

        DataFile datafile = new DataFile();
        datafile.setId(1L);
        metadata.setDataFile(datafile);

        List<DataTable> dataTables = new ArrayList<>();
        DataTable dataTable = new DataTable();
        dataTable.setOriginalFileName("some_silly_original_file_name");
        dataTables.add(dataTable);

        datafile.setDataTables(dataTables);

        metadataList.add(metadata);
        when(mockDV.getFileMetadatas()).thenReturn(metadataList);

        InternalExportDataProvider provider = new InternalExportDataProvider(mockDV);

        JsonArray json = provider.getDatasetFileDetails();
        assertTrue(json.toString().contains("some/directory/label"));
    }

}
