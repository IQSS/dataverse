package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Named;

@Named
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
