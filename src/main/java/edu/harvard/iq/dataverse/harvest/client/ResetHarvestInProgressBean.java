package edu.harvard.iq.dataverse.harvest.client;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class ResetHarvestInProgressBean {

    @EJB
    HarvestingClientServiceBean harvestingClientService;

    @PostConstruct
    public void init() {
        for (HarvestingClient client : harvestingClientService.getAllHarvestingClients()) {
            harvestingClientService.resetHarvestInProgress(client.getId());
            harvestingClientService.resetDeleteInProgress(client.getId());
        }
    }

}
