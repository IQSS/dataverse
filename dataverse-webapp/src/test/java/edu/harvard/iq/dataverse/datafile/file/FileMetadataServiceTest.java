package edu.harvard.iq.dataverse.datafile.file;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadataRepository;
import edu.harvard.iq.dataverse.provenance.UpdatesEntry;
import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static io.vavr.collection.HashMap.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {

    @InjectMocks
    private FileMetadataService fileMetadataService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Test
    public void updateFileMetadataWithProvFreeForm() {
        //given
        String checksum = "testChecksum";
        String provFree = "provFree";

        DataFile dataFile = new DataFile();
        dataFile.setChecksumValue(checksum);
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDataFile(dataFile);

        HashMap<String, UpdatesEntry> provenanceUpdates = of(checksum, new UpdatesEntry(dataFile, "prov", false, provFree))
                .toJavaMap();

        //when
        FileMetadata updatedFile = fileMetadataService.updateFileMetadataWithProvFreeForm(fileMetadata, provFree);

        //then
        Assert.assertEquals(provFree, updatedFile.getProvFreeForm());
    }

    @Test
    public void updateFileMetadataWithProvFreeForm_WithNoProvenance() {
        //given
        String checksum = "testChecksum";

        DataFile dataFile = new DataFile();
        dataFile.setChecksumValue(checksum);
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDataFile(dataFile);

        //when
        fileMetadataService.updateFileMetadataWithProvFreeForm(fileMetadata, null);

        //then
        Assert.assertNull(fileMetadata.getProvFreeForm());
    }

    @Test
    public void manageProvJson_ForDeletingProv() {
        //given
        String checksum = "testChecksum";

        DataFile dataFile = new DataFile();
        dataFile.setChecksumValue(checksum);
        dataFile.setProvEntityName("testEntity");
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDataFile(dataFile);

        HashMap<String, UpdatesEntry> provenanceUpdates = of(checksum, new UpdatesEntry(dataFile, "prov", true, "provFree"))
                .toJavaMap();

        UpdatesEntry updatedEntry = provenanceUpdates.get(checksum);

        //when
        DataFile value = new DataFile();
        value.setProvEntityName("");
        Mockito.when(commandEngine.submit(Mockito.any(DeleteProvJsonCommand.class))).thenReturn(value);
        Option<DataFile> updatedFile = fileMetadataService.manageProvJson(true, updatedEntry);

        //then
        Assert.assertEquals("", updatedFile.get().getProvEntityName());
        Mockito.verify(commandEngine, Mockito.times(1)).submit(Mockito.any(DeleteProvJsonCommand.class));

    }

    @Test
    public void manageProvJson_ForPersistingProv() {
        //given
        String checksum = "testChecksum";

        DataFile dataFile = new DataFile();
        dataFile.setChecksumValue(checksum);
        dataFile.setProvEntityName("");
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDataFile(dataFile);

        String provFreeForm = "provFree";
        HashMap<String, UpdatesEntry> provenanceUpdates = of(checksum, new UpdatesEntry(dataFile, "prov", false, provFreeForm))
                .toJavaMap();

        UpdatesEntry updatedEntry = provenanceUpdates.get(checksum);

        //when
        DataFile persistedProvOwner = new DataFile();
        persistedProvOwner.setProvEntityName(provFreeForm);
        Mockito.when(commandEngine.submit(Mockito.any(PersistProvJsonCommand.class))).thenReturn(persistedProvOwner);
        Option<DataFile> updatedFile = fileMetadataService.manageProvJson(true, updatedEntry);

        //then
        Assert.assertEquals(provFreeForm, updatedFile.get().getProvEntityName());
        Mockito.verify(commandEngine, Mockito.times(1)).submit(Mockito.any(PersistProvJsonCommand.class));

    }

    @Test
    public void findAccessibleFileMetadataSorted() {
        //given
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);

        //when
        Mockito.when(fileMetadataRepository.findFileMetadataByDatasetVersionIdWithPagination(Mockito.any(Long.class),
                                                                                             Mockito.any(Integer.class),
                                                                                             Mockito.any(Integer.class)))
               .thenReturn(Lists.newArrayList(fileMetadata));

        List<FileMetadata> foundData = fileMetadataService.findAccessibleFileMetadataSorted(1, 1, 1);

        //then
        assertEquals(1, foundData.size());
        assertEquals(1, foundData.get(0).getId());
    }

    @Test
    public void findSearchedAccessibleFileMetadataSorted() {
        //given
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);

        //when
        Mockito.when(fileMetadataRepository.findSearchedFileMetadataByDatasetVersionIdWithPagination(Mockito.any(Long.class),
                                                                                                      Mockito.any(Integer.class),
                                                                                                      Mockito.any(Integer.class),
                                                                                                      Mockito.any(String.class)))
                .thenReturn(Lists.newArrayList(fileMetadata));

        List<FileMetadata> foundData = fileMetadataService.findSearchedAccessibleFileMetadataSorted(1, 1, 1, "");

        //then
        assertEquals(1, foundData.size());
        assertEquals(1, foundData.get(0).getId());
    }

    @Test
    public void findFileMetadataIds() {
        //given
        long fileMetadataId = 1;
        long dsvId = 1;

        //when
        Mockito.when(fileMetadataRepository.findFileMetadataIdsByDatasetVersionId(Mockito.any(Long.class)))
               .thenReturn(Lists.newArrayList(fileMetadataId));

        List<Long> foundData = fileMetadataService.findFileMetadataIds(dsvId);

        //then
        assertEquals(1, foundData.size());
        assertEquals(1, foundData.get(0));
    }

    @Test
    public void findRestrictedFileMetadata() {
        //given
        long fileMetadataId = 1;
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);

        //when
        Mockito.when(fileMetadataRepository.findRestrictedFileMetadata(Mockito.anyCollection()))
               .thenReturn(Lists.newArrayList(fileMetadata));

        List<FileMetadata> foundData = fileMetadataService.findRestrictedFileMetadata(Lists.newArrayList(fileMetadataId));

        //then
        assertEquals(1, foundData.size());
        assertEquals(1, foundData.get(0).getId());
    }

    @Test
    public void findFileMetadata() {
        //given
        long fileMetadataId = 1;
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(1L);

        //when
        Mockito.when(fileMetadataRepository.findFileMetadata(Mockito.anyList()))
               .thenReturn(Lists.newArrayList(fileMetadata));

        List<FileMetadata> foundData = fileMetadataService.findFileMetadata(fileMetadataId);

        //then
        assertEquals(1, foundData.size());
        assertEquals(1, foundData.get(0).getId());
    }
}