package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.logging.Logger;

@Stateless
public class DeleteHarvestedDatasetsService implements Serializable {

    private static final Logger logger = Logger.getLogger(DeleteHarvestedDatasetsService.class.getCanonicalName());
    private static final long serialVersionUID = -756523918526241095L;

    private DatasetRepository datasetRepository;
    private DataFileRepository dataFileRepository;


    @Deprecated
    public DeleteHarvestedDatasetsService() {
    }

    @Inject
    public DeleteHarvestedDatasetsService(DatasetRepository datasetRepository, DataFileRepository dataFileRepository) {
        this.datasetRepository = datasetRepository;
        this.dataFileRepository = dataFileRepository;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeHarvestedDatasetInNewTransaction(Dataset doomedDataset) {
        for (DataFile harvestedFile : doomedDataset.getFiles()) {
            logger.fine("removing file: " + harvestedFile.getId());
            dataFileRepository.mergeAndDelete(harvestedFile);
        }
        logger.fine("removing dataset: " + doomedDataset.getId());
        doomedDataset.setFiles(null);
        datasetRepository.mergeAndDelete(doomedDataset);
    }
}
