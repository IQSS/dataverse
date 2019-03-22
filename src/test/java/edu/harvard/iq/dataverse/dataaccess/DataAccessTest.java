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
}
