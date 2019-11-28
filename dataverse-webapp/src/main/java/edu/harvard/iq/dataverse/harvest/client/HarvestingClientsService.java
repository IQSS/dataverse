package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Logger;

@Stateless
public class HarvestingClientsService {
    private static final Logger logger = Logger.getLogger(HarvestingClientsService.class.getCanonicalName());

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private DataFileServiceBean dataFileService;
    private IndexServiceBean indexService;
    private DataverseTimerServiceBean dataverseTimerService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public HarvestingClientsService() {
    }

    @Inject
    public HarvestingClientsService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                                    DataFileServiceBean dataFileService,
                                    IndexServiceBean indexService, DataverseTimerServiceBean dataverseTimerService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.dataFileService = dataFileService;
        this.indexService = indexService;
        this.dataverseTimerService = dataverseTimerService;
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
    public void deleteClient(HarvestingClient clientForDeletion) {
        String errorMessage = null;

        if (clientForDeletion == null) {
            return;
        }

        try {
            HarvestingClient merged = em.merge(clientForDeletion);

            dataverseTimerService.removeHarvestTimer(clientForDeletion);
            indexService.deleteHarvestedDocuments(clientForDeletion);
            removeHarvestedFiles(merged);

            em.remove(merged);
        } catch (Exception e) {
            errorMessage = "Failed to delete cleint. Unknown exception: " + e.getMessage();
        }

        if (errorMessage != null) {
            logger.warning(errorMessage);
        }
    }

    // -------------------- PRIVATE ---------------------

    /***
     * All the datasets harvested by this client will be cleanly deleted
     * through the defined cascade. Cascaded delete does not work for harvested
     * files, however. So they need to be removed explicitly; before we
     * proceed removing the client itself.
     * @param merged
     */
    private void removeHarvestedFiles(HarvestingClient merged) {
        dataFileService.findHarvestedFilesByClient(merged)
                .forEach(this::removeHarvestedFile);
    }

    private void removeHarvestedFile(DataFile harvestedFile) {
        DataFile mergedFile = em.merge(harvestedFile);
        em.remove(mergedFile);
    }
}
