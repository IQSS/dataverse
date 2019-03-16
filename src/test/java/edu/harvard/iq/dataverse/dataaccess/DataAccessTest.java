package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;


public class DataAccessTest {
  DataFile datafile;
  Dataset dataset;
  DataAccess dataAccess;

  @BeforeEach
  void setUp() {
    datafile = MocksFactory.makeDataFile();
    dataset = MocksFactory.makeDataset();
    datafile.setOwner(dataset);
  }

  @Test
  void testCreateNewStorageIO_throwsWithoutObject() {
    assertThrows(IOException.class, () -> {
      DataAccess.createNewStorageIO(null, "valid-tag");
    });
  }

  @Test
  void testCreateNewStorageIO_throwsWithoutStorageTag() {
    assertThrows(IOException.class, () -> {
      DataAccess.createNewStorageIO(datafile, null);
    });
  }

  @Test
  void testCreateNewStorageIO_throwsWithEmptyStorageTag() {
    assertThrows(IOException.class, () -> {
      DataAccess.createNewStorageIO(datafile, "");
    });
  }

  @Test
  void testCreateNewStorageIO_throwsOnUnsupported() {
    assertThrows(IOException.class, () -> {
      DataAccess.createNewStorageIO(datafile, "valid-tag", "zip-unsupported");
    });
  }

  @Test
  void testCreateNewStorageIO_createsFileAccessIObyDefault() throws IOException {
    StorageIO<Dataset> storageIo = DataAccess.createNewStorageIO(dataset, "valid-tag");
    assertTrue(storageIo.getClass().equals(FileAccessIO.class));
  }

  /*
  TODO: testing these branches requires the possibility to inject swift and s3 client mocks
  @Test
  void testCreateNewStorageIO_updatesStorageIdentifier() throws IOException {
    StorageIO<Dataset> storageIo = DataAccess.createNewStorageIO(dataset, "valid-tag");
    DvObject obj = storageIo.getDvObject();
    assertEquals("valid-tag", obj.getStorageIdentifier());
  }

  @Test
  void testCreateNewStorageIO_createsSwiftAccessIO() throws IOException {
    StorageIO<Dataset> storageIo = DataAccess.createNewStorageIO(dataset, "valid-tag", "swift");
    assertTrue(storageIo.getClass().equals(SwiftAccessIO.class));
  }

  @Test
  void testCreateNewStorageIO_createsS3AccessIO() throws IOException {
    StorageIO<Dataset> storageIo = DataAccess.createNewStorageIO(dataset, "valid-tag", "s3");
    assertTrue(storageIo.getClass().equals(S3AccessIO.class));
  } */
}
