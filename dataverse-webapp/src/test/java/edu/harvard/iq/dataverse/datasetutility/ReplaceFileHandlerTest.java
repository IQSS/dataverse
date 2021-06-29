package edu.harvard.iq.dataverse.datasetutility;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.datafile.DataFileCreator;
import edu.harvard.iq.dataverse.datafile.file.ReplaceFileHandler;
import edu.harvard.iq.dataverse.datafile.file.exception.FileReplaceException;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.group.IPv4Address;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReplaceFileHandlerTest {

    @InjectMocks
    private ReplaceFileHandler replaceFileHandler;

    @Mock
    private IngestServiceBean ingestService;

    @Mock
    private DataFileServiceBean dataFileServiceBean;

    @Mock
    private DataFileCreator dataFileCreator;

    @Mock
    private EjbDataverseEngine ejbDataverseEngine;

    @Mock
    private DataverseRequestServiceBean dataverseRequestServiceBean;

    @BeforeEach
    public void setUp() {

        when(dataverseRequestServiceBean.getDataverseRequest()).thenReturn(new DataverseRequest(new AuthenticatedUser(), new IPv4Address(111)));
        when(ingestService.saveAndAddFilesToDataset(any(DatasetVersion.class), any()))
                .thenReturn(Lists.newArrayList(new DataFile()));
        
        when(ejbDataverseEngine.submit(any(UpdateDatasetVersionCommand.class))).then(new Answer<Dataset>() {

            @Override
            public Dataset answer(InvocationOnMock invocation) throws Throwable {
                UpdateDatasetVersionCommand command = invocation.getArgument(0);
                return (Dataset) command.getAffectedDvObjects().values().iterator().next();
            }
        });
    }

    @Test
    public void createDataFile_shouldFailWithZip() {
        //given
        Dataset dataset = new Dataset();
        String fileName = "testFile";
        String fileContentType = "application/zip";

        //then
        FileReplaceException thrown;
        thrown = Assertions.assertThrows(FileReplaceException.class,
                                () -> replaceFileHandler.createDataFile(dataset, new byte[0], fileName, fileContentType));
        Assertions.assertEquals(FileReplaceException.Reason.ZIP_NOT_SUPPORTED, thrown.getReason());

    }

    @Test
    public void createDataFile_shouldFailWhenVirusWasDetected() throws IOException {
        //given
        Dataset dataset = new Dataset();
        String fileName = "testFile.png";
        String fileContentType = "image/png";
        when(dataFileCreator.createDataFiles(any(), any(), any(), any())).thenThrow(VirusFoundException.class);

        //then
        FileReplaceException thrown;
        thrown = Assertions.assertThrows(FileReplaceException.class,
                                () -> replaceFileHandler.createDataFile(dataset, new byte[0], fileName, fileContentType));
        Assertions.assertEquals(FileReplaceException.Reason.VIRUS_DETECTED, thrown.getReason());

    }

    @Test
    public void replaceFile_ShouldSuccessfullyReplaceFile() {
        //given
        Dataset dataset = new Dataset();
        DataFile fileToBeReplaced = generateFileToBeReplaced(dataset);
        DataFile newFile = generateNewFile(dataset);

        //when
        DataFile addedFile = replaceFileHandler.replaceFile(fileToBeReplaced, dataset, newFile);

        //then
        Assert.assertEquals(newFile.getFileMetadata().getLabel(), addedFile.getFileMetadata().getLabel());
        Assert.assertFalse(dataset.getEditVersion().getFileMetadatas().contains(fileToBeReplaced.getFileMetadata()));

    }

    private DataFile generateFileToBeReplaced(Dataset fileOwner) {
        DataFile dataFile = new DataFile();

        dataFile.setId(1L);
        dataFile.setContentType("json");
        dataFile.setFilesize(1);
        dataFile.setOwner(fileOwner);

        FileMetadata fileMetadata = new FileMetadata();

        fileMetadata.setId(1L);
        fileMetadata.setLabel("replaced file");
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setTermsOfUse(new FileTermsOfUse());

        dataFile.setFileMetadatas(Lists.newArrayList(fileMetadata));
        fileOwner.getEditVersion().getFileMetadatas().add(fileMetadata);

        return dataFile;
    }

    private DataFile generateNewFile(Dataset fileOwner) {
        DataFile dataFile = new DataFile();

        dataFile.setContentType("json");
        dataFile.setFilesize(1);
        dataFile.setOwner(fileOwner);

        FileMetadata fileMetadata = new FileMetadata();

        fileMetadata.setLabel("new file");
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setTermsOfUse(new FileTermsOfUse());

        dataFile.setFileMetadatas(Lists.newArrayList(fileMetadata));
        fileOwner.getEditVersion().getFileMetadatas().add(fileMetadata);

        return dataFile;
    }
}