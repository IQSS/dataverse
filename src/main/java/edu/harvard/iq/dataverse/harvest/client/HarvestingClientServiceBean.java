package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author Leonid Andreev
 * 
 * Dedicated service for managing Harvesting Client Configurations
 */
@Stateless
@Named
public class HarvestingClientServiceBean implements java.io.Serializable {
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    EjbDataverseEngine engineService;
    @EJB
    DataFileServiceBean dataFileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @EJB
    IndexServiceBean indexService;
    @EJB
    DataverseTimerServiceBean dataverseTimerService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvestingClinetServiceBean");
    
    public HarvestingClient find(Object pk) {
        return em.find(HarvestingClient.class, pk);
    }
    
    public HarvestingClient findByNickname(String nickName) {
        try {
            return em.createNamedQuery("HarvestingClient.findByNickname", HarvestingClient.class)
					.setParameter("nickName", nickName.toLowerCase())
					.getSingleResult();
        } catch ( NoResultException|NonUniqueResultException ex ) {
            logger.fine("Unable to find a single harvesting client by nickname \"" + nickName + "\": " + ex);
            return null;
        }
    }
    
    public List<HarvestingClient> getAllHarvestingClients() {
        try {
            return em.createQuery("SELECT object(c) FROM HarvestingClient AS c WHERE c.harvestType='oai' ORDER BY c.name", HarvestingClient.class).getResultList();
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while looking up configured Harvesting Clients: "+ex.getMessage());
        }
        return null; 
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void resetHarvestInProgress(Long hcId) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);
        harvestingClient.setHarvestingNow(false);
        
        // And if there is an unfinished RunResult object, we'll
        // just mark it as a failure:
        if (harvestingClient.getLastRun() != null 
                && harvestingClient.getLastRun().isInProgress()) {
            harvestingClient.getLastRun().setFailed();
        }
       
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestInProgress(Long hcId, Date startTime) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);
        harvestingClient.setHarvestingNow(true);
        if (harvestingClient.getRunHistory() == null) {
            harvestingClient.setRunHistory(new ArrayList<>());
        }
        ClientHarvestRun currentRun = new ClientHarvestRun();
        currentRun.setHarvestingClient(harvestingClient);
        currentRun.setStartTime(startTime);
        currentRun.setInProgress();
        harvestingClient.getRunHistory().add(currentRun);
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setDeleteInProgress(Long hcId) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient); // why are we doing this?
        harvestingClient.setDeleteInProgress(true);
    }
    
    // Deleting a client, with all the associated content, can take a while - 
    // hence it's an async action: 
    // TOFIGUREOUT:
    // for whatever reason I cannot call the DeleteHarvestingClientCommand from
    // inside this method; something to do with it being asynchronous?
    @Asynchronous
    public void deleteClient(Long clientId) {
        String errorMessage = null;
        HarvestingClient victim = find(clientId);

        if (victim == null) {
            return;
        }

        try {
            //engineService.submit(new DeleteHarvestingClientCommand(dvRequestService.getDataverseRequest(), victim));
            HarvestingClient merged = em.merge(victim);

            // if this was a scheduled harvester, make sure the timer is deleted:
            dataverseTimerService.removeHarvestTimer(victim);
                
            // purge indexed objects:
            indexService.deleteHarvestedDocuments(victim);
            // All the datasets harvested by this client will be cleanly deleted 
            // through the defined cascade. Cascaded delete does not work for harvested 
            // files, however. So they need to be removed explicitly; before we 
            // proceed removing the client itself. 
            for (DataFile harvestedFile : dataFileService.findHarvestedFilesByClient(merged)) {
                DataFile mergedFile = em.merge(harvestedFile);
                em.remove(mergedFile);
                harvestedFile = null;
            }

            em.remove(merged);

            // Reindex dataverse to update datasetCount
            List<Dataverse> toReindex = new ArrayList<>();
            toReindex.add(victim.getDataverse());
            toReindex.addAll(victim.getDataverse().getOwners());
            for (Dataverse dv : toReindex) {
                try {
                    indexService.indexDataverse(dv);
                } catch (IOException | SolrServerException e) {
                    logger.severe("Dataverse indexing failed. You can kickoff a re-index of this dataverse with: \r\n curl http://localhost:8080/api/admin/index/dataverses/" + dv.getId().toString());
                }
            }

        } catch (Exception e) {
            errorMessage = "Failed to delete cleint. Unknown exception: " + e.getMessage();
        }

        if (errorMessage != null) {
            logger.warning(errorMessage);
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestCompleted(Long hcId, Date currentTime, int harvestedCount, int failedCount, int deletedCount) {
        recordHarvestJobStatus(hcId, currentTime, harvestedCount, failedCount, deletedCount, ClientHarvestRun.RunResultType.COMPLETED);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestCompletedWithFailures(Long hcId, Date currentTime, int harvestedCount, int failedCount, int deletedCount) {
        recordHarvestJobStatus(hcId, currentTime, harvestedCount, failedCount, deletedCount, ClientHarvestRun.RunResultType.COMPLETED_WITH_FAILURES);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestFailure(Long hcId, Date currentTime, int harvestedCount, int failedCount, int deletedCount) {
        recordHarvestJobStatus(hcId, currentTime, harvestedCount, failedCount, deletedCount, ClientHarvestRun.RunResultType.FAILURE);
    } 
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setPartiallyCompleted(Long hcId, Date finishTime, int harvestedCount, int failedCount, int deletedCount) {
        recordHarvestJobStatus(hcId, finishTime, harvestedCount, failedCount, deletedCount, ClientHarvestRun.RunResultType.INTERRUPTED);
    }
    
    public void recordHarvestJobStatus(Long hcId, Date finishTime, int harvestedCount, int failedCount, int deletedCount, ClientHarvestRun.RunResultType result) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);
        
        ClientHarvestRun currentRun = harvestingClient.getLastRun();
        
        if (currentRun != null && currentRun.isInProgress()) {
            
            currentRun.setResult(result);
            currentRun.setFinishTime(finishTime);
            currentRun.setHarvestedDatasetCount(Long.valueOf(harvestedCount));
            currentRun.setFailedDatasetCount(Long.valueOf(failedCount));
            currentRun.setDeletedDatasetCount(Long.valueOf(deletedCount));
        }
    }
    
    public Long getNumberOfHarvestedDatasetsByAllClients() {
        try {
            return (Long) em.createNativeQuery("SELECT count(d.id) FROM dataset d "
                    + " WHERE d.harvestingclient_id IS NOT NULL").getSingleResult();

        } catch (Exception ex) {
            logger.info("Warning: exception looking up the total number of harvested datasets: " + ex.getMessage());
            return 0L;
        }
    }
    
    public Long getNumberOfHarvestedDatasetByClients(List<HarvestingClient> clients) {
        String clientIds = null; 
        for (HarvestingClient client: clients) {
            if (clientIds == null) {
                clientIds = client.getId().toString();
            } else {
                clientIds = clientIds.concat(","+client.getId().toString());
            }
        }
        
        try {
            return (Long) em.createNativeQuery("SELECT count(d.id) FROM dataset d "
                    + " WHERE d.harvestingclient_id in (" 
                    + clientIds + ")").getSingleResult();

        } catch (Exception ex) {
            logger.info("Warning: exception trying to count harvested datasets by clients: " + ex.getMessage());
            return 0L;
        }
    }
}
