package edu.harvard.iq.dataverse;

import com.google.gson.annotations.Expose;

import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;

@Entity
@Table
public class S3BigDataUpload  implements Serializable {
  private static final Logger logger = Logger.getLogger(S3BigDataUpload.class.getCanonicalName());

  private static final long serialVersionID = 1L;

  public S3BigDataUpload() {
  }

  public S3BigDataUpload(String preSignedUrl, User user, String jsonData, String datasetId, String storageId,
          String fileName, String checksum, String checksumType, String contentType, Timestamp creationTime) {
    this.preSignedUrl = preSignedUrl;
    this.user = user;
    this.jsonData = jsonData;
    this.datasetId = datasetId;
    this.storageId = storageId;
    this.fileName = fileName;
    this.checksum = checksum;
    this.checksumType = checksumType;
    this.contentType = contentType;
    this.creationTime = creationTime;
  }

  @Expose
  @Id
  @Column(nullable = false, length = 1024)
  private String preSignedUrl;

  @Expose
  @JoinColumn
  private User user;

  @Expose
  @Column(nullable = true)
  private String jsonData;

  @Expose
  @Column (nullable = false)
  private String datasetId;

  @Expose
  @Column
  private String storageId;

  @Expose
  @Column (nullable = false)
  private String fileName;

  @Expose
  @Column(nullable = false, length = 1024)
  private String checksum;

  @Expose
  @Column(nullable = false)
  private String checksumType;

  @Expose
  @NotBlank
  @Column(nullable = false)
  private String contentType;

  @Expose
  @Column (nullable = false)
  private Timestamp creationTime;

  public String getPreSignedUrl() {
    return preSignedUrl;
  }

  public void setPreSignedUrl(String preSignedUrl) {
    this.preSignedUrl = preSignedUrl;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getJsonData() {
    return jsonData;
  }

  public void setJsonData(String jsonData) {
    this.jsonData = jsonData;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getStorageId() {
    return storageId;
  }

  public void setStorageId(String storageId) {
    this.storageId = storageId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public String getChecksumType() {
    return checksumType;
  }

  public void setChecksumType(String checksumType) {
    this.checksumType = checksumType;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }
}
