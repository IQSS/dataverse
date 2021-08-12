package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClientRepository;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.logging.Logger;

@Stateless
public class DeleteHarvestingClientService implements Serializable {

    private static final Logger logger = Logger.getLogger(DeleteHarvestingClientService.class.getCanonicalName());
    private static final long serialVersionUID = -718825045392962798L;

    private HarvestingClientRepository harvestingClientRepository;

    @Deprecated
    public DeleteHarvestingClientService() {
    }

    @Inject
    public DeleteHarvestingClientService(HarvestingClientRepository harvestingClientRepository) {
        this.harvestingClientRepository = harvestingClientRepository;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeHarvestingClientInNewTransaction(HarvestingClient doomed) {
        HarvestingClient clientToBeDeleted = harvestingClientRepository.getById(doomed.getId());

        if (!clientToBeDeleted.getHarvestedDatasets().isEmpty()) {
            updateDeleteInProgress(clientToBeDeleted);
            return;
        }

        logger.fine("removing harvesting client: " + clientToBeDeleted.getId());
        harvestingClientRepository.delete(clientToBeDeleted);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDeleteInProgress(HarvestingClient clientForDeletion) {
        clientForDeletion.setDeleteInProgress(false);
        harvestingClientRepository.saveAndFlush(clientForDeletion);
    }
}
