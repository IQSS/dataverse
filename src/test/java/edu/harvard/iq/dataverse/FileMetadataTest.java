package edu.harvard.iq.dataverse;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileMetadataTest {
  FileMetadata metadata;

  @BeforeEach
  void setUp() {
    metadata = new FileMetadata();
  }

  @Test
  void testGetFileDateToDisplay_missingFile() {
    assertEquals("", metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithoutDates() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(null);
    datafile.setCreateDate(null);
    metadata.setDataFile(datafile);
    assertEquals("", metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithoutPublicationDate() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(null);
    datafile.setCreateDate(Timestamp.valueOf("2019-01-01 00:00:00"));
    metadata.setDataFile(datafile);
    assertEquals("Jan 1, 2019", metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithPublicationDate() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(Timestamp.valueOf("2019-02-02 00:00:00"));
    datafile.setCreateDate(Timestamp.valueOf("2019-01-02 00:00:00"));
    metadata.setDataFile(datafile);
    assertEquals("Feb 2, 2019", metadata.getFileDateToDisplay());
  }
}
