package edu.harvard.iq.dataverse.harvest.client;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.bean.ManagedBean;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Leonid Andreev
 */
@Stateless(name = "harvesterService")
@ManagedBean
public class HarvesterServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DatasetDao datasetDao;
    @EJB
    HarvestingClientDao harvestingClientService;
    @EJB
    private OAIHarvester oaiHarvester;
    @EJB
    private DataciteDOIHarvester dataciteDOIHarvester;

    private final Map<HarvestType, Harvester<?>> harvesterMap = new HashMap<>();

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean");
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

    public HarvesterServiceBean() {

    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void postConstruct() {
        harvesterMap.put(oaiHarvester.harvestType(), oaiHarvester);
        harvesterMap.put(dataciteDOIHarvester.harvestType(), dataciteDOIHarvester);
    }

    /**
     * Called to run an "On Demand" harvest.
     */
    @Asynchronous
    public void doAsyncHarvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, HarvesterParams params) {

        try {
            doHarvest(dataverseRequest, harvestingClient.getId(), params);
        } catch (Exception e) {
            logger.info("Caught exception running an asynchronous harvest (dataverse \"" + harvestingClient.getName() + "\")");
        }
    }

    /**
     * Run a harvest for an individual harvesting Dataverse
     *
     * @param dataverseRequest
     * @param harvestingClientId
     * @throws IOException
     */
    public <T extends HarvesterParams> void doHarvest(DataverseRequest dataverseRequest, Long harvestingClientId, HarvesterParams params) throws IOException {
        HarvestingClient client = harvestingClientService.find(harvestingClientId);

        if (client == null) {
            throw new IOException("No such harvesting client: id=" + harvestingClientId);
        }

        Dataverse harvestingDataverse = client.getDataverse();

        String logTimestamp = logFormatter.format(new Date());
        Logger hdLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean." + harvestingDataverse.getAlias() + logTimestamp);
        String logFileName = "../logs" + File.separator + "harvest_" + client.getName() + "_" + logTimestamp + ".log";
        FileHandler fileHandler = new FileHandler(logFileName);
        hdLogger.setUseParentHandlers(false);
        hdLogger.addHandler(fileHandler);

        Date harvestStartTime = new Date();

        try {
            boolean harvestingNow = client.isHarvestingNow();

            if (harvestingNow) {
                hdLogger.log(Level.SEVERE, "Cannot begin harvesting, Dataverse " + harvestingDataverse.getName() + " is currently being harvested.");

            } else {
                harvestingClientService.resetHarvestInProgress(harvestingClientId);
                harvestingClientService.setHarvestInProgress(harvestingClientId, harvestStartTime);

                Harvester<T> harvester = resolveHarvester(client);
                HarvesterResult result = harvester.harvest(dataverseRequest, client, hdLogger, params.getParams(harvester.getParamsClass()));

                harvestingClientService.setHarvestSuccess(harvestingClientId, new Date(), result.getNumHarvested(), result.getNumFailed(), result.getNumDeleted());
                hdLogger.log(Level.INFO, "COMPLETED HARVEST, server=" + client.getArchiveUrl() + ", metadataPrefix=" + client.getMetadataPrefix());
                hdLogger.log(Level.INFO, "Datasets created/updated: " + result.getNumHarvested() + ", datasets deleted: " + result.getNumDeleted() + ", datasets failed: " + result.getNumFailed());
            }
        } catch (Throwable e) {
            String message = "Exception processing harvest, server= " + client.getHarvestingUrl() + ",format=" + client.getMetadataPrefix() + " " + e.getClass().getName() + " " + e.getMessage();
            hdLogger.log(Level.SEVERE, message);
            logException(e, hdLogger);
            hdLogger.log(Level.INFO, "HARVEST NOT COMPLETED DUE TO UNEXPECTED ERROR.");
            // TODO:
            // even though this harvesting run failed, we may have had successfully
            // processed some number of datasets, by the time the exception was thrown.
            // We should record that number too. And the number of the datasets that
            // had failed, that we may have counted.  -- L.A. 4.4
            harvestingClientService.setHarvestFailure(harvestingClientId, new Date());

        } finally {
            harvestingClientService.resetHarvestInProgress(harvestingClientId);
            fileHandler.close();
            hdLogger.removeHandler(fileHandler);
        }
    }

    public HarvesterParams parseParams(HarvestingClient client, String paramsJson) {
        if (StringUtils.isBlank(paramsJson)) {
            return HarvesterParams.empty();
        }

        return new Gson().fromJson(paramsJson, resolveHarvester(client).getParamsClass());
    }

    // -------------------- PRIVATE --------------------

    // TODO: I doubt we need a full stacktrace in the harvest log - ??
    // -- L.A. 4.5 May 2016
    private void logException(Throwable e, Logger logger) {

        boolean cause = false;
        String fullMessage = "";
        do {
            String message = e.getClass().getName() + " " + e.getMessage();
            if (cause) {
                message = "\nCaused By Exception.................... " + e.getClass().getName() + " " + e.getMessage();
            }
            StackTraceElement[] ste = e.getStackTrace();
            message += "\nStackTrace: \n";
            for (int m = 0; m < ste.length; m++) {
                message += ste[m].toString() + "\n";
            }
            fullMessage += message;
            cause = true;
        } while ((e = e.getCause()) != null);
        logger.severe(fullMessage);
    }

    private <T extends HarvesterParams> Harvester<T> resolveHarvester(HarvestingClient client) {
        Harvester<?> harvester = harvesterMap.get(client.getHarvestType());
        if (harvester == null) {
            throw new IllegalStateException("Unsupported harvest type");
        }

        return (Harvester<T>) harvester;
    }
}
