/*
 * Copyright 2018 Forschungszentrum Jülich GmbH
 * SPDX-License-Identifier: Apache 2.0
 */
package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.services.s3.AmazonS3;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.api.UtilIT;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

import java.io.FileNotFoundException;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class S3AccessIOTest {
    
    @Mock
    private AmazonS3 s3client;
    
    private S3AccessIO<Dataset> dataSetAccess;
    private S3AccessIO<DataFile> dataFileAccess;
    private Dataset dataSet;
    private DataFile dataFile;
    private String dataFileId;
    
    @BeforeEach
    public void setup() throws IOException {
        dataFile = MocksFactory.makeDataFile();
        dataSet = MocksFactory.makeDataset();
        dataFile.setOwner(dataSet);
        dataFileId = UtilIT.getRandomIdentifier();
        dataFile.setStorageIdentifier("s3://bucket:"+dataFileId);
        dataSetAccess = new S3AccessIO<>(dataSet, null, s3client);
        dataFileAccess = new S3AccessIO<>(dataFile, null, s3client);
    }
    
    /*
    createTempFile
    getStorageLocation
    getFileSystemPath
    exists?
    getWriteChannel
    getOutputStream
    getDestinationKey
    
    DONE
    ---------------------
    getMainFileKey
    getUrlExpirationMinutes
     */
    
    @Test
    void keyNull_getMainFileKey() throws IOException {
        // given
        String authOwner = dataSet.getAuthority();
        String idOwner = dataSet.getIdentifier();
        
        // when
        String key = dataFileAccess.getMainFileKey();
        
        // then
        assertEquals(authOwner+"/"+idOwner+"/"+dataFileId, key);
    }
    
    @Test
    void keyNullstorageIdNullOrEmpty_getMainFileKey() throws IOException {
        // given
        dataFile.setStorageIdentifier(null);
        // when & then
        assertThrows(FileNotFoundException.class, () -> {dataFileAccess.getMainFileKey(); });
    
        // given
        dataFile.setStorageIdentifier("");
        // when & then
        assertThrows(FileNotFoundException.class, () -> {dataFileAccess.getMainFileKey(); });
    }
    
    @Test
    void keyNullstorageIdNull_getMainFileKey() throws IOException {
        // given
        dataFile.setStorageIdentifier("invalid://abcd");
        // when & then
        assertThrows(IOException.class, () -> {dataFileAccess.getMainFileKey(); });
    }
    
    @Test
    void default_getUrlExpirationMinutes() {
        // given
        System.clearProperty("dataverse.files.s3-url-expiration-minutes");
        // when & then
        assertEquals(60, dataFileAccess.getUrlExpirationMinutes());
    }
    
    @Test
    void validSetting_getUrlExpirationMinutes() {
        // given
        System.setProperty("dataverse.files.s3-url-expiration-minutes", "120");
        // when & then
        assertEquals(120, dataFileAccess.getUrlExpirationMinutes());
    }
    
    @Test
    void invalidSetting_getUrlExpirationMinutes() {
        // given
        System.setProperty("dataverse.files.s3-url-expiration-minutes", "NaN");
        // when & then
        assertEquals(60, dataFileAccess.getUrlExpirationMinutes());
    }
    
}
