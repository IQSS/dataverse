package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.DatasetServiceBean;
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
    DatasetServiceBean datasetService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void validateDatasets() {
        for (Dataset dataset : datasetService.findAll()) {
            for (DatasetVersion version : dataset.getVersions()) {
                for (FileMetadata fileMetadata : version.getFileMetadatas()) {
                }
            }
        }
    }

}
