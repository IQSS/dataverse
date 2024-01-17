package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;


public class DataAccessTest {
  DataFile datafile;
  Dataset dataset;
  Dataverse dataverse;

  @BeforeEach
  void setUp() {
    datafile = MocksFactory.makeDataFile();
    dataset = MocksFactory.makeDataset();
    dataverse = MocksFactory.makeDataverse();
    datafile.setOwner(dataset);
    dataset.setOwner(dataverse);
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
	System.setProperty("dataverse.files.file.type", "file");
	System.setProperty("dataverse.files.storage-driver-id", "file");
    StorageIO<Dataset> storageIo = DataAccess.createNewStorageIO(dataset, "valid-tag");
    assertTrue(storageIo.getClass().equals(FileAccessIO.class));
  }
  
  @Test
  void testGetLocationFromStorageId() {
      Dataset d = new Dataset();
      d.setAuthority("10.5072");
      d.setIdentifier("FK2/ABCDEF");
      assertEquals("s3://10.5072/FK2/ABCDEF/18b39722140-50eb7d3c5ece",
              DataAccess.getLocationFromStorageId("s3://18b39722140-50eb7d3c5ece", d));
      assertEquals("10.5072/FK2/ABCDEF/18b39722140-50eb7d3c5ece",
              DataAccess.getLocationFromStorageId("18b39722140-50eb7d3c5ece", d));

  }
  
  @Test
  void testGetStorageIdFromLocation() {
      assertEquals("file://18b39722140-50eb7d3c5ece",
              DataAccess.getStorageIdFromLocation("file://10.5072/FK2/ABCDEF/18b39722140-50eb7d3c5ece"));
      assertEquals("s3://18b39722140-50eb7d3c5ece",
              DataAccess.getStorageIdFromLocation("s3://bucketname:10.5072/FK2/ABCDEF/18b39722140-50eb7d3c5ece"));
  }
}
