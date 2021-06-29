package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.JhoveConfigurationInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataFileCreatorTest {

    @InjectMocks
    private DataFileCreator dataFileCreator = new DataFileCreator();
    
    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private FileService fileService;
    @Mock
    private TermsOfUseFactory termsOfUseFactory;
    @Mock
    private TermsOfUseFormMapper termsOfUseFormMapper;
    
    
    @BeforeEach
    void before() throws IOException {
        new JhoveConfigurationInitializer().initializeJhoveConfig();
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    void createDataFiles_shouldComputeUncompressedSizeForZipFile() throws IOException {
        // given
        Dataset dataset = new Dataset();
        DatasetVersion version = dataset.getLatestVersion();
        byte[] zipBytes = UnitTestUtils.readFileToByteArray("jhove/archive.zip");
        
        when(settingsService.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes)).thenReturn(1024*1024L);
        when(settingsService.getValueForKey(Key.FileFixityChecksumAlgorithm)).thenReturn("MD5");
        
        // when
        List<DataFile> datafiles = dataFileCreator.createDataFiles(version, new ByteArrayInputStream(zipBytes), "zipfile.zip", "application/zip");
        // then
        assertThat(datafiles).hasSize(1);
        assertThat(datafiles).element(0).extracting(DataFile::getUncompressedSize)
            .isEqualTo(4L);
    }
}
