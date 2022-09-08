package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import java.util.logging.Logger;

/**
 * Handler of {@link PermissionReindexEvent} events.
 * It assures that changes in permissions will be reflected
 * in solr index.
 *
 * @author madryk
 */
@ApplicationScoped
public class PermissionReindexHandler {

    private static final Logger logger = Logger.getLogger(PermissionReindexHandler.class.getCanonicalName());

    private SolrIndexServiceBean solrIndexService;
    private DatasetRepository datasetRepository;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    PermissionReindexHandler() {
        // JEE requirement
    }

    @Inject
    public PermissionReindexHandler(SolrIndexServiceBean solrIndexService, DatasetRepository datasetRepository) {
        this.solrIndexService = solrIndexService;
        this.datasetRepository = datasetRepository;
    }

    // -------------------- LOGIC --------------------

    public void reindexPermission(@Observes(during = TransactionPhase.AFTER_SUCCESS) PermissionReindexEvent reindexEvent) {

        for (DvObject dvObject : reindexEvent.getDvObjects()) {
            if (dvObject.isInstanceofDataverse()) {
                Dataverse dataverse = (Dataverse) dvObject;
                IndexResponse indexResponse = solrIndexService.indexPermissionsForOneDvObject(dataverse);
                logger.fine("index permissions for dataverse " + dvObject.getId() + ": " + indexResponse);

                datasetRepository.findByOwnerId(dataverse.getId()).forEach(dataset -> {
                    IndexResponse datasetIndexResponse = solrIndexService.indexPermissionsForDatasetWithDataFiles(dataset);
                    logger.fine("index permissions for dataset " + dvObject.getId() + ": " + datasetIndexResponse);
                });

            } else if (dvObject.isInstanceofDataset()) {
                Dataset dataset = (Dataset) dvObject;
                IndexResponse indexResponse = solrIndexService.indexPermissionsForDatasetWithDataFiles(dataset);
                logger.fine("index permissions for dataset " + dvObject.getId() + ": " + indexResponse);
            } else {
                IndexResponse indexResponse = solrIndexService.indexPermissionsForOneDvObject(dvObject);
                logger.fine("index permissions for datafile " + dvObject.getId() + ": " + indexResponse);
            }
        }

    }
}
