package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.bean.ManagedBean;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Leonid Andreev
 * 
 * Dedicated service for managing Harvesting Client Configurations
 */
@Stateless(name = "harvesterService")
@Named
@ManagedBean
public class HarvestingClientServiceBean {
    @EJB
    DataverseServiceBean dataverseService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvestingClinetServiceBean");

    /* let's try and live without this method:
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestResult(Long hdId, String result) {
        Dataverse hd = em.find(Dataverse.class, hdId);
        em.refresh(hd); // ??
        if (hd.isHarvested()) {
            hd.getHarvestingClientConfig().setHarvestResult(result);
        }
    }
    */
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void resetHarvestInProgress(Long hdId) {
        Dataverse hd = em.find(Dataverse.class, hdId);
        em.refresh(hd);
        if (!hd.isHarvested()) {
            return; 
        }
        hd.getHarvestingClientConfig().setHarvestingNow(false);
        
        // And if there is an unfinished RunResult object, we'll
        // just mark it as a failure:
        if (hd.getHarvestingClientConfig().getLastRun() != null 
                && hd.getHarvestingClientConfig().getLastRun().isInProgress()) {
            hd.getHarvestingClientConfig().getLastRun().setFailed();
        }
       
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestInProgress(Long hdId, Date startTime) {
        Dataverse hd = em.find(Dataverse.class, hdId);
        em.refresh(hd);
        HarvestingClient harvestingClient = hd.getHarvestingClientConfig();
        if (harvestingClient == null) {
            return;
        }
        harvestingClient.setHarvestingNow(false);
        if (harvestingClient.getRunHistory() == null) {
            harvestingClient.setRunHistory(new ArrayList<ClientHarvestRun>());
        }
        ClientHarvestRun currentRun = new ClientHarvestRun();
        currentRun.setHarvestingClient(harvestingClient);
        currentRun.setStartTime(startTime);
        currentRun.setInProgress();
        harvestingClient.getRunHistory().add(currentRun);
    }
    
    /*
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setLastHarvestTime(Long hdId, Date lastHarvestTime) {
        Dataverse hd = em.find(Dataverse.class, hdId);
        em.refresh(hd);
        if (hd.isHarvested()) {
            hd.getHarvestingClientConfig().setLastHarvestTime(lastHarvestTime);
        }
    }*/
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestSuccess(Long hdId, Date currentTime, int harvestedCount, int failedCount) {
        Dataverse hd = em.find(Dataverse.class, hdId);
        em.refresh(hd);
        HarvestingClient harvestingClient = hd.getHarvestingClientConfig();
        if (harvestingClient == null) {
            return;
        }
        
        ClientHarvestRun currentRun = harvestingClient.getLastRun();
        
        if (currentRun != null && currentRun.isInProgress()) {
            // TODO: what if there's no current run in progress? should we just
            // give up quietly, or should we make a noise of some kind? -- L.A. 4.4      
            
            currentRun.setSuccess();
            currentRun.setFinishTime(currentTime);
            currentRun.setHarvestedDatasetCount(new Long(harvestedCount));
            currentRun.setFailedDatasetCount(new Long(failedCount));

            /*TODO: still need to record the number of deleted datasets! */
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestFailure(Long hdId, Date currentTime) {
        Dataverse hd = em.find(Dataverse.class, hdId);
        em.refresh(hd);
        HarvestingClient harvestingClient = hd.getHarvestingClientConfig();
        if (harvestingClient == null) {
            return;
        }
        
        ClientHarvestRun currentRun = harvestingClient.getLastRun();
        
        if (currentRun != null && currentRun.isInProgress()) {
            // TODO: what if there's no current run in progress? should we just
            // give up quietly, or should we make a noise of some kind? -- L.A. 4.4      
            
            currentRun.setFailed();
            currentRun.setFinishTime(currentTime);
        }
    }        
}
