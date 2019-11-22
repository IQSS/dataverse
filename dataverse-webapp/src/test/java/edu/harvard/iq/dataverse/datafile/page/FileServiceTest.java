package edu.harvard.iq.dataverse.datafile.page;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.datafile.pojo.RsyncInfo;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UpdateDatasetException;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Option;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Mock
    private DataFileServiceBean dataFileService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @BeforeEach
    void setUp() throws CommandException {
        when(commandEngine.submit(Mockito.any(UpdateDatasetVersionCommand.class))).thenReturn(new Dataset());
        when(commandEngine.submit(Mockito.any(PersistProvFreeFormCommand.class))).thenReturn(new DataFile());
        when(dataFileService.getPhysicalFileToDelete(Mockito.any(DataFile.class))).thenReturn("location");
    }

    @Test
    public void deleteFile() throws CommandException {
        //given
        FileMetadata fileToDelete = prepareFileMetadata();

        //when
        fileService.deleteFile(fileToDelete);

        //then
        verify(commandEngine, times(1)).submit(Mockito.any(UpdateDatasetVersionCommand.class));
    }

    @Test
    public void deleteFile_WithUpdateError() throws CommandException {
        //given
        FileMetadata fileToDelete = prepareFileMetadata();
        when(commandEngine.submit(Mockito.any(UpdateDatasetVersionCommand.class))).thenAnswer(Answers.CALLS_REAL_METHODS);

        //when & then
        assertThrows(UpdateDatasetException.class, () -> fileService.deleteFile(fileToDelete));
        verify(commandEngine, times(1)).submit(Mockito.any(UpdateDatasetVersionCommand.class));

    }

    @Test
    public void deleteFile_WithConstraintError() throws CommandException {
        //given
        FileMetadata fileToDelete = prepareFileMetadata();
        fileToDelete.setLabel("");
        fileToDelete.getDatasetVersion().setFileMetadatas(Lists.newArrayList(fileToDelete));

        when(commandEngine.submit(Mockito.any(UpdateDatasetVersionCommand.class))).thenAnswer(Answers.CALLS_REAL_METHODS);

        //when & then
        assertThrows(ValidationException.class, () -> fileService.deleteFile(fileToDelete));
        verify(commandEngine, times(0)).submit(Mockito.any(UpdateDatasetVersionCommand.class));

    }

    @Test
    public void saveProvenanceFileWithDesc() throws CommandException {
        //given
        FileMetadata provFile = prepareFileMetadata();
        provFile.setProvFreeForm("desc");
        FileMetadata fileToEdit = prepareFileMetadata();

        //when
        fileService.saveProvenanceFileWithDesc(fileToEdit, provFile.getDataFile(), "provDesc");

        //then
        verify(commandEngine, times(1)).submit(Mockito.any(UpdateDatasetVersionCommand.class));
        verify(commandEngine, times(1)).submit(Mockito.any(PersistProvFreeFormCommand.class));
    }

    @Test
    public void saveProvenanceFileWithDesc_WithUpdateError() throws CommandException {
        //given
        FileMetadata provFile = prepareFileMetadata();
        provFile.setProvFreeForm("desc");
        FileMetadata fileToEdit = prepareFileMetadata();
        when(commandEngine.submit(Mockito.any(UpdateDatasetVersionCommand.class))).thenAnswer(Answers.CALLS_REAL_METHODS);

        //when & then
        assertThrows(UpdateDatasetException.class, () -> fileService.saveProvenanceFileWithDesc(fileToEdit, provFile.getDataFile(), "provDesc"));

        verify(commandEngine, times(1)).submit(Mockito.any(PersistProvFreeFormCommand.class));
        verify(commandEngine, times(1)).submit(Mockito.any(UpdateDatasetVersionCommand.class));
    }

    @Test
    public void saveProvenanceFileWithDesc_WithConstraintError() throws CommandException {
        //given
        FileMetadata provFile = prepareFileMetadata();
        provFile.setProvFreeForm("desc");
        FileMetadata fileToEdit = prepareFileMetadata();
        fileToEdit.setLabel("");
        fileToEdit.getDatasetVersion().setFileMetadatas(Lists.newArrayList(fileToEdit));

        //when & then
        assertThrows(ValidationException.class, () -> fileService.saveProvenanceFileWithDesc(fileToEdit, provFile.getDataFile(), "provDesc"));

        verify(commandEngine, times(1)).submit(Mockito.any(PersistProvFreeFormCommand.class));
    }

    @Test
    public void saveProvenanceFileWithDesc_WithPersistProvError() throws CommandException {
        //given
        FileMetadata provFile = prepareFileMetadata();
        provFile.setProvFreeForm("desc");
        FileMetadata fileToEdit = prepareFileMetadata();
        when(commandEngine.submit(Mockito.any(PersistProvFreeFormCommand.class))).thenAnswer(Answers.CALLS_REAL_METHODS);

        //when & then
        assertThrows(RuntimeException.class, () -> fileService.saveProvenanceFileWithDesc(fileToEdit, provFile.getDataFile(), "provDesc"));

        verify(commandEngine, times(1)).submit(Mockito.any(PersistProvFreeFormCommand.class));
    }

    @Test
    public void retrieveRsyncScript() {
        //given
        Dataset dataset = new Dataset();
        dataset.setIdentifier("testId");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);

        when(commandEngine.submit(any(RequestRsyncScriptCommand.class)))
                .thenReturn(new ScriptRequestResponse(200, "", 1, "testScript"));

        //when
        Option<RsyncInfo> rsyncScript = fileService.retrieveRsyncScript(dataset, datasetVersion);

        //then
        Assert.assertTrue(rsyncScript.isDefined());
        Assert.assertEquals("testScript", rsyncScript.get().getRsyncScript());
        Assert.assertEquals("upload-testId.bash", rsyncScript.get().getRsyncScriptFileName());

    }

    @Test
    public void retrieveRsyncScript_WithMissingScript() {
        //given
        Dataset dataset = new Dataset();
        dataset.setIdentifier("testId");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);

        when(commandEngine.submit(any(RequestRsyncScriptCommand.class)))
                .thenReturn(new ScriptRequestResponse(200, "", 1, ""));

        //when
        Option<RsyncInfo> rsyncScript = fileService.retrieveRsyncScript(dataset, datasetVersion);

        //then
        Assert.assertTrue(rsyncScript.isEmpty());
    }

    // -------------------- PRIVATE --------------------

    private FileMetadata prepareFileMetadata() {
        FileMetadata fileToDelete = new FileMetadata();
        DataFile dataFile = new DataFile();
        Dataset datafileOwner = new Dataset();
        DatasetVersion datasetVersion = new DatasetVersion();

        datafileOwner.setVersions(Lists.newArrayList(datasetVersion));
        dataFile.setOwner(datafileOwner);
        fileToDelete.setDataFile(dataFile);
        fileToDelete.setDatasetVersion(datasetVersion);
        return fileToDelete;
    }
}