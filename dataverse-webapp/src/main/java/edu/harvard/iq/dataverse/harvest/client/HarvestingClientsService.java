package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClientRepository;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.logging.Logger;

@Stateless
public class HarvestingClientsService {
    private static final Logger logger = Logger.getLogger(HarvestingClientsService.class.getCanonicalName());

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private IndexServiceBean indexService;
    private DataverseTimerServiceBean dataverseTimerService;
    private DeleteHarvestedDatasetsService deleteHarvestedDatasetsService;
    private DeleteHarvestingClientService deleteHarvestingClientService;
    private HarvestingClientRepository harvestingClientRepository;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public HarvestingClientsService() {
    }

    @Inject
    public HarvestingClientsService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                                    IndexServiceBean indexService, DataverseTimerServiceBean dataverseTimerService,
                                    DeleteHarvestedDatasetsService deleteHarvestedDatasetsService,
                                    DeleteHarvestingClientService deleteHarvestingClientService,
                                    HarvestingClientRepository harvestingClientRepository) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.indexService = indexService;
        this.dataverseTimerService = dataverseTimerService;
        this.deleteHarvestedDatasetsService = deleteHarvestedDatasetsService;
        this.deleteHarvestingClientService = deleteHarvestingClientService;
        this.harvestingClientRepository = harvestingClientRepository;
    }

    // -------------------- LOGIC --------------------

    public HarvestingClient createHarvestingClient(HarvestingClient newHarvestingClient) {
        return commandEngine.submit(new CreateHarvestingClientCommand(dvRequestService.getDataverseRequest(), newHarvestingClient));
    }

    public HarvestingClient updateHarvestingClient(HarvestingClient updatedHarvestingClient) {
        return commandEngine.submit(new UpdateHarvestingClientCommand(dvRequestService.getDataverseRequest(), updatedHarvestingClient));
    }

    /***
     * Deleting a client, with all the associated content, can take a while -
     * hence it's an async action:
     * TODO:
     * for whatever reason I cannot call the DeleteHarvestingClientCommand from
     * inside this method; something to do with it being asynchronous?
     * @param clientForDeletion - client to be deleted
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void deleteClient(HarvestingClient clientForDeletion) {

        if (clientForDeletion == null) {
            return;
        }

        try {
            dataverseTimerService.removeHarvestTimer(clientForDeletion);
            indexService.deleteHarvestedDocuments(clientForDeletion);
            clientForDeletion.getHarvestedDatasets()
                    .forEach(dataset -> deleteHarvestedDatasetsService.removeHarvestedDatasetInNewTransaction(dataset));
            deleteHarvestingClientService.removeHarvestingClientInNewTransaction(clientForDeletion);
        } catch (Exception e) {
            deleteHarvestingClientService.updateDeleteInProgress(clientForDeletion);
            logger.warning("Failed to delete client. Unknown exception: " + e.getMessage());
        }
    }
}
