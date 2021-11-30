/*
 * Copyright 2018 Forschungszentrum Jülich GmbH
 * SPDX-License-Identifier: Apache 2.0
 */
package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.services.s3.AmazonS3;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private String defaultBucketName = "defaultBucketName";

    @BeforeEach
    public void setup() throws IOException {
        dataFile = MocksFactory.makeDataFile();
        dataSet = MocksFactory.makeDataset();
        dataSet.setStorageIdentifier("s3://dataset/storage/id");
        dataFile.setOwner(dataSet);
        dataFileId = UUID.randomUUID().toString().substring(0, 8);
        dataFile.setStorageIdentifier("s3://bucket:" + dataFileId);
        dataSetAccess = new S3AccessIO<>(dataSet, s3client, defaultBucketName);
        dataFileAccess = new S3AccessIO<>(dataFile, s3client, defaultBucketName);
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
    void constructor_dvObject_with_incorrect_storage_prefix() {
        // given
        dataSet.setStorageIdentifier("invalid://abc");
        // when & then
        assertThatThrownBy(() -> new S3AccessIO<>(dataSet, s3client, defaultBucketName))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void keyNull_getMainFileKey() throws IOException {

        // when
        String key = dataFileAccess.getMainFileKey();

        // then
        assertEquals("dataset/storage/id/" + dataFileId, key);
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
