package edu.harvard.iq.dataverse.dataset;

import com.google.common.base.Stopwatch;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetCitationsCount;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetCitationsCountRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for creating/updating {@link DatasetCitationsCount}s
 * entries in database.
 * 
 * @author madryk
 */
@ApplicationScoped
public class DatasetCitationsCountUpdater {
    private static final Logger logger = LoggerFactory.getLogger(DatasetCitationsCountUpdater.class);

    private DatasetRepository datasetRepository;

    private DatasetCitationsCountRepository datasetCitationsCountRepository;

    private DataCiteRestApiClient dataCiteRestApiClient;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetCitationsCountUpdater() {
        // CDI requirement
    }

    @Inject
    public DatasetCitationsCountUpdater(DatasetRepository datasetRepository,
            DatasetCitationsCountRepository datasetCitationsCountRepository,
            DataCiteRestApiClient dataCiteRestApiClient) {
        this.datasetRepository = datasetRepository;
        this.datasetCitationsCountRepository = datasetCitationsCountRepository;
        this.dataCiteRestApiClient = dataCiteRestApiClient;
    }

    // -------------------- LOGIC --------------------

    /**
     * Update citation count for datasets.
     * Method will skip processing of datasets that are:
     * <ul>
     * <li>not released
     * <li>harvested
     * <li>protocol of global id is different than doi
     * </ul>
     * <p>
     * Note that method internally communicates with DataCite to obtain
     * number of citations. And if your installation uses some other doi
     * provider then most probably DataCite will respond with 404 in
     * such cases.
     */
    public void updateCitationCount() {
        Stopwatch watch = new Stopwatch().start();

        logger.info("Updating citation count started");
        List<Long> datasetIds = datasetRepository.findIdsByNullHarvestedFrom();
        
        for (Long datasetId: datasetIds) {
            try {
                updateDatasetCitationCount(datasetId);
            } catch (IOException | RuntimeException e) {
                logger.warn("Unable to update citation count for dataset with id: {} (cause: {}: {})",
                        datasetId, e.getClass(), e.getMessage());
            }
        }
        
        logger.info("Updating citation count finished. Process took {}s", watch.elapsedTime(TimeUnit.SECONDS));
        watch.stop();
    }

    // -------------------- PRIVATE --------------------

    private void updateDatasetCitationCount(Long datasetId) throws IOException {
        
        Dataset dataset = datasetRepository.getById(datasetId);
        
        GlobalId globalId = dataset.getGlobalId();

        if (dataset.isReleased() && GlobalId.DOI_PROTOCOL.equals(globalId.getProtocol())) {
            
            DataCiteFindDoiResponse dataCiteDoiMetadata = dataCiteRestApiClient.findDoi(globalId.getAuthority(), globalId.getIdentifier());

            DatasetCitationsCount currentCitationsCount = datasetCitationsCountRepository.findByDatasetId(datasetId)
                    .orElse(buildEmptyCitationCounts(dataset));

            currentCitationsCount.setCitationsCount(dataCiteDoiMetadata.getCitationCount());

            logger.info("Updating citation count for {} citation count: {}", globalId.asString(), currentCitationsCount.getCitationsCount());
            datasetCitationsCountRepository.save(currentCitationsCount);
        }
        
    }

    // -------------------- PRIVATE --------------------

    private DatasetCitationsCount buildEmptyCitationCounts(Dataset dataset) {
        DatasetCitationsCount citationsCount = new DatasetCitationsCount();
        citationsCount.setCitationsCount(0);
        citationsCount.setDataset(dataset);
        return citationsCount;
    }

}
