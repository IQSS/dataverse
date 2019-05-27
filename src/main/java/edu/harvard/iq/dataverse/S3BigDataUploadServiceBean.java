package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.ConstraintViolationException;

import edu.harvard.iq.dataverse.authorization.users.User;

@Stateless
@Named
public class S3BigDataUploadServiceBean {

  private  static final Logger logger = Logger.getLogger(S3BigDataUploadServiceBean.class.getCanonicalName());

  @PersistenceContext(name = "VDCNet-ejbPU")
  private EntityManager em;


  public void addS3BigDataUpload(String preSignedUrl, User user, String jsonData, String datasetId, String stroageId,
          String fileName, String checksum, String checksumType, String contentType, Timestamp creationTime) {

    try {
      S3BigDataUpload bigData = new S3BigDataUpload();
      bigData.setPreSignedUrl(preSignedUrl);
      bigData.setUser(user);
      bigData.setJsonData(jsonData);
      bigData.setDatasetId(datasetId);
      bigData.setStorageId(stroageId);
      bigData.setFileName(fileName);
      bigData.setChecksum(checksum);
      bigData.setChecksumType(checksumType);
      bigData.setContentType(contentType);
      bigData.setCreationTime(creationTime);
      em.persist(bigData);
    } catch (ConstraintViolationException e) {
      logger.warning("Exception: ");
      e.getConstraintViolations().forEach(err->logger.warning(err.toString()));
    }
  }

  public S3BigDataUpload getS3BigDataUploadByUrl(String preSignedUrl) {
    try {
      Query query = em.createQuery("SELECT s FROM S3BigDataUpload s WHERE s.preSignedUrl = :preSignedUrl");
      query.setParameter("preSignedUrl", preSignedUrl);
      return (S3BigDataUpload) query.getSingleResult();
    } catch (ConstraintViolationException e) {
      logger.warning("Exception: ");
      e.getConstraintViolations().forEach(err->logger.warning(err.toString()));
    }
    return null;
  }

  @PreDestroy
  public void destruct() {
    em.close();
  }

}
