/*
 * Copyright 2018 Forschungszentrum Jülich GmbH
 * SPDX-License-Identifier: Apache 2.0
 */
package edu.harvard.iq.dataverse.dataaccess;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.api.UtilIT;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.FileUtil;

import org.junit.jupiter.api.AfterEach;
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
    private S3AsyncClient s3client;
    
    private StorageIO<Dataset> dataSetAccess;
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
        System.setProperty("dataverse.files.s3test.type", "s3");
        System.setProperty("dataverse.files.s3test.label", "S3test");
        System.setProperty("dataverse.files.s3test.bucket-name", "thebucket");

        dataFile.setStorageIdentifier("s3test://thebucket:"+dataFileId);
        dataSetAccess = new S3AccessIO<>(dataSet, null, s3client, "s3test");
        dataFileAccess = new S3AccessIO<>(dataFile, null, s3client, "s3test");
    }
    
    @AfterEach
    public void tearDown() {
        System.clearProperty("dataverse.files.s3test.type");
        System.clearProperty("dataverse.files.s3test.label");
        System.clearProperty("dataverse.files.s3test.bucket-name");
    }
    
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
    void keyNullstorageIdInvalid_getMainFileKey() throws IOException {
        // given
        dataFile.setStorageIdentifier("invalid://abcd");
        // when & then
        assertThrows(IOException.class, () -> {dataFileAccess.getMainFileKey(); });
    }
    
    @Test
    void default_getUrlExpirationMinutes() {
        // given
        System.clearProperty("dataverse.files.s3test.url-expiration-minutes");
        // when & then
        assertEquals(60, dataFileAccess.getUrlExpirationMinutes());
    }
    
    @Test
    void validSetting_getUrlExpirationMinutes() {
        // given
        System.setProperty("dataverse.files.s3test.url-expiration-minutes", "120");
        // when & then
        assertEquals(120, dataFileAccess.getUrlExpirationMinutes());
    }
    
    @Test
    void invalidSetting_getUrlExpirationMinutes() {
        // given
        System.setProperty("dataverse.files.s3test.url-expiration-minutes", "NaN");
        // when & then
        assertEquals(60, dataFileAccess.getUrlExpirationMinutes());
    }
    
    @Test
    void testS3IdentifierFormats() throws IOException {
        assertTrue(DataAccess.isValidDirectStorageIdentifier("s3test://thebucket:" + FileUtil.generateStorageIdentifier()));
        //The tests here don't use a valid identifier string
        assertFalse(DataAccess.isValidDirectStorageIdentifier(dataFile.getStorageIdentifier()));
        //bad store id
        assertFalse(DataAccess.isValidDirectStorageIdentifier("s3://thebucket:" + FileUtil.generateStorageIdentifier()));
        //bad bucket
        assertFalse(DataAccess.isValidDirectStorageIdentifier("s3test://bucket:" + FileUtil.generateStorageIdentifier()));
    }
}