package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
public class BeanValidationServiceBean {

    @EJB
    DatasetDao datasetDao;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void validateDatasets() {
        for (Dataset dataset : datasetDao.findAll()) {
            for (DatasetVersion version : dataset.getVersions()) {
                for (FileMetadata fileMetadata : version.getFileMetadatas()) {
                }
            }
        }
    }

}
