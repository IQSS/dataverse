package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.util.BundleUtil;

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
    assertEquals(DateFormat.getDateInstance(DateFormat.DEFAULT, BundleUtil.getCurrentLocale()).format(Date.from(Instant.parse("2019-01-01T00:00:00.00Z"))), metadata.getFileDateToDisplay());
  }

  @Test
  void testGetFileDateToDisplay_fileWithPublicationDate() {
    DataFile datafile = new DataFile();
    datafile.setPublicationDate(Timestamp.valueOf("2019-02-02 00:00:00"));
    datafile.setCreateDate(Timestamp.valueOf("2019-01-02 00:00:00"));
    metadata.setDataFile(datafile);
    assertEquals(DateFormat.getDateInstance(DateFormat.DEFAULT, BundleUtil.getCurrentLocale()).format(Date.from(Instant.parse("2019-02-02T00:00:00.00Z"))), metadata.getFileDateToDisplay());
  }
}
