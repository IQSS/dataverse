package edu.harvard.iq.dataverse.datafile.file;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.datafile.file.exception.ProvenanceChangeException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.provenance.UpdatesEntry;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static io.vavr.collection.HashMap.of;

@ExtendWith(MockitoExtension.class)
public class SingleFileFacadeTest {

    @InjectMocks
    private SingleFileFacade singleFileFacade;

    @Mock
    private FileMetadataService fileMetadataService;

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @Mock
    private GenericDao genericDao;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Test
    public void saveFileChanges_WithError() {
        //given
        String checksum = "testChecksum";

        DataFile dataFile = new DataFile();
        dataFile.setChecksumValue(checksum);
        dataFile.setProvEntityName("");
        FileMetadata fileToSave = new FileMetadata();
        fileToSave.setDataFile(dataFile);

        String provFreeForm = "provFree";
        HashMap<String, UpdatesEntry> provenanceUpdates = of(checksum, new UpdatesEntry(dataFile, "prov", false, provFreeForm))
                .toJavaMap();

        //when & then
        Mockito.when(settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)).thenReturn(true);
        Mockito.when(fileMetadataService.manageProvJson(Mockito.any(Boolean.TYPE), Mockito.any())).thenThrow(NullPointerException.class);

        Assertions.assertThrows(ProvenanceChangeException.class, () -> singleFileFacade.saveFileChanges(fileToSave, provenanceUpdates));
    }

    @Test
    public void saveFileChanges() {
        //given
        String checksum = "testChecksum";

        DataFile dataFile = new DataFile();
        dataFile.setChecksumValue(checksum);
        dataFile.setProvEntityName("");
        FileMetadata fileToSave = new FileMetadata();
        fileToSave.setDataFile(dataFile);
        DatasetVersion dsv = new DatasetVersion();
        dsv.setId(1L);
        Dataset dataset = new Dataset();
        dsv.setDataset(dataset);
        dataset.setVersions(Lists.newArrayList(dsv));
        fileToSave.setDatasetVersion(dsv);

        String provFreeForm = "provFree";
        HashMap<String, UpdatesEntry> provenanceUpdates = of(checksum, new UpdatesEntry(dataFile, "prov", false, provFreeForm))
                .toJavaMap();

        //when
        Mockito.when(settingsService.isTrueForKey(SettingsServiceBean.Key.ProvCollectionEnabled)).thenReturn(true);
        Mockito.when(commandEngine.submit(Mockito.any(UpdateDatasetVersionCommand.class))).thenReturn(dataset);
        Mockito.when(fileMetadataService.manageProvJson(Mockito.any(Boolean.TYPE), Mockito.any())).then(Answers.RETURNS_MOCKS);
        Mockito.when(genericDao.find(Mockito.anyLong(), Mockito.eq(DatasetVersion.class))).thenReturn(new DatasetVersion());

        singleFileFacade.saveFileChanges(fileToSave, provenanceUpdates);

        //then
        Mockito.verify(commandEngine, Mockito.times(1)).submit(Mockito.any(UpdateDatasetVersionCommand.class));
        Mockito.verify(fileMetadataService, Mockito.times(1)).manageProvJson(Mockito.eq(false), Mockito.any());
    }
}